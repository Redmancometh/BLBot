package com.redmancometh.muckfojang;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CloudflareClient
{
    //https://api.cloudflare.com/client/v4/zones/

    CloseableHttpClient masterClient = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
    private String email;
    private String authKey;
    private Gson reader = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    public CloudflareClient(String email, String authKey)
    {
        super();
        this.email = email;
        this.authKey = authKey;
    }

    public CompletableFuture<List<String>> requestZones()
    {
        List<String> zoneList = new ArrayList();
        HttpGet updateRequest = new HttpGet("http://api.cloudflare.com/client/v4/zones/");
        updateRequest.addHeader("X-Auth-Email", email);
        updateRequest.addHeader("X-Auth-Key", authKey);
        updateRequest.addHeader("Content-Type", "application/json");
        try
        {
            try (CloseableHttpResponse response = masterClient.execute(updateRequest))
            {
                try (InputStreamReader inputReader = new InputStreamReader(response.getEntity().getContent()))
                {
                    EntityUtils.consume(response.getEntity());
                    JsonParser parser = new JsonParser();
                    JsonObject obj = parser.parse(inputReader).getAsJsonObject();
                    System.out.println(obj.toString());
                    int zoneId = obj.get("result").getAsJsonObject().get("id").getAsInt();
                    System.out.println("Zone ID: " + zoneId);
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return CompletableFuture.supplyAsync(() -> zoneList);
    }

    public static JSONObject initContent(HttpPut updateRequest, String name, String id, String domain)
    {
        updateRequest.addHeader("X-Auth-Email", "serayne05@gmail.com");
        updateRequest.addHeader("X-Auth-Key", "382b8c99a921b352707fb91ce6adc4b3a0d7d");
        updateRequest.addHeader("Content-Type", "application/json");
        JSONObject json = new JSONObject();
        JSONObject srvContent = new JSONObject();

        srvContent.put("service", "_minecraft");
        srvContent.put("proto", "_tcp");
        srvContent.put("name", name);
        srvContent.put("priority", 1);
        srvContent.put("weight", 1);
        srvContent.put("port", 25565);
        srvContent.put("target", domain);
        json.put("type", "SRV");
        json.put("name", "_minecraft._tcp." + name + ".arkhamnetwork.org");
        if (name.equalsIgnoreCase("arkhamnetwork.org"))
        {
            json.put("name", "_minecraft._tcp.arkhamnetwork.org");
        }
        json.put("data", srvContent);
        return json;
    }

    public List<String> getZones()
    {
        return null;
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
