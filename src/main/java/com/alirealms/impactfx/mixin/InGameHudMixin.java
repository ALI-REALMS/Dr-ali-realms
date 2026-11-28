package com.alirealms.impactfx.mixin;

import com.alirealms.impactfx.client.ImpactRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * BBS cancels InGameHud.render while its camera is running, so Fabric's HudRenderCallback never fires then.
 * This runs before BBS' own injection (lower priority number) and draws the impact effect with shader packs.
 */
@Mixin(value = InGameHud.class, priority = 500)
public class InGameHudMixin
{
    @Inject(method = "render", at = @At("HEAD"))
    private void impactfx$beforeHud(CallbackInfo info)
    {
        ImpactRenderer.renderHud();
    }
}
