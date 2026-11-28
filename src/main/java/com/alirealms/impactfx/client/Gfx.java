package com.alirealms.impactfx.client;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.util.OptionalInt;

/**
 * Tiny drawing layer for Minecraft 1.21.11 (new RenderPipeline / RenderPass API).
 * Vertices are transformed to view space on the CPU, then projected by our own shader.
 */
final class Gfx
{
    private static RenderPipeline add;
    private static RenderPipeline normal;

    private static BufferAllocator allocator;
    private static BufferBuilder buffer;
    private static boolean additive = true;
    private static Matrix4f proj = new Matrix4f();
    private static int count;

    private Gfx() {}

    private static RenderPipeline pipeline(String name, BlendFunction blend)
    {
        return net.minecraft.client.gl.RenderPipelines.register(RenderPipeline.builder()
            .withLocation(Identifier.of("impactfx", "pipeline/" + name))
            .withVertexShader(Identifier.of("impactfx", "core/glow"))
            .withFragmentShader(Identifier.of("impactfx", "core/glow"))
            .withUniform("GlowData", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withBlend(blend)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build());
    }

    static void initLate()
    {
        net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents.START_MAIN.register((context) -> clearLate());
    }

    static float partial(WorldRenderContext context)
    {
        return MinecraftClient.getInstance().getRenderTickCounter().getTickProgress(false);
    }

    /** the projection matrix of the world camera */
    static Matrix4f projection()
    {
        Matrix4f p = mchorse.bbs_mod.client.BBSRendering.getWorldProjection();

        if (p == null || p.m00() == 0F || Float.isNaN(p.m00()))
        {
            MinecraftClient mc = MinecraftClient.getInstance();
            float fov = (float) Math.toRadians(mc.options.getFov().getValue());
            float aspect = mc.getWindow().getFramebufferWidth() / (float) Math.max(1, mc.getWindow().getFramebufferHeight());

            return new Matrix4f().perspective(fov, aspect, 0.05F, 1024F);
        }

        return new Matrix4f(p);
    }

    /** additive glow state */
    static void setup()
    {
        if (add == null)
        {
            add = pipeline("glow_add", BlendFunction.LIGHTNING);
            normal = pipeline("glow_normal", BlendFunction.TRANSLUCENT);
        }

        proj = projection();
        additive = true;
    }

    /** normal alpha blending (for dark voids) */
    static void blendNormal()
    {
        additive = false;
    }

    /** additive glow blending */
    static void blendAdd()
    {
        additive = true;
    }

    static void cleanup()
    {
    }

    private static boolean pipe;

    static void begin()
    {
        count = 0;
        pipe = additive;

        if (allocator == null)
        {
            allocator = new BufferAllocator(1 << 16);
        }

        buffer = new BufferBuilder(allocator, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
    }

    static void v(Matrix4f m, float x, float y, float z, float r, float g, float b, float a)
    {
        count++;
        buffer.vertex(m, x, y, z).color(clamp(r), clamp(g), clamp(b), clamp(a));
    }

    static void end()
    {
        BuiltBuffer built = buffer == null ? null : buffer.endNullable();

        buffer = null;

        if (built == null)
        {
            return;
        }

        if (Compat.iris())
        {
            /* shader pack: keep the geometry and draw it from the InGameHud mixin, after the pack's final pass */
            try (built)
            {
                java.nio.ByteBuffer src = built.getBuffer();
                java.nio.ByteBuffer copy = java.nio.ByteBuffer.allocateDirect(src.remaining()).order(src.order());

                copy.put(src.duplicate());
                copy.flip();

                if (queued.size() > 4000)
                {
                    queued.clear();
                }

                queued.add(new Queued(copy, built.getDrawParameters().mode(), built.getDrawParameters().indexCount(), pipe, new Matrix4f(proj)));
            }

            return;
        }

        try (built)
        {
            draw(built.getBuffer(), built.getDrawParameters().mode(), built.getDrawParameters().indexCount(), pipe, proj,
                MinecraftClient.getInstance().getFramebuffer());
        }
    }

    private static final class Queued
    {
        final java.nio.ByteBuffer data;
        final VertexFormat.DrawMode mode;
        final int indexCount;
        final boolean add;
        final Matrix4f proj;

        Queued(java.nio.ByteBuffer data, VertexFormat.DrawMode mode, int indexCount, boolean add, Matrix4f proj)
        {
            this.data = data;
            this.mode = mode;
            this.indexCount = indexCount;
            this.add = add;
            this.proj = proj;
        }
    }

    private static final java.util.List<Queued> queued = new java.util.ArrayList<>();

    /** draws what was recorded while a shader pack was active; target = framebuffer holding the finished image */
    static void flushLate(Framebuffer target)
    {
        if (queued.isEmpty())
        {
            return;
        }

        for (Queued q : queued)
        {
            draw(q.data.duplicate().order(q.data.order()), q.mode, q.indexCount, q.add, q.proj, target);
        }

        queued.clear();
    }

    static void clearLate()
    {
        queued.clear();
    }

    private static void draw(java.nio.ByteBuffer data, VertexFormat.DrawMode mode, int indexCount, boolean additivePipe, Matrix4f projection, Framebuffer fb)
    {
        RenderPipeline p = additivePipe ? add : normal;

        if (p == null)
        {
            return;
        }

        GpuBuffer vb = p.getVertexFormat().uploadImmediateVertexBuffer(data);
        RenderSystem.ShapeIndexBuffer shape = RenderSystem.getSequentialBuffer(mode);
        GpuBuffer ib = shape.getIndexBuffer(indexCount);
        GpuBuffer ubo;

        try (MemoryStack stack = MemoryStack.stackPush())
        {
            Std140Builder b = Std140Builder.onStack(stack, 64);

            b.putMat4f(projection);
            ubo = RenderSystem.getDevice().createBuffer(() -> "impactfx glow", GpuBuffer.USAGE_UNIFORM, b.get());
        }

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
            .createRenderPass(() -> "impactfx glow", fb.getColorAttachmentView(), OptionalInt.empty()))
        {
            pass.setPipeline(p);
            pass.setUniform("GlowData", ubo);
            pass.setVertexBuffer(0, vb);
            pass.setIndexBuffer(ib, shape.getIndexType());
            pass.drawIndexed(0, 0, indexCount, 1);
        }

        ubo.close();
    }

    private static int clamp(float v)
    {
        return Math.max(0, Math.min(255, (int) (v * 255F)));
    }
}
