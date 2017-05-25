package com.redmancometh.muckfojang.config;

public class Configuration
{
    private CloudflareConfig cloudflareConfig;
    private DomainConfig domainConfig;

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

}
