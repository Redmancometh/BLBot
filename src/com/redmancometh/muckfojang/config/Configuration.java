package com.redmancometh.muckfojang.config;

import lombok.Data;

@Data
public class Configuration
{
    private CloudflareConfig cloudflareConfig;
    private DomainConfig domainConfig;
    private ZoneConfiguration individualZones;
}
