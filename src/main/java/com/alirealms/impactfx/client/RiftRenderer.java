package com.alirealms.impactfx.client;

import com.alirealms.impactfx.RiftClip;
import mchorse.bbs_mod.api.client.events.FilmEvents;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** 3D space rift: dark void lens + glowing core + jagged rim + sparks. Uses PowerRenderer's primitives. */
public final class RiftRenderer
{
    private static final float PI = (float) Math.PI;
    private static float vr, vg, vb;      /* void color */
    private static float voidAlpha;

    private RiftRenderer() {}

    public static void init()
    {
        FilmEvents.RENDER_AFTER.register(RiftRenderer::render);
    }

    private static float clamp01(float x)
    {
        return Math.max(0F, Math.min(1F, x));
    }

    private static float ease(float x)
    {
        x = clamp01(x);

        return 1F - (1F - x) * (1F - x) * (1F - x);
    }

    private static void render(BaseFilmController controller, WorldRenderContext context)
    {
        if (controller == null || controller.film == null || controller.film.camera == null || context.matrixStack() == null)
        {
            return;
        }

        List<RiftClip> clips = controller.film.camera.getClips(RiftClip.class);

        if (clips.isEmpty())
        {
            return;
        }

        int tick = controller.getTick();
        float partial = Gfx.partial(context);
        Vec3d cam = context.camera().getPos();
        List<Replay> replays = controller.film.replays.getList();
        boolean began = false;

        context.camera().getRotation().transform(1F, 0F, 0F, PowerRenderer.camRight);
        context.camera().getRotation().transform(0F, 1F, 0F, PowerRenderer.camUp);
        PowerRenderer.m = context.matrixStack().peek().getPositionMatrix();

        for (RiftClip clip : clips)
        {
            if (!clip.enabled.get() || !clip.isInside(tick))
            {
                continue;
            }

            int duration = Math.max(1, clip.duration.get());
            float rel = (tick - clip.tick.get()) + partial;
            float amount = clip.envelope.factorEnabled(duration, rel);

            if (amount <= 0.001F)
            {
                continue;
            }

            int index = clip.actor.get() - 1;

            if (index < 0 || index >= replays.size())
            {
                continue;
            }

            IEntity entity = controller.getEntities().get(replays.get(index).getId());

            if (entity == null)
            {
                continue;
            }

            double x = entity.getPrevX() + (entity.getX() - entity.getPrevX()) * partial;
            double y = entity.getPrevY() + (entity.getY() - entity.getPrevY()) * partial;
            double z = entity.getPrevZ() + (entity.getZ() - entity.getPrevZ()) * partial;
            float ent = entity.getPrevYaw() + (entity.getYaw() - entity.getPrevYaw()) * partial;

            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) || Float.isNaN(ent))
            {
                continue;
            }

            if (!began)
            {
                Gfx.setup();
                began = true;
            }

            float[] c = new float[3];

            PowerRenderer.unpack(clip.core.get(), c);
            PowerRenderer.cr = c[0]; PowerRenderer.cg = c[1]; PowerRenderer.cb = c[2];
            PowerRenderer.unpack(clip.edge.get(), c);
            PowerRenderer.er = c[0]; PowerRenderer.eg = c[1]; PowerRenderer.eb = c[2];
            PowerRenderer.unpack(clip.voidColor.get(), c);
            vr = c[0]; vg = c[1]; vb = c[2];
            PowerRenderer.gain = clip.intensity.get() * amount;
            voidAlpha = 0.94F * amount;

            float yawRad = (float) Math.toRadians(ent + clip.yaw.get());
            float fx = -(float) Math.sin(yawRad), fz = (float) Math.cos(yawRad);
            float rx = fz, rz = -fx;                      /* horizontal right */
            float dist = clip.dist.get();
            float cx = (float) (x - cam.x) + clip.offX.get() + fx * dist;
            float cy = (float) (y - cam.y) + clip.offY.get();
            float cz = (float) (z - cam.z) + clip.offZ.get() + fz * dist;
            float p = clamp01(rel / duration);
            float time = rel / 20F * clip.pulse.get();
            float L = clip.length.get(), W = clip.width.get();
            int seed = clip.seed.get();

