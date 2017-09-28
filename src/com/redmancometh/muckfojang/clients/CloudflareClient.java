package com.redmancometh.muckfojang.clients;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
    private List<String> pendingDomainChanges = new CopyOnWriteArrayList();
    private Map<Integer, String> groupTargets = new ConcurrentHashMap();

    private List<Integer> pendingGroupChanges = new CopyOnWriteArrayList();
    LoadingCache<Integer, List<Zone>> groupCache = CacheBuilder.newBuilder().build(new CacheLoader<Integer, List<Zone>>()
    {
        @Override
        public List<Zone> load(Integer key)
        {
            return new ArrayList();
        }

    });

    public String getGroupTarget(int arg0)
    {
        return groupTargets.get(arg0);
    }

    public String put(int arg0, String arg1)
    {
        return groupTargets.put(arg0, arg1);
    }

    public CloudflareClient(String email, String authKey)
    {
        super();
        this.email = email;
        this.authKey = authKey;
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

    public void logPurge()
    {

    }

    public void checkZones()
    {
        MuckFojang.getClient().getConfigManager().getConfig().getIndividualZones().getZones().forEach((zone) ->
        {
            if (!pendingDomainChanges.contains(zone.getName())) checkZone(zone);
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
        zone.check();
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
