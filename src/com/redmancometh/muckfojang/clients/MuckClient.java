package com.redmancometh.muckfojang.clients;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.redmancometh.muckfojang.config.ConfigManager;
import com.redmancometh.muckfojang.config.Configuration;

import lombok.Data;

@Data
public class MuckClient
{
    private CloudflareClient client;
    private ConfigManager configManager;
    private ScheduledExecutorService tickTimer = Executors.newScheduledThreadPool(4);

    public void start()
    {
        configManager = new ConfigManager();
        configManager.init();
        Configuration config = configManager.getConfig();
        System.out.println("Config: " + config);
        client = new CloudflareClient(config.getCloudflareConfig().getCfEmail(), config.getCloudflareConfig().getAuthKey());
        client.initializeZones().thenRun(() ->
        {
            tickTimer.scheduleAtFixedRate(() -> tick(), 0, configManager.getConfig().getCloudflareConfig().getCheckInterval() * 1000, TimeUnit.MILLISECONDS);
        });
    }

    public void tick()
    {
        client.checkZones();
    }

}
