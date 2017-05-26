package com.redmancometh.muckfojang.clients;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.redmancometh.muckfojang.MuckFojang;
import com.redmancometh.muckfojang.config.CloudflareConfig;
import com.redmancometh.muckfojang.config.Zone;

public class CloudflareClient
{
    //https://api.cloudflare.com/client/v4/zones/

    CloseableHttpClient masterClient = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
    private String email;
    private String authKey;
    private Gson reader = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    private BlockCheckerClient blockChecker = new BlockCheckerClient();
    private List<String> pendingSubdomainChanges = new CopyOnWriteArrayList();
    private Random rand = new Random();

    public CloudflareClient(String email, String authKey)
    {
        super();
        this.email = email;
        this.authKey = authKey;
    }

    public void changeZone(Zone zone, String subdomain)
    {
        Collections.shuffle(zone.getTargetDomains());
        String newTarget = zone.getTargetDomains().get(0);
        blockChecker.isBlocked(newTarget).thenAccept((blocked) ->
        {
            if (blocked)
            {
                purgeZoneTarget(newTarget);
                changeZone(zone, subdomain);
                return;
            }
            System.out.println("Trying new target for: " + subdomain + "." + zone.getName() + " \n\tTarget:" + newTarget);
            try
            {
                long timeToChange = rand.nextInt((zone.getMaxChangeDelay() - zone.getMinChangeDelay()) + zone.getMinChangeDelay()) * 1000;
                System.out.println("Seconds until change: " + timeToChange);
                MuckFojang.getClient().getTickTimer().schedule(new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        changeSubdomainRecord(subdomain, newTarget, zone);
                        pendingSubdomainChanges.remove(subdomain + "." + zone.getName());
                    }
                }, timeToChange);
                pendingSubdomainChanges.add(subdomain + "." + zone.getName());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        });
    }

    public void initContent(HttpPut updateRequest, String subdomain, String domainString, String target, Zone zone)
    {
        CloudflareConfig cfConfig = MuckFojang.getClient().getConfigManager().getConfig().getCloudflareConfig();
        updateRequest.addHeader("X-Auth-Email", cfConfig.getCfEmail());
        updateRequest.addHeader("X-Auth-Key", cfConfig.getAuthKey());
        updateRequest.addHeader("Content-Type", "application/json");
        JSONObject json = new JSONObject();
        JSONObject srvContent = new JSONObject();
        srvContent.put("service", "_minecraft");
        srvContent.put("proto", "_tcp");
        srvContent.put("name", domainString);
        srvContent.put("priority", 1);
        srvContent.put("weight", 1);
        srvContent.put("port", 25565);
        srvContent.put("target", target);
        json.put("type", "SRV");
        json.put("name", domainString);
        json.put("data", srvContent);
        try
        {
            updateRequest.setEntity(new StringEntity(json.toJSONString()));
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
    }

    public CompletableFuture<Void> changeSubdomainRecord(String subdomain, String target, Zone zone)
    {
        return CompletableFuture.runAsync(() ->
        {

            String domainString = "_minecraft._tcp." + subdomain + "." + zone.getName();
            try
            {
                HttpPut put = new HttpPut("https://api.cloudflare.com/client/v4/zones/" + zone.getZoneId() + "/dns_records/" + zone.getSubdomainId(subdomain));
                initContent(put, subdomain, domainString, target, zone);
                System.out.println("Changing: " + domainString + " to " + target);
                String response = EntityUtils.toString(masterClient.execute(put).getEntity());
                System.out.println(response);
            }
            catch (ParseException | IOException e)
            {
                e.printStackTrace();
            }
        }, MuckFojang.getPool());
    }

    public void purgeZoneTarget(String target)
    {
        System.out.println("PURGE ZONE " + target);
    }

    public void logPurge()
    {

    }

    public void checkZones()
    {
        MuckFojang.getClient().getConfigManager().getConfig().getIndividualZones().getZones().forEach((zone) ->
        {
            if (!pendingSubdomainChanges.contains(zone.getName())) checkZone(zone);
        });
    }

    public void checkZone(Zone zone)
    {
        zone.getCurrentDomains().thenRun(() ->
        {
            zone.getSubdomains().stream().filter(sd -> !pendingSubdomainChanges.contains(sd + "." + zone.getName())).forEach((sd) -> blockChecker.isBlocked(zone.getTargetFor(sd)).thenAccept((blocked) ->
            {
                System.out.println("Subdomain: " + sd + ", Zone: " + zone.getName() + ", Blocked: " + blocked);
                if (blocked) changeZone(zone, sd);
            }));
        });
    }

    public HttpGet getListRequest()
    {
        HttpGet listRequest = new HttpGet("http://api.cloudflare.com/client/v4/zones?per_page=500");
        listRequest.addHeader("X-Auth-Email", email);
        listRequest.addHeader("X-Auth-Key", authKey);
        listRequest.addHeader("Content-Type", "application/json");
        return listRequest;
    }

    public CompletableFuture<Void> initializeZones()
    {
        return CompletableFuture.runAsync(() ->
        {
            HttpGet updateRequest = getListRequest();
            try (CloseableHttpResponse response = masterClient.execute(updateRequest))
            {
                try (InputStreamReader inputReader = new InputStreamReader(response.getEntity().getContent()))
                {
                    JsonParser parser = new JsonParser();
                    JsonElement obj = parser.parse(EntityUtils.toString(response.getEntity()));
                    JsonArray json = obj.getAsJsonObject().get("result").getAsJsonArray();
                    json.forEach((element) ->
                    {
                        JsonObject jso = element.getAsJsonObject();
                        Predicate<Zone> zonePredicate = (zone) -> zone.getName().equalsIgnoreCase(jso.get("name").getAsString());
                        MuckFojang.getClient().getConfigManager().getConfig().getIndividualZones().getZones().stream().filter(zonePredicate).forEach((zone) -> zone.setZoneId(jso.get("id").getAsString()));
                    });
                    EntityUtils.consume(response.getEntity());
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }, MuckFojang.getPool());
    }

    public CloseableHttpClient getClient()
    {
        return masterClient;
    }

    public void setClient(CloseableHttpClient client)
    {
        this.masterClient = client;
    }

    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }

    public String getAuthKey()
    {
        return authKey;
    }

    public void setAuthKey(String authKey)
    {
        this.authKey = authKey;
    }

    public Gson getReader()
    {
        return reader;
    }

    public void setReader(Gson reader)
    {
        this.reader = reader;
    }
}
