package com.redmancometh.muckfojang.config;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.redmancometh.muckfojang.MuckFojang;

public class Zone
{
    private String name;
    private List<String> subdomains;
    private boolean uniformSubdomain;
    protected String zoneId;
    protected Map<String, String> sdTargetMap = new ConcurrentHashMap();
    protected String currentTarget;

    CloseableHttpClient zoneClient = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();

    public String getZoneId()
    {
        return zoneId;
    }

    public String getTargetFor(String subdomain)
    {
        return sdTargetMap.get(subdomain);
    }

    public CompletableFuture<Void> getCurrentDomains()
    {
        HttpGet updateRequest = new HttpGet("https://api.cloudflare.com/client/v4/zones/" + zoneId + "/dns_records?type=SRV");
        System.out.println("https://api.cloudflare.com/client/v4/zones/" + zoneId + "/dns_records");
        if (this.isUniformSubdomain())
            return checkUniform(updateRequest);
        else
            return checkNonUniform(updateRequest);
    }

    public CompletableFuture<Void> checkUniform(HttpGet updateRequest)
    {
        return CompletableFuture.runAsync(() ->
        {
            MuckFojang.getClient().getConfigManager().getConfig().getDomainConfig().getSubdomainList().forEach((subDomain) ->
            {
                headRequest(updateRequest, subDomain);
                try
                {
                    try (CloseableHttpResponse response = zoneClient.execute(updateRequest))
                    {
                        JsonParser parser = new JsonParser();
                        JsonElement jse = parser.parse(EntityUtils.toString(response.getEntity()));
                        jse.getAsJsonObject().get("result").getAsJsonArray().forEach((element) -> sdTargetMap.put(subDomain, element.getAsJsonObject().get("content").getAsString().replace("1\t25565\t", "")));
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            });
        });
    }

    public CompletableFuture<Void> checkNonUniform(HttpGet updateRequest)
    {
        return CompletableFuture.runAsync(() ->
        {
            getSubdomains().forEach((subDomain) ->
            {
                System.out.println("REG SUBDOMAIN: " + subDomain);
                headRequest(updateRequest, subDomain);
                try
                {
                    try (CloseableHttpResponse response = zoneClient.execute(updateRequest))
                    {
                        JsonParser parser = new JsonParser();
                        String responseString = EntityUtils.toString(response.getEntity());
                        JsonElement jse = parser.parse(responseString);
                        jse.getAsJsonObject().get("result").getAsJsonArray().forEach((element) -> sdTargetMap.put(subDomain, element.getAsJsonObject().get("content").getAsString().replace("1\t25565\t", "")));
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            });
        });
    }

    public void headRequest(HttpGet updateRequest, String subName)
    {
        CloudflareConfig cfConf = MuckFojang.getClient().getConfigManager().getConfig().getCloudflareConfig();
        updateRequest.addHeader("X-Auth-Email", cfConf.getCfEmail());
        updateRequest.addHeader("X-Auth-Key", cfConf.getAuthKey());
        updateRequest.addHeader("Content-Type", "application/json");
    }

    public void setZoneId(String zoneId)
    {
        this.zoneId = zoneId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String zone)
    {
        this.name = zone;
    }

    public List<String> getSubdomains()
    {
        return subdomains;
    }

    public void setSubdomains(List<String> subdomains)
    {
        this.subdomains = subdomains;
    }

    public boolean isUniformSubdomain()
    {
        return uniformSubdomain;
    }

    public void setUniformSubdomain(boolean uniformSubdomain)
    {
        this.uniformSubdomain = uniformSubdomain;
    }

    public String getCurrentTarget()
    {
        return currentTarget;
    }

    public void setCurrentTarget(String currentTarget)
    {
        this.currentTarget = currentTarget;
    }

}
