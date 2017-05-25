package com.redmancometh.muckfojang;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class FojangBlockChecker
{

    private JsonParser parser = new JsonParser();

    public static JSONObject initRequest(HttpPost updateRequest, String domain)
    {
        updateRequest.addHeader("host", "blocklist.tcpr.ca");
        JSONObject json = new JSONObject();
        json.put("ip", domain);
        System.out.println(json + "\n\n");
        return json;
    }

    public CompletableFuture<Boolean> isBlocked(String domain)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
            HttpGet updateRequest = new HttpGet("https://api.mcuuid.com/json/blacklist/" + domain);
            try
            {
                HttpResponse response = client.execute(updateRequest);
                InputStreamReader br = new InputStreamReader((response.getEntity().getContent()));
                JsonElement obj = parser.parse(br);
                EntityUtils.consume(response.getEntity());
                return obj.getAsJsonObject().get("blacklisted").getAsBoolean();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            System.out.println("Domain is not blocked!");
            return true;

        });
    }
}
