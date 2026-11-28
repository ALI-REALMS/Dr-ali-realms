package com.alirealms.impactfx.client;

import net.fabricmc.api.ClientModInitializer;

public class ImpactClient implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) ->
            dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("impactfx")
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("on").executes((ctx) ->
                {
                    ImpactRenderer.enabled = true;
                    ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("Impact FX: ON"));
                    return 1;
                }))
                .then(net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal("off").executes((ctx) ->
                {
                    ImpactRenderer.enabled = false;
                    ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("Impact FX: OFF"));
                    return 1;
                }))
                .executes((ctx) ->
                {
                    ImpactRenderer.enabled = !ImpactRenderer.enabled;
                    ctx.getSource().sendFeedback(net.minecraft.text.Text.literal("Impact FX: " + (ImpactRenderer.enabled ? "ON" : "OFF")));
                    return 1;
                })));

        ImpactRenderer.init();
        PowerRenderer.init();
        SlashRenderer.init();
        RiftRenderer.init();
    }
}
