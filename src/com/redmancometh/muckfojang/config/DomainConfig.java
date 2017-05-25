package com.redmancometh.muckfojang.config;

import java.util.List;

public class DomainConfig
{
    private List<String> targetDomains;
    private List<String> subdomainList;

    public List<String> getTargetDomains()
    {
        return targetDomains;
    }

    public void setTargetDomains(List<String> targetDomains)
    {
        this.targetDomains = targetDomains;
    }

    public List<String> getSubdomainList()
    {
        return subdomainList;
    }

    public void setSubdomainList(List<String> subdomainList)
    {
        this.subdomainList = subdomainList;
    }

}
