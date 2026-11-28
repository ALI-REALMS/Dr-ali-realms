package com.alirealms.impactfx.client;

import com.alirealms.impactfx.ImpactClip;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.api.client.events.FilmEvents;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.forms.entities.IEntity;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.List;

/**
 * Full-screen post effect. The BBS film controller that rendered this frame is remembered in
 * FilmEvents.RENDER_AFTER; the effect itself is drawn at WorldRenderEvents.LAST so that the sky,
 * terrain, entities, particles and weather are already in the main framebuffer.
 */
public final class ImpactRenderer
{
    private static ShaderProgram program;
    private static SimpleFramebuffer copy;
    private static volatile BaseFilmController pending;
    private static Quaternionf snapRot = new Quaternionf();
    private static Vec3d snapPos = Vec3d.ZERO;
    private static Matrix4f snapProj = new Matrix4f();

    public static volatile boolean enabled = true;

    private ImpactRenderer() {}

    public static void init()
    {
        CoreShaderRegistrationCallback.EVENT.register((context) ->
            {
                context.register(new Identifier("impactfx", "impact"), VertexFormats.POSITION, (p) -> program = p);
                context.register(new Identifier("impactfx", "glow"), VertexFormats.POSITION_COLOR, (p) -> Gfx.glow = p);
                context.register(new Identifier("impactfx", "glowlate"), VertexFormats.POSITION_COLOR, (p) -> Gfx.glowLate = p);
            });

      FilmEvents.RENDER_AFTER.register((controller, context) ->
{
    pending = controller != null
        && controller.film != null
        && controller.film.camera != null
        ? controller
        : null;

    if (pending != null)
    {
        snapRot = new Quaternionf(context.camera().getRotation());
        snapPos = context.camera().getPos();
        snapProj = new Matrix4f(RenderSystem.getProjectionMatrix());
    }
});

        WorldRenderEvents.LAST.register((context) ->
        {
            /* with a shader pack the main framebuffer only holds the finished image after the pack's final pass,
               so the effect is drawn later, from the HUD callback */
            if (!Compat.iris())
            {
                run(false);
            }
        });

        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((context, tickCounter) ->
        {
            if (Compat.iris())
            {
                run(true);
            }
        });

        Gfx.initLate();
    }

    private static void u4(String name, float a, float b, float c, float d)
    {
        GlUniform u = program.getUniform(name);

        if (u != null)
        {
            u.set(a, b, c, d);
        }
    }

    private static void color(String name, int rgb)
    {
        u4(name, ((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F, 1F);
    }

  private static void run(boolean hud)
{
    BaseFilmController controller = pending;

    pending = null;

        if (!enabled)
        {
            return;
        }

    if (controller == null || program == null || controller.film == null || controller.film.camera == null)
    {
        return;
    }

    MinecraftClient mc = MinecraftClient.getInstance();
    int tick = controller.getTick();
    float partial = mc.getTickDelta();

    /* find the strongest active impact clip */
    List<ImpactClip> clips = controller.film.camera.getClips(ImpactClip.class);

    ImpactClip best = null;
    float amount = 0F;
    float progress = 0F;
        float time = 0F;

        for (ImpactClip clip : clips)
        {
            if (!clip.enabled.get() || !clip.isInside(tick))
            {
                continue;
            }

            int duration = Math.max(1, clip.duration.get());
            float rel = (tick - clip.tick.get()) + partial;
            float factor = clip.envelope.factorEnabled(duration, rel);

            if (factor > amount)
            {
                amount = factor;
                best = clip;
                progress = Math.min(1F, Math.max(0F, rel / duration));
                time = rel;
            }
        }

        if (best == null || amount <= 0.001F)
        {
            return;
        }

        Framebuffer main = mc.getFramebuffer();
        int w = main.textureWidth;
        int h = main.textureHeight;

        if (copy == null)
        {
            copy = new SimpleFramebuffer(w, h, true, MinecraftClient.IS_SYSTEM_MAC);
        }
        else if (copy.textureWidth != w || copy.textureHeight != h)
        {
            copy.resize(w, h, MinecraftClient.IS_SYSTEM_MAC);
        }

        /* copy color + depth of the finished world */
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.fbo);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, copy.fbo);
        GL30.glBlitFramebuffer(0, 0, w, h, 0, 0, w, h, GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        main.beginWrite(false);

        /* matrices */
        Matrix4f invProj = new Matrix4f(snapProj).invert();
        Matrix4f camRot = new Matrix4f().rotation(snapRot);
        Vec3d cam = snapPos;

        GlUniform g = program.getUniform("InvProj");

        if (g != null)
        {
            g.set(invProj);
        }

        g = program.getUniform("CamRot");

        if (g != null)
        {
            g.set(camRot);
        }

        ImpactClip c = best;

        u4("P0", time, progress, amount, w / (float) h);
        u4("P1", c.mode.get(), c.threshold.get(), c.worldMix.get(), c.distort.get());
        u4("P2", c.actorRadius.get(), c.actorHeight.get(), c.aura.get(), c.auraWidth.get());
        u4("P3", c.starCount.get(), c.starSize.get(), c.starSeed.get(), c.starRate.get());
        u4("P4", c.speedLines.get(), c.shockwave.get(), c.inkBurst.get(), c.flash.get());
        u4("P5", c.centerX.get(), c.centerY.get(), c.grain.get(), c.posterize.get());
        u4("P6", c.shape.get(), c.shapeScale.get(), c.shapeSpeed.get(), c.shapeDensity.get());
        u4("P7", c.shapeGlow.get(), (float) Math.toRadians(c.shapeRot.get()), c.shapeFill.get(), Math.round(c.charMode.get()) + Math.min(1F, Math.max(0F, c.charShade.get())) * 0.99F);
        u4("P8", Compat.iris() ? 1F : 0F, 0F, 0F, 0F);
        color("ColS1", c.shapeCol1.get());
        color("ColS2", c.shapeCol2.get());
        color("ColBg", c.bg.get());
        color("ColInk", c.ink.get());
        color("ColInk2", c.inkAlt.get());
        color("ColAccent", c.accent.get());
        color("ColStar", c.starColor.get());

        /* actors (up to 6), position relative to the camera, interpolated */
        int n = 0;

        for (IEntity entity : controller.getEntities().values())
        {
            if (n >= 6)
            {
                break;
            }

            if (entity == null)
            {
                continue;
            }

            double x = entity.getPrevX() + (entity.getX() - entity.getPrevX()) * partial;
            double y = entity.getPrevY() + (entity.getY() - entity.getPrevY()) * partial;
            double z = entity.getPrevZ() + (entity.getZ() - entity.getPrevZ()) * partial;

            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z))
            {
                continue;
            }

            u4("Actor" + n, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z), 1F);
            n++;
        }

        for (; n < 6; n++)
        {
            u4("Actor" + n, 0F, 0F, 0F, 0F);
        }

        /* draw */
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.setShader(() -> program);
        RenderSystem.setShaderTexture(0, copy.getColorAttachment());
        RenderSystem.setShaderTexture(1, copy.getDepthAttachment());

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();

        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

        buffer.vertex(-1F, -1F, 0F).next();
        buffer.vertex(1F, -1F, 0F).next();
        buffer.vertex(1F, 1F, 0F).next();
        buffer.vertex(-1F, 1F, 0F).next();
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);

        if (hud)
        {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }
        else
        {
            RenderSystem.enableDepthTest();
        }
    }
}
