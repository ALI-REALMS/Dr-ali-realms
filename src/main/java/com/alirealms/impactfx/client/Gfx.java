package com.alirealms.impactfx.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Tiny drawing layer. This is the ONLY file that differs between Minecraft versions
 * (1.21.1 here, 1.20.1 on the other branch).
 */
final class Gfx
{
    private static BufferBuilder buffer;

    /* shader-pack (Iris) mode: geometry is recorded and drawn later with our own shader */
    static ShaderProgram glow;
    private static boolean late;
    private static boolean additive = true;
    private static Matrix4f proj = new Matrix4f();
    private static final Vector4f tmp = new Vector4f();
    private static final List<Batch> queue = new ArrayList<>();
    private static Batch cur;
    private static int queued;

    private static final class Batch
    {
        final boolean add;
        float[] d = new float[7 * 64];
        int n;

        Batch(boolean add)
        {
            this.add = add;
        }

        void put(float x, float y, float z, float r, float g, float b, float a)
        {
            if (n * 7 + 7 > d.length)
            {
                d = java.util.Arrays.copyOf(d, d.length * 2);
            }

            int i = n * 7;

            d[i] = x; d[i + 1] = y; d[i + 2] = z; d[i + 3] = r; d[i + 4] = g; d[i + 5] = b; d[i + 6] = a;
            n++;
        }
    }


    static void flushLate()
    {
        if (queue.isEmpty())
        {
            return;
        }

        ShaderProgram p = glow;

        if (p != null)
        {
            RenderSystem.setShader(() -> p);

            GlUniform u = p.getUniform("Proj");

            if (u != null)
            {
                u.set(proj);
            }

            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();

            for (Batch b : queue)
            {
                RenderSystem.blendFunc(770, b.add ? 1 : 771);

                BufferBuilder bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

                for (int i = 0; i < b.n; i++)
                {
                    int k = i * 7;

                    bb.vertex(b.d[k], b.d[k + 1], b.d[k + 2]).color(b.d[k + 3], b.d[k + 4], b.d[k + 5], b.d[k + 6]);
                }

                BufferRenderer.drawWithGlobalProgram(bb.end());
            }

            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
        }

        queue.clear();
        queued = 0;
    }

    private Gfx() {}

    static float partial(WorldRenderContext context)
    {
        return MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(false);
    }

    /** additive glow state */
    static void setup()
    {
        if (Compat.iris() && glow != null)
        {
            RenderSystem.setShader(() -> glow);
        }
        else
        {
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(770, 1);
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
    }

    /** normal alpha blending (for dark voids) */
    static void blendNormal()
    {
        if (late)
        {
            additive = false;
            return;
        }

        RenderSystem.blendFunc(770, 771);
    }

    /** additive glow blending */
    static void blendAdd()
    {
        if (late)
        {
            additive = true;
            return;
        }

        RenderSystem.blendFunc(770, 1);
    }

    static void cleanup()
    {
        if (late)
        {
            return;
        }

        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    static void begin()
    {
        if (late)
        {
            cur = new Batch(additive);
            return;
        }

        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
    }

    static void v(Matrix4f m, float x, float y, float z, float r, float g, float b, float a)
    {
        if (late)
        {
            m.transform(x, y, z, 1F, tmp);
            cur.put(tmp.x, tmp.y, tmp.z, clamp(r) / 255F, clamp(g) / 255F, clamp(b) / 255F, clamp(a) / 255F);
            return;
        }

        buffer.vertex(m, x, y, z).color(clamp(r), clamp(g), clamp(b), clamp(a));
    }

    static void end()
    {
        if (late)
        {
            if (cur != null && cur.n > 0)
            {
                if (queued > 600000)
                {
                    queue.clear();
                    queued = 0;
                }

                queued += cur.n;
                queue.add(cur);
            }

            cur = null;
            return;
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        buffer = null;
    }

    private static int clamp(float v)
    {
        return Math.max(0, Math.min(255, (int) (v * 255F)));
    }
}
