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
        return RenderPipeline.builder()
            .withLocation(Identifier.of("impactfx", "pipeline/" + name))
            .withVertexShader(Identifier.of("impactfx", "core/glow"))
            .withFragmentShader(Identifier.of("impactfx", "core/glow"))
            .withUniform("GlowData", UniformType.UNIFORM_BUFFER)
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
            .withBlend(blend)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();
    }

    static void initLate()
    {
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

        RenderPipeline p = pipe ? add : normal;

        try (built)
        {
            GpuBuffer vb = p.getVertexFormat().uploadImmediateVertexBuffer(built.getBuffer());
            RenderSystem.ShapeIndexBuffer shape = RenderSystem.getSequentialBuffer(built.getDrawParameters().mode());
            GpuBuffer ib = shape.getIndexBuffer(built.getDrawParameters().indexCount());
            Framebuffer fb = MinecraftClient.getInstance().getFramebuffer();
            GpuBuffer ubo;

            try (MemoryStack stack = MemoryStack.stackPush())
            {
                Std140Builder b = Std140Builder.onStack(stack, 64);

                b.putMat4f(proj);
                ubo = RenderSystem.getDevice().createBuffer(() -> "impactfx glow", GpuBuffer.USAGE_UNIFORM, b.get());
            }

            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
                .createRenderPass(() -> "impactfx glow", fb.getColorAttachmentView(), OptionalInt.empty()))
            {
                pass.setPipeline(p);
                pass.setUniform("GlowData", ubo);
                pass.setVertexBuffer(0, vb);
                pass.setIndexBuffer(ib, shape.getIndexType());
                pass.drawIndexed(0, 0, built.getDrawParameters().indexCount(), 1);
            }

            ubo.close();
        }
    }

    private static int clamp(float v)
    {
        return Math.max(0, Math.min(255, (int) (v * 255F)));
    }
}