            if (clip.type.get() == 2)
            {
                float[] tilts = {clip.tilt.get(), clip.tilt.get() + 60F, clip.tilt.get() - 55F};
                float[] lens = {1F, 0.75F, 0.85F};

                for (int k = 0; k < 3; k++)
                {
                    float ox = (PowerRenderer.hash(k * 2.3F + seed) - 0.5F) * L * 0.4F;
                    float oy = (PowerRenderer.hash(k * 5.9F + seed) - 0.5F) * L * 0.3F;

                    shape(cx + rx * ox, cy + oy, cz + rz * ox, rx, rz, tilts[k], L * lens[k], W, openAnim(clamp01(p * 1.5F - k * 0.2F)),
                          time + k, clip.jagged.get(), seed + k * 7, false, clip.sparks.get() / 3);
                }
            }
            else
            {
                shape(cx, cy, cz, rx, rz, clip.tilt.get(), L, W, openAnim(p), time, clip.jagged.get(), seed, clip.type.get() == 1, clip.sparks.get());
            }
        }

        if (began)
        {
            Gfx.cleanup();
        }
    }

    /** open fast, hold, close at the end */
    private static float openAnim(float p)
    {
        return ease(p / 0.22F) * (1F - clamp01((p - 0.8F) / 0.2F));
    }

    private static float prof(float s, boolean round)
    {
        float a = Math.min(1F, Math.abs(s));

        return round ? (float) Math.sqrt(1F - a * a) : (float) Math.pow(Math.cos(PI * 0.5F * a), 1.1);
    }

    private static void shape(float cx, float cy, float cz, float rx, float rz, float tiltDeg, float L, float W, float open,
                              float time, float jag, int seed, boolean round, int sparks)
    {
        if (open <= 0.01F)
        {
            return;
        }

        float t = (float) Math.toRadians(tiltDeg);
        /* up axis in the rift plane (rolled) and right axis */
        float ux = rx * (float) Math.sin(t), uy = (float) Math.cos(t), uz = rz * (float) Math.sin(t);
        float qx = rx * (float) Math.cos(t), qy = -(float) Math.sin(t), qz = rz * (float) Math.cos(t);
        int N = 36;
        float len = L * 0.5F * (round ? 1F : open);
        float wid = W * (float) Math.pow(open, 0.6F) * (1F + 0.07F * (float) Math.sin(time * 6.28F));
        float step = (float) Math.floor(time * 8F);
        float[] sx = new float[N + 1], sy = new float[N + 1], sz = new float[N + 1], sw = new float[N + 1];

        for (int i = 0; i <= N; i++)
        {
            float s = -1F + 2F * i / N;
            float j = 1F + jag * (PowerRenderer.hash(i * 1.7F + seed + step * 0.37F) - 0.5F) * 0.7F * (Math.abs(s) < 0.97F ? 1F : 0F);
            float w = wid * prof(s, round) * (round ? 1F : j);
            float along = s * len;

            sx[i] = cx + ux * along; sy[i] = cy + uy * along; sz[i] = cz + uz * along;
            sw[i] = w;
        }

        /* 1) dark void, normal blending */
        Gfx.blendNormal();
        Gfx.begin();

        for (int i = 0; i < N; i++)
        {
            Gfx.v(PowerRenderer.m, sx[i] - qx * sw[i] * 0.92F, sy[i] - qy * sw[i] * 0.92F, sz[i] - qz * sw[i] * 0.92F, vr, vg, vb, voidAlpha);
            Gfx.v(PowerRenderer.m, sx[i] + qx * sw[i] * 0.92F, sy[i] + qy * sw[i] * 0.92F, sz[i] + qz * sw[i] * 0.92F, vr, vg, vb, voidAlpha);
            Gfx.v(PowerRenderer.m, sx[i + 1] + qx * sw[i + 1] * 0.92F, sy[i + 1] + qy * sw[i + 1] * 0.92F, sz[i + 1] + qz * sw[i + 1] * 0.92F, vr, vg, vb, voidAlpha);
            Gfx.v(PowerRenderer.m, sx[i + 1] - qx * sw[i + 1] * 0.92F, sy[i + 1] - qy * sw[i + 1] * 0.92F, sz[i + 1] - qz * sw[i + 1] * 0.92F, vr, vg, vb, voidAlpha);
        }

        Gfx.end();

        /* 2) additive: inner glow ridge + rim */
        Gfx.blendAdd();
        Gfx.begin();

        float pulse = 0.8F + 0.2F * (float) Math.sin(time * 6.28F * 1.3F);

        for (int i = 0; i < N; i++)
        {
            float s0 = -1F + 2F * i / N, s1 = -1F + 2F * (i + 1) / N;
            float a0 = (float) Math.pow(Math.max(0F, 1F - s0 * s0), 0.7F) * 0.9F * pulse;
            float a1 = (float) Math.pow(Math.max(0F, 1F - s1 * s1), 0.7F) * 0.9F * pulse;

            for (int side = -1; side <= 1; side += 2)
            {
                /* ridge: center (core) -> edge of void (edge color, dim) */
                PowerRenderer.vert(sx[i], sy[i], sz[i], 0F, a0);
                PowerRenderer.vert(sx[i + 1], sy[i + 1], sz[i + 1], 0F, a1);
                PowerRenderer.vert(sx[i + 1] + qx * sw[i + 1] * side * 0.9F, sy[i + 1] + qy * sw[i + 1] * side * 0.9F, sz[i + 1] + qz * sw[i + 1] * side * 0.9F, 1F, a1 * 0.25F);
                PowerRenderer.vert(sx[i] + qx * sw[i] * side * 0.9F, sy[i] + qy * sw[i] * side * 0.9F, sz[i] + qz * sw[i] * side * 0.9F, 1F, a0 * 0.25F);

                /* rim: bright at the edge, fading outwards */
                float o0 = 1.0F + 0.45F + 0.2F * PowerRenderer.hash(i * 3.1F + seed + step), o1 = 1.0F + 0.45F + 0.2F * PowerRenderer.hash((i + 1) * 3.1F + seed + step);
                float e0 = Math.max(0.15F, 1F - Math.abs(s0) * 0.3F), e1 = Math.max(0.15F, 1F - Math.abs(s1) * 0.3F);

                PowerRenderer.vert(sx[i] + qx * sw[i] * side * 0.92F, sy[i] + qy * sw[i] * side * 0.92F, sz[i] + qz * sw[i] * side * 0.92F, 0.6F, 0.9F * e0 * Math.min(1F, a0 + 0.3F));
                PowerRenderer.vert(sx[i + 1] + qx * sw[i + 1] * side * 0.92F, sy[i + 1] + qy * sw[i + 1] * side * 0.92F, sz[i + 1] + qz * sw[i + 1] * side * 0.92F, 0.6F, 0.9F * e1 * Math.min(1F, a1 + 0.3F));
                PowerRenderer.vert(sx[i + 1] + qx * sw[i + 1] * side * o1 + qx * W * 0.1F * side, sy[i + 1] + qy * sw[i + 1] * side * o1 + qy * W * 0.1F * side, sz[i + 1] + qz * sw[i + 1] * side * o1 + qz * W * 0.1F * side, 1F, 0F);
                PowerRenderer.vert(sx[i] + qx * sw[i] * side * o0 + qx * W * 0.1F * side, sy[i] + qy * sw[i] * side * o0 + qy * W * 0.1F * side, sz[i] + qz * sw[i] * side * o0 + qz * W * 0.1F * side, 1F, 0F);
            }
        }

        /* tip glows + sparks */
        PowerRenderer.glow(sx[0], sy[0], sz[0], W * 0.5F, 0.5F, 0.6F * open);
        PowerRenderer.glow(sx[N], sy[N], sz[N], W * 0.5F, 0.5F, 0.6F * open);
        PowerRenderer.glow(cx, cy, cz, W * 0.9F, 0.3F, 0.25F * pulse * open);

        for (int k = 0; k < sparks; k++)
        {
            float ph = PowerRenderer.fract(time * 0.35F + PowerRenderer.hash(k * 1.3F + seed));
            float s = (PowerRenderer.hash(k * 3.3F + seed) * 2F - 1F) * 0.85F;
            float side = PowerRenderer.hash(k * 7.7F + seed) > 0.5F ? 1F : -1F;
            float w = wid * prof(s, round);
            float out = w * (1.05F + ph * 1.2F) + ph * W * 0.9F;
            float along = s * len + ph * 0.5F * W;
            float size = (0.012F + 0.05F * W) * (1F - ph);

            PowerRenderer.glow(cx + ux * along + qx * out * side, cy + uy * along + qy * out * side, cz + uz * along + qz * out * side,
                size, 0.2F, (1F - ph) * open);
        }

        Gfx.end();
    }
}
