package com.redmancometh.muckfojang.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redmancometh.muckfojang.MuckFojang;
import com.redmancometh.muckfojang.clients.BlockCheckerClient;
import com.redmancometh.muckfojang.pojo.ZoneListResponse;

import lombok.Data;

@Data
public class Zone
{
    private String name, zoneId, currentTarget;
    private List<String> targetDomains = new CopyOnWriteArrayList();
    private int minChangeDelay, maxChangeDelay;
    private boolean notifyOnly, isGrouped = false;
    private int group;
    protected long lastChange;
    protected Map<String, String> sdTargetMap = new ConcurrentHashMap(), sdIdMap = new ConcurrentHashMap();
    protected SubdomainConsumer subConsumer = new SubdomainConsumer();
    CloseableHttpClient zoneClient = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
    protected BlockCheckerClient blockChecker = new BlockCheckerClient();
    protected AtomicBoolean pendingChanges = new AtomicBoolean(false);
    protected Random rand = new Random();
    protected Gson reader = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setPrettyPrinting().create();

    public String getZoneId()
    {
        return zoneId;
    }

    /**
     * Returns the currently selected target for the given subdomain.
     * TODO: This is unnecessary complication
     * @param subdomain
     * @return
     */
    public String getTargetFor(String subdomain)
    {
        return sdTargetMap.get(subdomain);
    }

    /**
     * 
     * @return
     */
    public CompletableFuture<Void> getCurrentDomains()
    {
        HttpGet updateRequest = new HttpGet("https://api.cloudflare.com/client/v4/zones/" + zoneId + "/dns_records?type=SRV");
        return checkList(updateRequest, getSubdomains());
    }

    /**
     * 
     * @param updateRequest
     * @param domainList
     * @return
     */
    public CompletableFuture<Void> checkList(HttpGet updateRequest, List<String> domainList)
    {
        return CompletableFuture.runAsync(() -> domainList.forEach((
        subDomain) -> subConsumer.accept(subDomain, updateRequest)), MuckFojang.getPool());
    }

    public void headRequest(HttpGet updateRequest, String subName)
    {
        CloudflareConfig cfConf = MuckFojang.getClient().getConfigManager().getConfig().getCloudflareConfig();
        updateRequest.addHeader("X-Auth-Email", cfConf.getCfEmail());
        updateRequest.addHeader("X-Auth-Key", cfConf.getAuthKey());
        updateRequest.addHeader("Content-Type", "application/json");
    }

    /**
     * Retrieve all subdoamins for the zone.
     * @return
     */
    public List<String> getSubdomains()
    {
        return MuckFojang.getClient().getConfigManager().getConfig().getDomainConfig().getSubdomainList();
    }

    /**
     * Get the cloudflare API ID for the given subdomain parameter
     * @param subdomain
     * @return
     */
    public String getSubdomainId(String subdomain)
    {
        return sdIdMap.get(subdomain);
    }

    /**
     * Master check method. Does NOT change by itself.
     */
    public void check()
    {
        if (pendingChanges.get()) return;
        getCurrentDomains().thenRun(() ->
        {
            getSubdomains().forEach((sd) -> blockChecker.isBlocked(getTargetFor(sd)).thenAccept((blocked) ->
            {
                System.out.println(sd + " " + getTargetFor(sd));
                System.out.println("\n\tChecked Subdomain: " + sd + "\n\t\tZone: " + getName() + "\n\t\tBlocked: " + blocked);
                if (blocked)
                {
                    if (notifyOnly)
                    {
                        for (int x = 0; x < 15; x++)
                            System.out.println("\n\n\007Domain is blocked, but not being changed, because this zone is notify-only!");
                        return;
                    }
                    changeSubdomains();
                }
            }));
            System.out.println("\n\n");
        });
    }

    /**
     * Gets a new target and sees if it's blocked. If it's not it schedules a change.
     * @param zone
     * @param subdomain
     */
    public void changeSubdomains()
    {
        Collections.shuffle(getTargetDomains());
        String newTarget = getTargetDomains().get(0);
        blockChecker.isBlocked(newTarget).thenAccept((isBlocked) ->
        {
            if (isBlocked)
            {
                purgeZoneTarget(newTarget);
                changeSubdomains();
                return;
            }
            System.out.println("Trying new target for: " + "." + getName() + " \n\tTarget:" + newTarget);
            getSubdomains().forEach((subdomain) -> rotate(subdomain, newTarget));
        });
    }

    public void purgeZoneTarget(String target)
    {
        System.out.println("PURGE ZONE " + target);
        this.targetDomains.remove(target);
        if (this.targetDomains.size() < 3) for (int x = 0; x < 50; x++)
            System.out.println("Down to less than 3 subdomains for rotation!");
        MuckFojang.getClient().getConfigManager().updateConfig();
    }

    public void rotate(String subdomain, String newTarget)
    {
        long timeToChange = rand.nextInt((getMaxChangeDelay() - getMinChangeDelay()) + getMinChangeDelay()) * 1000;
        System.out.println("Seconds until change: " + timeToChange + " milliseconds!");
        MuckFojang.getClient().getTickTimer().schedule(() ->
        {
            MuckFojang.getClient().getClient().changeSubdomainRecord(subdomain, newTarget, Zone.this);
            pendingChanges.set(false);
        }, timeToChange, TimeUnit.MILLISECONDS);
        pendingChanges.set(true);
    }

    /**
     * I think this gets called like, a bunch more times than it's supposed to.
     * There's stateful-ish design so it doesn't matter, but worth investigating.
     * TODO: Investigate
     * @author Redmancometh
     *
     */
    class SubdomainConsumer implements BiConsumer<String, HttpGet>
    {
        @Override
        public void accept(String subDomain, HttpGet retrieveRequest)
        {
            headRequest(retrieveRequest, subDomain);
            try (CloseableHttpResponse response = zoneClient.execute(retrieveRequest))
            {
                String responseString = EntityUtils.toString(response.getEntity());
                ZoneListResponse jse = reader.fromJson(responseString, ZoneListResponse.class);
                jse.getResult().forEach((element) ->
                {
                    String toCheck = "_minecraft._tcp." + subDomain + "." + getName();
                    String rootRecord = "_minecraft._tcp." + getName();
                    /**
                     * Probably a more elegant way to do this.
                     * Do contains 25565 to ensure we don't accidentally use like a 
                     * fucking TS record or something 
                     */
                    if (element.getContent().contains("25565") && element.getName().equalsIgnoreCase(toCheck))
                        insertIDAndTarget(subDomain, element.getContent().replace("1\t25565\t", ""), element.getId());
                    else if (element.getContent().contains("25565") && element.getName().equalsIgnoreCase(rootRecord) && getSubdomains().contains("rootdomain")) insertIDAndTarget("rootdomain", element.getContent().replace("1\t25565\t", ""), element.getId());
                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public void insertIDAndTarget(String sdKey, String target, String id)
    {
        sdTargetMap.put(sdKey, target);
        sdIdMap.put(sdKey, id);
    }

}
