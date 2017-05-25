package com.redmancometh.muckfojang.clients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class BlockCheckerClient
{
    private JSONParser parser = new JSONParser();

    public boolean isBlocked(String domain)
    {
        System.out.println("Checking domain: " + domain);
        CloseableHttpClient client = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
        HttpGet updateRequest = new HttpGet("https://mcapi.ca/blockedservers");
        try
        {
            HttpResponse response = client.execute(updateRequest);
            BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));
            JSONObject obj = (JSONObject) parser.parse(br);

            for (Object value : ((JSONObject) obj.get("found")).values())
            {
                String ip = (String) ((JSONObject) value).get("ip");
                if (ip.contains(domain))
                {
                    System.out.println("DOMAIN IS BLOCKED!");
                    return true;
                }
            }
            EntityUtils.consume(response.getEntity());
        }
        catch (IOException | ParseException e)
        {
            e.printStackTrace();
        }
        System.out.println("Domain is not blocked!");
        return false;
    }

    public static JSONObject initRequest(HttpPost updateRequest, String domain)
    {
        updateRequest.addHeader("host", "blocklist.tcpr.ca");
        JSONObject json = new JSONObject();
        json.put("ip", domain);
        System.out.println(json + "\n\n");
        return json;
    }

}
