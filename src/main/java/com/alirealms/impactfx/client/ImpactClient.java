package com.alirealms.impactfx.client;

import net.fabricmc.api.ClientModInitializer;

public class ImpactClient implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        ImpactRenderer.init();
    }
}
