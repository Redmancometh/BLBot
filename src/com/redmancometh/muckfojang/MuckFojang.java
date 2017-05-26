package com.redmancometh.muckfojang;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.redmancometh.muckfojang.clients.MuckClient;

public class MuckFojang
{
    private static MuckClient client;
    private static Executor pool = Executors.newFixedThreadPool(8);

    public static void main(String[] args)
    {
        setClient(new MuckClient());
        client.start();
    }

    public static MuckClient getClient()
    {
        return client;
    }

    public static void setClient(MuckClient client)
    {
        MuckFojang.client = client;
    }

    public static Executor getPool()
    {
        return pool;
    }

    public static void setPool(Executor pool)
    {
        MuckFojang.pool = pool;
    }

}
