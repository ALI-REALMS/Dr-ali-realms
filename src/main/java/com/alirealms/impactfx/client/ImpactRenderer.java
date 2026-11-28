package com.alirealms.impactfx.client;

import com.alirealms.impactfx.ImpactClip;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import mchorse.bbs_mod.api.client.events.FilmEvents;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.forms.entities.IEntity;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.Camera;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.system.MemoryStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Full-screen post effect. The BBS film controller that rendered this frame is remembered in
 * FilmEvents.RENDER_AFTER; the effect itself is drawn at WorldRenderEvents.LAST so that the sky,
 * terrain, entities, particles and weather are already in the main framebuffer.
 */
public final class ImpactRenderer
{
    private static RenderPipeline pipeline;
    private static final Map<String, float[]> uniforms = new LinkedHashMap<>();
    private static SimpleFramebuffer copy;
    private static volatile BaseFilmController pending;
    private static Quaternionf snapRot = new Quaternionf();
    private static Vec3d snapPos = Vec3d.ZERO;
    private static Matrix4f snapProj = new Matrix4f();

    public static volatile boolean enabled = true;

    private ImpactRenderer() {}

    public static void init()
    {
      FilmEvents.RENDER_AFTER.register((controller, context) ->
{
    if (Compat.shadow())
    {
        return;
    }

    pending = controller != null
        && controller.film != null
        && controller.film.camera != null
        ? controller
        : null;

    if (pending != null)
    {
        Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();

        snapRot = new Quaternionf(camera.getRotation());
        snapPos = camera.getCameraPos();
        snapProj = Gfx.projection();
    }
});

        WorldRenderEvents.END_MAIN.register((context) ->
        {
            /* with a shader pack the main target only holds the finished image after the pack's final pass,
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

    /** called from the InGameHud mixin (before BBS cancels the HUD); only used with a shader pack */
    public static void renderHud()
    {
        if (Compat.iris())
        {
            Gfx.flushLate(hudTarget());
            run(true);
        }
    }

    /** the framebuffer the finished image is in: BBS's own one in the editor preview, the main one otherwise */
    private static Framebuffer hudTarget()
    {
        Framebuffer main = MinecraftClient.getInstance().getFramebuffer();

        try
        {
            Framebuffer fb = mchorse.bbs_mod.client.BBSRendering.getFramebuffer();

            if (fb != null && fb != main && mchorse.bbs_mod.client.BBSRendering.isCustomSize())
            {
                return fb;
            }
        }
        catch (Throwable ignored)
        {}

        return main;
    }

    private static void u4(String name, float a, float b, float c, float d)
    {
        uniforms.put(name, new float[] {a, b, c, d});
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

    if (controller == null || false || controller.film == null || controller.film.camera == null)
    {
        return;
    }

    MinecraftClient mc = MinecraftClient.getInstance();
    int tick = controller.getTick();
    float partial = Gfx.partial(null);

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

        /* shader pack + BBS editor preview: the image the editor shows lives in BBS's own framebuffer */
        Framebuffer main = mc.getFramebuffer();

        if (hud && Compat.iris())
        {
            try
            {
                Framebuffer fb = mchorse.bbs_mod.client.BBSRendering.getFramebuffer();

                if (fb != null && fb != main && mchorse.bbs_mod.client.BBSRendering.isCustomSize())
                {
                    main = fb;
                }
            }
            catch (Throwable ignored)
            {}
        }
        int w = main.textureWidth;
        int h = main.textureHeight;

        if (copy == null)
        {
            copy = new SimpleFramebuffer("impactfx copy", w, h, true);
        }
        else if (copy.textureWidth != w || copy.textureHeight != h)
        {
            copy.resize(w, h);
        }

        /* copy color + depth of the finished world */
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

        encoder.copyTextureToTexture(main.getColorAttachment(), copy.getColorAttachment(), 0, 0, 0, 0, 0, w, h);
        copy.copyDepthFrom(main);

        /* matrices */
        Matrix4f invProj = new Matrix4f(snapProj).invert();
        Matrix4f camRot = new Matrix4f().rotation(snapRot);
        Vec3d cam = snapPos;

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

        /* uniform block, in std140 order */
        String[] order = {"P0", "P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "ColS1", "ColS2", "ColBg", "ColInk", "ColInk2",
            "ColAccent", "ColStar", "Actor0", "Actor1", "Actor2", "Actor3", "Actor4", "Actor5"};
        GpuBuffer ubo;

        try (MemoryStack stack = MemoryStack.stackPush())
        {
            Std140Builder b = Std140Builder.onStack(stack, 128 + order.length * 16);

            b.putMat4f(invProj);
            b.putMat4f(camRot);

            for (String key : order)
            {
                float[] v = uniforms.getOrDefault(key, new float[4]);

                b.putVec4(v[0], v[1], v[2], v[3]);
            }

            ubo = RenderSystem.getDevice().createBuffer(() -> "impactfx data", GpuBuffer.USAGE_UNIFORM, b.get());
        }

        if (pipeline == null)
        {
            pipeline = net.minecraft.client.gl.RenderPipelines.register(RenderPipeline.builder()
                .withLocation(Identifier.of("impactfx", "pipeline/impact"))
                .withVertexShader(Identifier.of("impactfx", "core/impact"))
                .withFragmentShader(Identifier.of("impactfx", "core/impact"))
                .withSampler("Sampler0")
                .withSampler("Sampler1")
                .withUniform("ImpactData", UniformType.UNIFORM_BUFFER)
                .withVertexFormat(net.minecraft.client.render.VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .withoutBlend()
                .build());
        }

        try (RenderPass pass = encoder.createRenderPass(() -> "impactfx", main.getColorAttachmentView(), OptionalInt.empty()))
        {
            pass.setPipeline(pipeline);
            pass.setUniform("ImpactData", ubo);
            pass.bindTexture("Sampler0", copy.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            pass.bindTexture("Sampler1", copy.getDepthAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            pass.draw(0, 3);
        }

        ubo.close();
    }
}
