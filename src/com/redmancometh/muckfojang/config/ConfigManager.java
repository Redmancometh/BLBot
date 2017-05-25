package com.redmancometh.muckfojang.config;

import java.io.FileReader;
import java.lang.reflect.Field;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ConfigManager
{
    private Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES).create();

    private Configuration config;

    public void init()
    {
        initConfig();
    }

    public void initConfig()
    {
        try (FileReader reader = new FileReader("config\\config.json"))
        {
            try
            {
                for (Field f : DomainConfig.class.getDeclaredFields())
                {
                    System.out.println(FieldNamingPolicy.LOWER_CASE_WITH_DASHES.translateName(f));
                }
                Configuration conf = gson.fromJson(reader, Configuration.class);
                this.config = conf;
                System.out.println(config.getCloudflareConfig().getAuthKey());
                System.out.println(config.getCloudflareConfig().getCfEmail());
                System.out.println((config.getDomainConfig() == null) + " DOMAIN CONF NULL");
                config.getDomainConfig().getSubdomainList().forEach((item) -> System.out.println(item));
                config.getDomainConfig().getTargetDomains().forEach((item) -> System.out.println(item));

            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public Configuration getConfig()
    {
        return config;
    }

    public void setConfig(Configuration config)
    {
        this.config = config;
    }

}
