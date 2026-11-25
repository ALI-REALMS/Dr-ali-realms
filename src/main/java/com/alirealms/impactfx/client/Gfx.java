package com.alirealms.impactfx.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

/**
 * Tiny drawing layer. This is the ONLY file that differs between Minecraft versions
 * (1.20.1 here, 1.21.1 on the other branch).
 */
final class Gfx
{
    private static BufferBuilder buffer;

    private Gfx() {}

    static float partial(WorldRenderContext context)
    {
        return context.tickDelta();
    }

    /** additive glow state */
    static void setup()
    {
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(770, 1);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
    }

    /** normal alpha blending (for dark voids) */
    static void blendNormal()
    {
        RenderSystem.blendFunc(770, 771);
    }

    /** additive glow blending */
    static void blendAdd()
    {
        RenderSystem.blendFunc(770, 1);
    }

    static void cleanup()
    {
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    static void begin()
    {
        buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
    }

    static void v(Matrix4f m, float x, float y, float z, float r, float g, float b, float a)
    {
        buffer.vertex(m, x, y, z).color(clamp(r), clamp(g), clamp(b), clamp(a)).next();
    }

    static void end()
    {
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        buffer = null;
    }

    private static int clamp(float v)
    {
        return Math.max(0, Math.min(255, (int) (v * 255F)));
    }
}
