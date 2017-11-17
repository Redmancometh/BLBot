package com.redmancometh.muckfojang.pojo;

import java.util.List;

import lombok.Data;

@Data
/**
 * We don't really care about most of the fields in the response.
 * @author Redmancometh
 *
 */
public class ZoneListResponse
{
    List<ZoneListEntry> result;
}
