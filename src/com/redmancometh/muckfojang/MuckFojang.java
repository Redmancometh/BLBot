package com.redmancometh.muckfojang;

public class MuckFojang
{
    private static MuckClient client;

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

}
