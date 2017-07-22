package com.redmancometh.muckfojang.clients;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
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
    private List<Integer> pendingGroupChanges = new CopyOnWriteArrayList();
    private Random rand = new Random();
    LoadingCache<Integer, List<Zone>> groupCache = CacheBuilder.newBuilder().build(new CacheLoader<Integer, List<Zone>>()
    {
        @Override
        public List<Zone> load(Integer key)
        {
            return new ArrayList();
        }

    });

    public CloudflareClient(String email, String authKey)
    {
        super();
        this.email = email;
        this.authKey = authKey;
    }

    /**
     * Changes all domains for the domain group given in the parameter
     * @param groupId
     */
    public void changeGroup(int groupId)
    {
        try
        {
            groupCache.get(groupId).forEach((zone) ->
            {
                Collections.shuffle(zone.getTargetDomains());
                String newTarget = zone.getTargetDomains().get(0);
                blockChecker.isBlocked(newTarget).thenAccept((blocked) -> tryNewTarget(blocked, groupId, zone, newTarget));
            });
        }
        catch (ExecutionException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * This will take the result from the block checker and 
     * take appropriate action.
     * 
     * If it's blocked it will purget the target chosen which was blocked then it will call back up to 
     * changeGroup and start the process again.
     * 
     * @param blocked
     * @param groupId
     * @param zone
     * @param newTarget
     */
    private void tryNewTarget(boolean blocked, int groupId, Zone zone, String newTarget)
    {
        if (blocked)
        {
            purgeZoneTarget(newTarget);
            changeGroup(groupId);
            return;
        }
        zone.getSubdomains().forEach((subdomain) -> // iterate through subdomains and schedule change to newTarget, as it's a valid domain
        {
            //scheduleChange(subdomain, newTarget, zone);
            System.out.println("Trying new target for: " + subdomain + "." + zone.getName() + " \n\tTarget:" + newTarget);
        });
    }

    /**
     * Gets a new target and sees if it's blocked. If it's not it schedules a change.
     * @param zone
     * @param subdomain
     */
    public void changeSubdomain(Zone zone, String subdomain)
    {
        Collections.shuffle(zone.getTargetDomains());
        String newTarget = zone.getTargetDomains().get(0);
        blockChecker.isBlocked(newTarget).thenAccept((blocked) ->
        {
            if (blocked)
            {
                purgeZoneTarget(newTarget);
                //changeSubdomain(zone, subdomain);
                return;
            }
            System.out.println("Trying new target for: " + subdomain + "." + zone.getName() + " \n\tTarget:" + newTarget);
            //scheduleChange(subdomain, newTarget, zone);
        });
    }

    public void scheduleChange(String subdomain, String newTarget, Zone zone)
    {
        long timeToChange = rand.nextInt((zone.getMaxChangeDelay() - zone.getMinChangeDelay()) + zone.getMinChangeDelay()) * 1000;
        System.out.println("Seconds until change: " + timeToChange);
        MuckFojang.getClient().getTickTimer().schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                if (zone.isGrouped())
                {
                    zone.getSubdomains().forEach((subDomain) ->
                    {
                        System.out.println(subDomain);
                        changeSubdomainRecord(subdomain, newTarget, zone);
                    });
                    pendingGroupChanges.remove(zone.getGroup());
                    return;
                }
                changeSubdomainRecord(subdomain, newTarget, zone);
                pendingSubdomainChanges.remove(subdomain + "." + zone.getName());
            }
        }, timeToChange);
        if (zone.isGrouped())
            pendingGroupChanges.add(zone.getGroup());
        else
            pendingSubdomainChanges.add(subdomain + "." + zone.getName());
    }

    public void insertSrv(JSONObject srvContent, String domainString, String target)
    {
        srvContent.put("service", "_minecraft");
        srvContent.put("proto", "_tcp");
        System.out.println("DOMAIN STRING: " + domainString);
        srvContent.put("name", domainString);
        srvContent.put("priority", 1);
        srvContent.put("weight", 1);
        srvContent.put("port", 25565);
        srvContent.put("target", target);
    }

    public void initContent(HttpPut updateRequest, String subdomain, String domainString, String target, Zone zone)
    {
        System.out.println("CONTENT STR: " + subdomain);
        CloudflareConfig cfConfig = MuckFojang.getClient().getConfigManager().getConfig().getCloudflareConfig();
        updateRequest.addHeader("X-Auth-Email", cfConfig.getCfEmail());
        updateRequest.addHeader("X-Auth-Key", cfConfig.getAuthKey());
        updateRequest.addHeader("Content-Type", "application/json");
        JSONObject json = new JSONObject();
        JSONObject srvContent = new JSONObject();
        insertSrv(srvContent, domainString, target);
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

    /**
     * The actual CF request that changes the zone.
     * @param subdomain
     * @param target
     * @param zone
     * @return
     */
    public CompletableFuture<Void> changeSubdomainRecord(String subdomain, String target, Zone zone)
    {
        return CompletableFuture.runAsync(() ->
        {
            String domainString = subdomain + "." + zone.getName();
            try
            {
                HttpPut put = new HttpPut("https://api.cloudflare.com/client/v4/zones/" + zone.getZoneId() + "/dns_records/" + zone.getSubdomainId(subdomain));
                initContent(put, subdomain, domainString, target, zone);
                String response = EntityUtils.toString(masterClient.execute(put).getEntity());
                System.out.println("Subdomain change response: \n\t" + response);
            }
            catch (ParseException | IOException e)
            {
                e.printStackTrace();
            }
        }, MuckFojang.getPool());
    }

    public void removeJson() throws IOException
    {

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

    /**
     * Check a zone.
     * 
     * @param zone
     */
    public void checkZone(Zone zone)
    {
        System.out.println("Checking zone: " + zone.getName());
        addZoneGroup(zone);
        zone.getCurrentDomains().thenRun(() ->
        {
            zone.getSubdomains().stream().filter(sd -> !pendingSubdomainChanges.contains(sd + "." + zone.getName())).forEach((sd) -> blockChecker.isBlocked(zone.getTargetFor(sd)).thenAccept((blocked) ->
            {
                System.out.println("\n\tChecked Subdomain: " + sd + "\n\t\tZone: " + zone.getName() + "\n\t\tBlocked: " + blocked);
                if (blocked && zone.isNotifyOnly())
                    for (int x = 0; x < 5; x++)
                    {
                        System.out.println("Domain is blocked, but not being changed, because this zone is notify-only!");
                        System.out.print("\007");
                    }
                else if (blocked) changeSubdomain(zone, sd);
            }));
            System.out.println("\n\n");
        });
    }

    /**
     * Check to see if the zone is already in the appropriate group cache
     * if it's not found it will be added.
     * 
     * If it's found no action is taken.
     * @param zone
     */
    public void addZoneGroup(Zone zone)
    {
        if (zone.isGrouped()) try
        {
            List<Zone> zones = groupCache.get(zone.getGroup());
            if (!zones.contains(zone)) groupCache.get(zone.getGroup()).add(zone);
        }
        catch (ExecutionException e)
        {
            e.printStackTrace();
        }
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
                    parseZoneListResponse(response);
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }, MuckFojang.getPool());
    }

    public void parseZoneListResponse(CloseableHttpResponse response)
    {
        try
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
        catch (JsonSyntaxException | ParseException | IOException e)
        {
            e.printStackTrace();
        }
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

    public List<Integer> getPendingGroupChanges()
    {
        return pendingGroupChanges;
    }

    public void setPendingGroupChanges(List<Integer> pendingGroupChanges)
    {
        this.pendingGroupChanges = pendingGroupChanges;
    }
}
