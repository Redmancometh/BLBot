package com.redmancometh.muckfojang.config;

public class Configuration
{
    private CloudflareConfig cloudflareConfig;
    private DomainConfig domainConfig;
    private ZoneConfiguration individualZones;

    public DomainConfig getDomainConfig()
    {
        return domainConfig;
    }

    public void setDomainConfig(DomainConfig domainConfig)
    {
        this.domainConfig = domainConfig;
    }

    public CloudflareConfig getCloudflareConfig()
    {
        return cloudflareConfig;
    }

    public void setCloudflareConfig(CloudflareConfig cloudflareConfig)
    {
        this.cloudflareConfig = cloudflareConfig;
    }

    public ZoneConfiguration getIndividualZones()
    {
        return individualZones;
    }

    public void setIndividualZones(ZoneConfiguration individualZones)
    {
        this.individualZones = individualZones;
    }

}
