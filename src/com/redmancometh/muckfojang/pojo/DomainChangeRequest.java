package com.redmancometh.muckfojang.pojo;

import lombok.Data;
import lombok.NonNull;

/**
 * A pojo representing a request to change a domain
 * @author Redmancometh
 *
 */
@Data
public class DomainChangeRequest
{
    protected String type = "SRV";
    @NonNull
    private String name;
    @NonNull
    private SRVContent data;
}
