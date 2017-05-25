package com.redmancometh.muckfojang.config;

import java.util.List;

public class Zone
{
    private String name;
    private List<String> subdomains;
    private boolean uniformSubdomain;
    protected String zoneId;

    public String getZoneId()
    {
        return zoneId;
    }

    public void setZoneId(String zoneId)
    {
        this.zoneId = zoneId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String zone)
    {
        this.name = zone;
    }

    public List<String> getSubdomains()
    {
        return subdomains;
    }

    public void setSubdomains(List<String> subdomains)
    {
        this.subdomains = subdomains;
    }

    public boolean isUniformSubdomain()
    {
        return uniformSubdomain;
    }

    public void setUniformSubdomain(boolean uniformSubdomain)
    {
        this.uniformSubdomain = uniformSubdomain;
    }
}
