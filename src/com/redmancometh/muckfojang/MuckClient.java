package com.redmancometh.muckfojang;

import java.util.Timer;
import java.util.TimerTask;

import com.redmancometh.muckfojang.config.ConfigManager;
import com.redmancometh.muckfojang.config.Configuration;

public class MuckClient
{
    private CloudflareClient client;
    private ConfigManager configManager;
    private Timer tickTimer = new Timer();

    public void start()
    {
        configManager = new ConfigManager();
        configManager.init();
        Configuration config = configManager.getConfig();
        client = new CloudflareClient(config.getCloudflareConfig().getCfEmail(), config.getCloudflareConfig().getAuthKey());
        client.initializeZones().thenRun(() ->
        {
            System.out.println("THEN RUN");
            tickTimer.scheduleAtFixedRate(new TimerTask()
            {
                @Override
                public void run()
                {
                    tick();
                }
            }, 0, 5000);
        });
    }

    public void tick()
    {
        System.out.println("TICK");
        client.checkZones();
    }

    public CloudflareClient getClient()
    {
        return client;
    }

    public void setClient(CloudflareClient client)
    {
        this.client = client;
    }

    public ConfigManager getConfigManager()
    {
        return configManager;
    }

    public void setConfigManager(ConfigManager configManager)
    {
        this.configManager = configManager;
    }

}
