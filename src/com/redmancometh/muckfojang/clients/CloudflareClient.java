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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.redmancometh.muckfojang.MuckFojang;
import com.redmancometh.muckfojang.config.CloudflareConfig;
import com.redmancometh.muckfojang.config.Zone;
import com.redmancometh.muckfojang.pojo.DomainChangeRequest;
import com.redmancometh.muckfojang.pojo.SRVContent;
import com.redmancometh.muckfojang.pojo.ZoneListResponse;

import lombok.Data;

@Data
public class CloudflareClient
{
    //https://api.cloudflare.com/client/v4/zones/
    CloseableHttpClient masterClient = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
    private String email;
    private String authKey;
    private Gson reader = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setPrettyPrinting().create();
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

    public CloudflareClient(String email, String authKey)
    {
        super();
        this.email = email;
        this.authKey = authKey;
    }

    public void initContent(HttpPut updateRequest, String subdomain, String domainString, String target, Zone zone)
    {
        CloudflareConfig cfConfig = MuckFojang.getClient().getConfigManager().getConfig().getCloudflareConfig();
        updateRequest.addHeader("X-Auth-Email", cfConfig.getCfEmail());
        updateRequest.addHeader("X-Auth-Key", cfConfig.getAuthKey());
        updateRequest.addHeader("Content-Type", "application/json");
        DomainChangeRequest domainChange = new DomainChangeRequest(domainString, new SRVContent(domainString, target));
        try
        {
            updateRequest.setEntity(new StringEntity(reader.toJson(domainChange)));
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
            if (!pendingDomainChanges.contains(zone.getName())) zone.check();
        });
    }

    public HttpGet getListRequest()
    {
        HttpGet listRequest = new HttpGet("https://api.cloudflare.com/client/v4/zones?per_page=500");
        listRequest.addHeader("X-Auth-Email", email);
        listRequest.addHeader("X-Auth-Key", authKey);
        listRequest.addHeader("Content-Type", "application/json");
        return listRequest;
    }

    public CompletableFuture<Void> initializeZones()
    {
        return CompletableFuture.runAsync(() ->
        {
            HttpGet listRequest = getListRequest();
            try (CloseableHttpResponse response = masterClient.execute(listRequest))
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
            String resp = EntityUtils.toString(response.getEntity());
            ZoneListResponse zoneList = reader.fromJson(resp, ZoneListResponse.class);
            zoneList.getResult().forEach((jso) ->
            {
                Predicate<Zone> zonePredicate = (zone) -> zone.getName().equalsIgnoreCase(jso.getName());
                MuckFojang.getClient().getConfigManager().getConfig().getIndividualZones().getZones().stream().filter(zonePredicate).forEach((
                zone) -> zone.setZoneId(jso.getId()));
            });
            EntityUtils.consume(response.getEntity());
        }
        catch (JsonSyntaxException | ParseException | IOException e)
        {
            e.printStackTrace();
        }
    }

}
