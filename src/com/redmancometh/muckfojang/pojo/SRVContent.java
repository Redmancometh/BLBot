package com.redmancometh.muckfojang.pojo;

import lombok.Data;
import lombok.NonNull;

/**
 * A pojo representing the "data" field in the DomainChangeRequest POJO
 * @author Redmancometh
 *
 */
@Data
public class SRVContent
{
    @NonNull
    private String name, target;
    protected int priority = 1, weight =1, port = 25565;
    protected String proto = "_tcp", service = "_minecraft";
}
