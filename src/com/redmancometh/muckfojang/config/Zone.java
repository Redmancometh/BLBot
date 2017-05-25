package com.redmancometh.muckfojang.config;

import java.util.List;

public class Zone
{
    private String zone;
    private List<String> subdomains;
    private boolean uniformSubdomain;

    public Zone(String zone, List<String> subdomains, boolean uniformSubdomain)
    {
        super();
        this.zone = zone;
        this.subdomains = subdomains;
        this.uniformSubdomain = uniformSubdomain;
    }

    public String getZone()
    {
        return zone;
    }

    public void setZone(String zone)
    {
        this.zone = zone;
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
