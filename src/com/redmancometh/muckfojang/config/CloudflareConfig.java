package com.redmancometh.muckfojang.config;

public class CloudflareConfig
{
    private String cfEmail;
    private String authKey;

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
}
