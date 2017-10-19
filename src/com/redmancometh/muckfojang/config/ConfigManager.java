package com.redmancometh.muckfojang.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ConfigManager
{

    /**
     * Lol magic key away the fields I want to ignore.
     */
    private Gson gson = new GsonBuilder().excludeFieldsWithModifiers(Modifier.PROTECTED).setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_DASHES).create();

    private Configuration config;

    public void init()
    {
        initConfig();
    }

    public void initConfig()
    {
        try (FileReader reader = new FileReader("config" + File.separator + "config.json"))
        {
            Configuration conf = gson.fromJson(reader, Configuration.class);
            this.config = conf;
        }
        catch (IOException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
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
