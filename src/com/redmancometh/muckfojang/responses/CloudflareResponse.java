package com.redmancometh.muckfojang.responses;

import com.redmancometh.muckfojang.ResponseResult;

public class CloudflareResponse
{
    private boolean success;
    private ResponseResult result;
    
    public ResponseResult getResult()
    {
        return result;
    }

    public void setResult(ResponseResult result)
    {
        this.result = result;
    }

    public boolean isSuccess()
    {
        return success;
    }

    public void setSuccess(boolean success)
    {
        this.success = success;
    }

}
