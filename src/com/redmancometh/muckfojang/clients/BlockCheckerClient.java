package com.redmancometh.muckfojang.clients;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;

public class BlockCheckerClient
{
    public static JSONObject initRequest(HttpPost updateRequest, String domain)
    {
        updateRequest.addHeader("host", "blocklist.tcpr.ca");
        JSONObject json = new JSONObject();
        json.put("ip", domain);
        return json;
    }

    public CompletableFuture<Boolean> isBlocked(String domain)
    {
        return CompletableFuture.supplyAsync(() -> checkForDomain(domain));
    }

    private boolean checkForDomain(String domain)
    {
        CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
        HttpGet updateRequest = new HttpGet("https://sessionserver.mojang.com/blockedservers");
        try
        {
            HttpResponse response = client.execute(updateRequest);
            String doc = EntityUtils.toString(response.getEntity());
            List<String> responseList = new CopyOnWriteArrayList(Arrays.asList(doc.split("\n")));
            String hash = Hashing.sha1().hashString(domain, Charsets.UTF_8).toString();
            String starHash = Hashing.sha1().hashString("*." + domain, Charsets.UTF_8).toString();
            boolean blackListed = responseList.contains(hash) || responseList.contains(starHash);
            EntityUtils.consume(response.getEntity());
            return blackListed;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return true;
    }
}
