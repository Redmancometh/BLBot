package com.redmancometh.muckfojang.config;

import lombok.Data;

@Data
public class CloudflareConfig
{
    private String cfEmail;
    private String authKey;
    private int checkInterval;
}
