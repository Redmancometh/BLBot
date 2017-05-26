package com.redmancometh.muckfojang.config;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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
    private boolean uniformDomain;
    private List<String> targetDomains = new CopyOnWriteArrayList();
    private int maxChangeDelay;
    private int minChangeDelay;
    private boolean notifyOnly;
    private boolean isGrouped = false;
    private int group;
    protected String zoneId;
    protected Map<String, String> sdTargetMap = new ConcurrentHashMap();
    protected Map<String, String> sdIdMap = new ConcurrentHashMap();
    protected String currentTarget;
    protected long lastChange;
    protected SubdomainConsumer subConsumer = new SubdomainConsumer();
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
        return checkList(updateRequest, getSubdomains());
    }

    public CompletableFuture<Void> checkList(HttpGet updateRequest, List<String> domainList)
    {
        return CompletableFuture.runAsync(() -> domainList.forEach((subDomain) -> subConsumer.accept(subDomain, updateRequest)), MuckFojang.getPool());
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
        if (!uniformDomain) return subdomains;
        return MuckFojang.getClient().getConfigManager().getConfig().getDomainConfig().getSubdomainList();
    }

    public void setSubdomains(List<String> subdomains)
    {
        this.subdomains = subdomains;
    }

    public boolean isUniformDomain()
    {
        return uniformDomain;
    }

    public void setDomain(boolean uniformSubdomain)
    {
        this.uniformDomain = uniformSubdomain;
    }

    public String getCurrentTarget()
    {
        return currentTarget;
    }

    public void setCurrentTarget(String currentTarget)
    {
        this.currentTarget = currentTarget;
    }

    public String getSubdomainId(String subdomain)
    {
        return sdIdMap.get(subdomain);
    }

    public List<String> getTargetDomains()
    {
        return targetDomains;
    }

    public void setTargetDomains(List<String> targetDomains)
    {
        this.targetDomains = targetDomains;
    }

    public int getMaxChangeDelay()
    {
        return maxChangeDelay;
    }

    public void setMaxChangeDelay(int changeTime)
    {
        this.maxChangeDelay = changeTime;
    }

    public int getMinChangeDelay()
    {
        return minChangeDelay;
    }

    public void setMinChangeDelay(int minChangeDelay)
    {
        this.minChangeDelay = minChangeDelay;
    }

    public boolean isNotifyOnly()
    {
        return notifyOnly;
    }

    public void setNotifyOnly(boolean notifyOnly)
    {
        this.notifyOnly = notifyOnly;
    }

    public boolean isGrouped()
    {
        return isGrouped;
    }

    public void setGrouped(boolean isGrouped)
    {
        this.isGrouped = isGrouped;
    }

    public int getGroup()
    {
        return group;
    }

    public void setGroup(int group)
    {
        this.group = group;
    }

    @FunctionalInterface
    public interface UpdateStringConsumer
    {
        public abstract void accept(String subDomain, HttpGet retrieveRequest);
    }

    class SubdomainConsumer implements UpdateStringConsumer
    {
        @Override
        public void accept(String subDomain, HttpGet retrieveRequest)
        {
            headRequest(retrieveRequest, subDomain);
            try (CloseableHttpResponse response = zoneClient.execute(retrieveRequest))
            {
                JsonParser parser = new JsonParser();
                String responseString = EntityUtils.toString(response.getEntity());
                JsonElement jse = parser.parse(responseString);
                jse.getAsJsonObject().get("result").getAsJsonArray().forEach((element) ->
                {
                    String toCheck = "_minecraft._tcp." + subDomain + "." + getName();
                    if (element.getAsJsonObject().get("name").getAsString().equalsIgnoreCase(toCheck))
                    {
                        sdTargetMap.put(subDomain, element.getAsJsonObject().get("content").getAsString().replace("1\t25565\t", ""));
                        sdIdMap.put(subDomain, element.getAsJsonObject().get("id").getAsString());
                    }
                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

        }
    }

}
