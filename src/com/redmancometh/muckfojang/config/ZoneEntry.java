package com.redmancometh.muckfojang.config;

import java.util.List;

public class ZoneEntry
{
    private String name;
    private List<String> subdomains;
    private List<String> targetDomains;
    private boolean uniformSubdomain;

    public ZoneEntry(String zone, List<String> subdomains, boolean uniformSubdomain)
    {
        super();
        this.name = zone;
        this.subdomains = subdomains;
        this.uniformSubdomain = uniformSubdomain;
    }

    public String getName()
    {
        return name;
    }

    public void setZone(String zone)
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
