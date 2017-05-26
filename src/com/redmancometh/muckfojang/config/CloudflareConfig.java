package com.redmancometh.muckfojang.config;

public class CloudflareConfig
{
    private String cfEmail;
    private String authKey;
    private int checkInterval;

    public String getCfEmail()
    {
        return cfEmail;
    }

    public void setCfEmail(String cfEmail)
    {
        this.cfEmail = cfEmail;
    }

    public String getAuthKey()
    {
        return authKey;
    }

    public void setAuthKey(String authKey)
    {
        this.authKey = authKey;
    }

    public int getCheckInterval()
    {
        return checkInterval;
    }

    public void setCheckInterval(int checkInterval)
    {
        this.checkInterval = checkInterval;
    }
}
