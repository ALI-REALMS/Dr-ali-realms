package com.alirealms.impactfx.client;

import com.alirealms.impactfx.RiftClip;
import mchorse.bbs_mod.api.client.events.FilmEvents;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.util.math.Vec3d;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Matrix4f;

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

        Matrix4f base = PowerRenderer.m;

        for (RiftClip clip : clips)
        {
            PowerRenderer.m = base;

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
            Transform xt = clip.xform.get();
            float[] lo = FxXform.local(xt, fx, fz, rx, rz);
            float cx = (float) (x - cam.x) + clip.offX.get() + fx * dist + lo[0];
            float cy = (float) (y - cam.y) + clip.offY.get() + lo[1];
            float cz = (float) (z - cam.z) + clip.offZ.get() + fz * dist + lo[2];

            PowerRenderer.m = FxXform.around(base, xt, cx, cy, cz);
            float p = clamp01(rel / duration);
            float time = rel / 20F * clip.pulse.get();
            float L = clip.length.get(), W = clip.width.get();
            int seed = clip.seed.get();

            int ty = clip.type.get();

            if (ty >= 3)
            {
                float gx = cx, gy = cy, gz = cz;

                special(ty, clip, gx, gy, gz, rx, rz, p, time, L, W, seed);

                continue;
            }

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

        /* Blender-style streak sparks flying out of the tear + short electric arcs along it */
        EnergyFx.streaks(cx, cy, cz, sparks * 2, W * 2.2F, 0.35F, 1.6F, time, seed + 5, W * 0.03F, open, 0F);
        EnergyFx.arcsAlong(sx, sy, sz, N, qx, qy, qz, W, time, seed, open);

        Gfx.end();
    }

    /* ====================================================== extra effects (types 3..6) */

    static float[] cross(float ax, float ay, float az, float bx, float by, float bz)
    {
        return new float[] {ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx};
    }

    /** camera facing ribbon between two points */
    static void ribbon(float x0, float y0, float z0, float x1, float y1, float z1, float w0, float w1, float t, float a0, float a1)
    {
        ribbonG(x0, y0, z0, x1, y1, z1, w0, w1, t, t, a0, a1);
    }

    /** camera facing feathered ribbon with a colour gradient t0 -> t1 along its length */
    static void ribbonG(float x0, float y0, float z0, float x1, float y1, float z1, float w0, float w1, float t0, float t1, float a0, float a1)
    {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float mx = (x0 + x1) * 0.5F, my = (y0 + y1) * 0.5F, mz = (z0 + z1) * 0.5F;
        float[] sd = cross(dx, dy, dz, mx, my, mz);
        float l = (float) Math.sqrt(sd[0] * sd[0] + sd[1] * sd[1] + sd[2] * sd[2]);

        if (l < 1e-6F)
        {
            return;
        }

        sd[0] /= l; sd[1] /= l; sd[2] /= l;

        /* feathered cross-section: 0, .35, 1, .35, 0 */
        float[] off = {-1F, -0.5F, 0F, 0.5F, 1F};
        float[] fa = {0F, 0.35F, 1F, 0.35F, 0F};

        for (int k = 0; k < 4; k++)
        {
            PowerRenderer.vert(x0 + sd[0] * w0 * off[k], y0 + sd[1] * w0 * off[k], z0 + sd[2] * w0 * off[k], t0, a0 * fa[k]);
            PowerRenderer.vert(x0 + sd[0] * w0 * off[k + 1], y0 + sd[1] * w0 * off[k + 1], z0 + sd[2] * w0 * off[k + 1], t0, a0 * fa[k + 1]);
            PowerRenderer.vert(x1 + sd[0] * w1 * off[k + 1], y1 + sd[1] * w1 * off[k + 1], z1 + sd[2] * w1 * off[k + 1], t1, a1 * fa[k + 1]);
            PowerRenderer.vert(x1 + sd[0] * w1 * off[k], y1 + sd[1] * w1 * off[k], z1 + sd[2] * w1 * off[k], t1, a1 * fa[k]);
        }
    }

    /** white-hot narrow ribbon for the core of a bolt */
    static void ribbonHot(float x0, float y0, float z0, float x1, float y1, float z1, float w, float a)
    {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float mx = (x0 + x1) * 0.5F, my = (y0 + y1) * 0.5F, mz = (z0 + z1) * 0.5F;
        float[] sd = cross(dx, dy, dz, mx, my, mz);
        float l = (float) Math.sqrt(sd[0] * sd[0] + sd[1] * sd[1] + sd[2] * sd[2]);

        if (l < 1e-6F)
        {
            return;
        }

        sd[0] /= l; sd[1] /= l; sd[2] /= l;

        float[] off = {-1F, 0F, 1F};
        float[] fa = {0F, 1F, 0F};

        for (int k = 0; k < 2; k++)
        {
            PowerRenderer.vertHot(x0 + sd[0] * w * off[k], y0 + sd[1] * w * off[k], z0 + sd[2] * w * off[k], a * fa[k]);
            PowerRenderer.vertHot(x0 + sd[0] * w * off[k + 1], y0 + sd[1] * w * off[k + 1], z0 + sd[2] * w * off[k + 1], a * fa[k + 1]);
            PowerRenderer.vertHot(x1 + sd[0] * w * off[k + 1], y1 + sd[1] * w * off[k + 1], z1 + sd[2] * w * off[k + 1], a * fa[k + 1]);
            PowerRenderer.vertHot(x1 + sd[0] * w * off[k], y1 + sd[1] * w * off[k], z1 + sd[2] * w * off[k], a * fa[k]);
        }
    }

    /** rays in the camera plane (star flare) */
    static void flare(float x, float y, float z, int n, float len, float w, float alpha, float rot)
    {
        for (int i = 0; i < n; i++)
        {
            float a = rot + i * PI / n * 2F;
            float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
            float dx = PowerRenderer.camRight.x * ca + PowerRenderer.camUp.x * sa;
            float dy = PowerRenderer.camRight.y * ca + PowerRenderer.camUp.y * sa;
            float dz = PowerRenderer.camRight.z * ca + PowerRenderer.camUp.z * sa;
            float px = PowerRenderer.camRight.x * -sa + PowerRenderer.camUp.x * ca;
            float py = PowerRenderer.camRight.y * -sa + PowerRenderer.camUp.y * ca;
            float pz = PowerRenderer.camRight.z * -sa + PowerRenderer.camUp.z * ca;
            float l = len * (0.6F + 0.4F * PowerRenderer.hash(i * 3.7F));

            PowerRenderer.vert(x - px * w, y - py * w, z - pz * w, 0F, alpha);
            PowerRenderer.vert(x + px * w, y + py * w, z + pz * w, 0F, alpha);
            PowerRenderer.vert(x + dx * l, y + dy * l, z + dz * l, 1F, 0F);
            PowerRenderer.vert(x + dx * l, y + dy * l, z + dz * l, 1F, 0F);
        }
    }

    /** small glowing rectangle (debris shard) facing the camera */
    private static void shard(float x, float y, float z, float w, float h, float ang, float t, float alpha)
    {
        float c = (float) Math.cos(ang), s = (float) Math.sin(ang);
        float[] ux = {PowerRenderer.camRight.x * c + PowerRenderer.camUp.x * s, PowerRenderer.camRight.y * c + PowerRenderer.camUp.y * s, PowerRenderer.camRight.z * c + PowerRenderer.camUp.z * s};
        float[] vx = {-PowerRenderer.camRight.x * s + PowerRenderer.camUp.x * c, -PowerRenderer.camRight.y * s + PowerRenderer.camUp.y * c, -PowerRenderer.camRight.z * s + PowerRenderer.camUp.z * c};

        for (int k = 0; k < 4; k++)
        {
            float sx = (k == 0 || k == 3) ? -1F : 1F, sy = (k < 2) ? -1F : 1F;

            PowerRenderer.vert(x + ux[0] * w * sx + vx[0] * h * sy, y + ux[1] * w * sx + vx[1] * h * sy, z + ux[2] * w * sx + vx[2] * h * sy, t, alpha);
        }
    }

    private static void special(int type, RiftClip c, float x, float y, float z, float rx, float rz, float p, float time, float L, float W, int seed)
    {
        float open = openAnim(p);
        float jag = c.jagged.get();
        int sparks = c.sparks.get();

        switch (type)
        {
            case 3: lightning(x, y, z, p, time, L, W, jag, sparks, seed, true); break;
            case 4: cracksOnly(x, y, z, p, time, L, W, sparks, seed); break;
            case 5: vortex(x, y, z, p, time, W, jag, sparks, seed); break;
            case 7: EnergyFx.burst(c, x, y - c.offY.get() + 0.03F, z, y, p, time, L, W, jag, sparks, seed, vr, vg, vb); break;
            case 8: EnergyFx.sweep(c, x, y - c.offY.get() + 0.02F, z, y, rx, rz, p, time, L, W, jag, sparks, seed); break;
            case 9: EnergyFx.orb(c, x, y - c.offY.get() + 0.02F, z, y, p, time, W, jag, sparks, seed); break;
            case 10: EnergyFx.shower(c, x, y - c.offY.get() + 0.02F, z, y, p, time, L, W, sparks, seed); break;
            default: slit(c, x, y, z, rx, rz, p, time, L, W, jag, sparks, seed); break;
        }
    }

    /* ---------- lightning strike ---------- */

    private static void lightning(float x, float y, float z, float p, float time, float H, float W, float jag, int sparks, int seed, boolean withCracks)
    {
        float vis = clamp01(p / 0.08F) * (1F - clamp01((p - 0.75F) / 0.25F));
        float flash = (float) Math.pow(1F - clamp01(p * 1.2F), 1.6F);
        float step = (float) Math.floor(time * 14F);

        /* ground glow, cracks */
        Gfx.blendAdd();
        Gfx.begin();
        glowDisc(x, y + 0.02F, z, W * 1.9F, 0.5F * vis + 0.3F * flash);

        if (vis > 0.01F)
        {
            for (int b = 0; b < 2; b++)
            {
                float topx = x + (b == 0 ? 0.35F : -0.3F) * H * 0.35F + (PowerRenderer.hash(b * 5.1F + seed) - 0.5F) * H * 0.1F;
                float topz = z + (b == 0 ? 0.1F : 0F) * H;
                float px = topx, py = y + H, pz = topz;
                int n = 16;

                for (int i = 1; i <= n; i++)
                {
                    float f = (float) i / n;
                    float jx = (PowerRenderer.hash(i * 1.7F + b * 9.3F + step + seed) - 0.5F) * H * 0.07F * jag * (1F - f * 0.6F);
                    float jz = (PowerRenderer.hash(i * 3.3F + b * 4.1F + step + seed) - 0.5F) * H * 0.04F * jag;
                    float nx = x + (topx - x) * (1F - f) + (i == n ? 0F : jx);
                    float ny = y + H * (1F - f);
                    float nz = z + (topz - z) * (1F - f) + (i == n ? 0F : jz);
                    float wd = W * 0.06F * (b == 0 ? 1F : 0.7F) * (0.6F + 0.4F * f);

                    float fl = 0.75F + 0.25F * PowerRenderer.hash(step * 1.9F + i * 0.7F + seed);       /* flicker */

                    ribbon(px, py, pz, nx, ny, nz, wd * 9F, wd * 9F, 1F, 0.14F * vis * fl, 0.14F * vis * fl);   /* wide bloom */
                    ribbon(px, py, pz, nx, ny, nz, wd * 3.5F, wd * 3.5F, 0.7F, 0.34F * vis * fl, 0.34F * vis * fl);  /* halo */
                    ribbon(px, py, pz, nx, ny, nz, wd * 1.2F, wd * 1.2F, 0F, 0.95F * vis, 0.95F * vis);        /* core */
                    ribbonHot(px, py, pz, nx, ny, nz, wd * 0.55F, vis);                                       /* white-hot */

                    /* small side forks */
                    if (i % 3 == 1 && PowerRenderer.hash(i * 2.1F + b * 6.7F + step + seed) > 0.35F)
                    {
                        float side = PowerRenderer.hash(i * 8.3F + step + seed) < 0.5F ? -1F : 1F;
                        float fx = nx + side * H * (0.04F + 0.05F * PowerRenderer.hash(i * 4.4F + step));
                        float fy = ny - H * 0.06F;
                        float fz = nz + (PowerRenderer.hash(i * 5.9F + step) - 0.5F) * H * 0.05F;

                        ribbon(nx, ny, nz, fx, fy, fz, wd * 1.8F, wd * 0.3F, 0.6F, 0.35F * vis * fl, 0.0F);
                        ribbon(nx, ny, nz, fx, fy, fz, wd * 0.5F, wd * 0.1F, 0F, 0.8F * vis, 0F);
                    }

                    px = nx; py = ny; pz = nz;
                }
            }

            /* impact flare */
            glowAt(x, y + 0.1F, z, W * (0.5F + 0.6F * flash), 0.3F, 0.9F * vis);
            flare(x, y + 0.1F, z, 8, W * (0.9F + 1.4F * flash), W * 0.03F, 0.9F * vis, time * 0.5F);
        }

        /* flying sparks: motion streaks */
        EnergyFx.streaks(x, y + 0.1F, z, sparks * 3, W * 2.2F, 0.7F, 2.4F, time, seed, W * 0.025F, vis, 0.6F);

        Gfx.end();

        if (withCracks)
        {
            cracks(x, y + 0.03F, z, W * 1.5F, W * 0.07F, clamp01(p * 6F), seed, 5, 0.6F * (1F - clamp01((p - 0.6F) / 0.4F)));
        }
    }

    private static void glowAt(float x, float y, float z, float size, float t, float a)
    {
        PowerRenderer.glow(x, y, z, size, t, a);
    }

    static void glowDisc(float x, float y, float z, float r, float a)
    {
        int n = 28;
        int rings = 5;

        for (int k = 0; k < rings; k++)
        {
            float r0 = (float) k / rings, r1 = (float) (k + 1) / rings;
            float f0 = PowerRenderer.falloff(r0), f1 = k + 1 == rings ? 0F : PowerRenderer.falloff(r1);

            for (int i = 0; i < n; i++)
            {
                float a0 = i * 6.2832F / n, a1 = (i + 1) * 6.2832F / n;
                float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);

                PowerRenderer.vert(x + c0 * r * r0, y, z + s0 * r * r0, r0, a * f0);
                PowerRenderer.vert(x + c1 * r * r0, y, z + s1 * r * r0, r0, a * f0);
                PowerRenderer.vert(x + c1 * r * r1, y, z + s1 * r * r1, r1, a * f1);
                PowerRenderer.vert(x + c0 * r * r1, y, z + s0 * r * r1, r1, a * f1);
            }
        }
    }

    /* ---------- ground cracks ---------- */

    private static void cracksOnly(float x, float y, float z, float p, float time, float L, float W, int sparks, int seed)
    {
        float grow = ease(p / 0.25F);
        float fade = 1F - clamp01((p - 0.7F) / 0.3F);

        Gfx.blendAdd();
        Gfx.begin();
        glowDisc(x, y + 0.02F, z, W * 2.2F, 0.35F * fade);

        for (int k = 0; k < sparks; k++)
        {
            float ph = PowerRenderer.fract(time * 0.45F + PowerRenderer.hash(k * 1.3F + seed));
            float a = PowerRenderer.hash(k * 3.7F + seed) * 6.2832F;
            float sp = (0.3F + PowerRenderer.hash(k * 7.1F + seed)) * W * 1.6F;

            shard(x + (float) Math.cos(a) * sp * ph, y + 0.1F + (ph * 2F - ph * ph * 2.2F) * W * 0.7F, z + (float) Math.sin(a) * sp * ph,
                W * 0.03F, W * 0.07F, k + time, 0.2F, (1F - ph) * fade);
        }

        Gfx.end();
        cracks(x, y + 0.03F, z, L * 0.5F, W * 0.1F, grow, seed, 7, fade);
    }

    /** branching fissures on the ground: dark inside, glowing rim */
    private static void cracks(float x, float y, float z, float R, float width, float grow, int seed, int branches, float glow)
    {
        if (grow <= 0.01F)
        {
            return;
        }

        java.util.ArrayList<float[]> segs = new java.util.ArrayList<>();

        for (int b = 0; b < branches; b++)
        {
            float ang = b * 6.2832F / branches + (PowerRenderer.hash(b * 2.1F + seed) - 0.5F) * 0.8F;
            float len = R * (0.55F + 0.45F * PowerRenderer.hash(b * 4.3F + seed)) * grow;

            walk(segs, x, z, ang, len, 9, width, seed + b * 13, 2);
        }

        /* dark inside */
        Gfx.blendNormal();
        Gfx.begin();

        for (float[] sg : segs)
        {
            flat(sg, y, 1F, 0F, 0F, 0F, 0.92F, true);
        }

        Gfx.end();

        /* glowing rim */
        float g0 = PowerRenderer.gain;

        PowerRenderer.gain = g0 * glow;
        Gfx.blendAdd();
        Gfx.begin();

        for (float[] sg : segs)
        {
            flat(sg, y + 0.005F, 2.2F, 1F, 0F, 0F, 0F, false);
        }

        Gfx.end();
        PowerRenderer.gain = g0;
    }

    /** polyline walker; each segment = {x0,z0,x1,z1,w0,w1} */
    private static void walk(java.util.List<float[]> out, float x, float z, float ang, float len, int steps, float width, int seed, int depth)
    {
        float step = len / steps;
        float cx = x, cz = z, a = ang;

        for (int i = 0; i < steps; i++)
        {
            a += (PowerRenderer.hash(i * 1.9F + seed) - 0.5F) * 0.9F;

            float nx = cx + (float) Math.cos(a) * step, nz = cz + (float) Math.sin(a) * step;
            float w0 = width * (1F - (float) i / steps), w1 = width * (1F - (float) (i + 1) / steps);

            out.add(new float[] {cx, cz, nx, nz, w0, w1});

            if (depth > 0 && (i == 2 || i == 5) && PowerRenderer.hash(i * 5.3F + seed) > 0.25F)
            {
                float side = PowerRenderer.hash(i * 7.7F + seed) > 0.5F ? 1F : -1F;

                walk(out, nx, nz, a + side * (0.7F + 0.5F * PowerRenderer.hash(i * 9.1F + seed)), len * 0.4F, 4, width * 0.7F, seed + i * 31, depth - 1);
            }

            cx = nx; cz = nz;
        }
    }

    /** flat quad for a crack segment. inside=true: full width void; false: rim strips */
    private static void flat(float[] sg, float y, float rimScale, float t, float u0, float u1, float alpha, boolean inside)
    {
        float dx = sg[2] - sg[0], dz = sg[3] - sg[1];
        float l = (float) Math.sqrt(dx * dx + dz * dz);

        if (l < 1e-6F)
        {
            return;
        }

        float nx = -dz / l, nz = dx / l;

        if (inside)
        {
            Gfx.v(PowerRenderer.m, sg[0] - nx * sg[4], y, sg[1] - nz * sg[4], vr, vg, vb, alpha * (voidAlpha > 0 ? 1F : 0F));
            Gfx.v(PowerRenderer.m, sg[0] + nx * sg[4], y, sg[1] + nz * sg[4], vr, vg, vb, alpha * (voidAlpha > 0 ? 1F : 0F));
            Gfx.v(PowerRenderer.m, sg[2] + nx * sg[5], y, sg[3] + nz * sg[5], vr, vg, vb, alpha * (voidAlpha > 0 ? 1F : 0F));
            Gfx.v(PowerRenderer.m, sg[2] - nx * sg[5], y, sg[3] - nz * sg[5], vr, vg, vb, alpha * (voidAlpha > 0 ? 1F : 0F));
        }
        else
        {
            for (int side = -1; side <= 1; side += 2)
            {
                PowerRenderer.vert(sg[0] + nx * sg[4] * side, y, sg[1] + nz * sg[4] * side, 0F, 0.9F);
                PowerRenderer.vert(sg[2] + nx * sg[5] * side, y, sg[3] + nz * sg[5] * side, 0F, 0.9F);
                PowerRenderer.vert(sg[2] + nx * sg[5] * side * rimScale, y, sg[3] + nz * sg[5] * side * rimScale, 1F, 0F);
                PowerRenderer.vert(sg[0] + nx * sg[4] * side * rimScale, y, sg[1] + nz * sg[4] * side * rimScale, 1F, 0F);
            }
        }
    }

    /* ---------- wind vortex orb of shredded ribbons ---------- */

    private static void vortex(float x, float y, float z, float p, float time, float R, float jag, int sparks, int seed)
    {
        float open = ease(p / 0.2F) * (1F - clamp01((p - 0.6F) / 0.4F));
        int n = Math.max(6, sparks);
        int segs = 18;

        Gfx.blendAdd();
        Gfx.begin();

        for (int k = 0; k < n; k++)
        {
            float u = PowerRenderer.hash(k * 1.3F + seed) * 2F - 1F;
            float a = PowerRenderer.hash(k * 2.7F + seed) * 6.2832F;
            float s = (float) Math.sqrt(1F - u * u);
            float ax = s * (float) Math.cos(a), ay = u, az = s * (float) Math.sin(a);      /* orbit axis */
            /* any vector perpendicular to the axis */
            float[] bv = Math.abs(ay) < 0.9F ? cross(ax, ay, az, 0F, 1F, 0F) : cross(ax, ay, az, 1F, 0F, 0F);
            float bl = (float) Math.sqrt(bv[0] * bv[0] + bv[1] * bv[1] + bv[2] * bv[2]);

            bv[0] /= bl; bv[1] /= bl; bv[2] /= bl;

            float[] cv = cross(ax, ay, az, bv[0], bv[1], bv[2]);
            float th0 = PowerRenderer.hash(k * 5.9F + seed) * 6.2832F + time * (1.5F + PowerRenderer.hash(k * 8.3F + seed) * 2F) * (k % 2 == 0 ? 1F : -1F);
            float span = 1.2F + PowerRenderer.hash(k * 6.1F + seed) * 1.4F;
            float wmax = R * (0.1F + 0.14F * PowerRenderer.hash(k * 3.3F + seed));
            float rad = R * (0.85F + 0.2F * PowerRenderer.hash(k * 4.7F + seed));
            float[] prev = null;

            for (int i = 0; i <= segs; i++)
            {
                float f = (float) i / segs;
                float th = th0 + span * f;
                float px = x + rad * (bv[0] * (float) Math.cos(th) + cv[0] * (float) Math.sin(th));
                float py = y + rad * (bv[1] * (float) Math.cos(th) + cv[1] * (float) Math.sin(th));
                float pz = z + rad * (bv[2] * (float) Math.cos(th) + cv[2] * (float) Math.sin(th));
                float rag = 0.55F + 0.45F * PowerRenderer.hash(i * 2.3F + k * 7.7F + seed + (float) Math.floor(time * 4F)) * jag;
                float w = wmax * (float) Math.sin(Math.PI * f) * rag;
                float al = open * (float) Math.sin(Math.PI * f) * 0.65F;

                if (prev != null)
                {
                    PowerRenderer.vert(prev[0] - ax * prev[3], prev[1] - ay * prev[3], prev[2] - az * prev[3], 1F, prev[4] * 0.4F);
                    PowerRenderer.vert(prev[0] + ax * prev[3], prev[1] + ay * prev[3], prev[2] + az * prev[3], 1F, prev[4] * 0.4F);
                    PowerRenderer.vert(px + ax * w, py + ay * w, pz + az * w, 1F, al * 0.4F);
                    PowerRenderer.vert(px - ax * w, py - ay * w, pz - az * w, 1F, al * 0.4F);

                    /* bright inner line */
                    PowerRenderer.vert(prev[0] - ax * prev[3] * 0.4F, prev[1] - ay * prev[3] * 0.4F, prev[2] - az * prev[3] * 0.4F, 0.2F, prev[4]);
                    PowerRenderer.vert(prev[0] + ax * prev[3] * 0.4F, prev[1] + ay * prev[3] * 0.4F, prev[2] + az * prev[3] * 0.4F, 0.2F, prev[4]);
                    PowerRenderer.vert(px + ax * w * 0.4F, py + ay * w * 0.4F, pz + az * w * 0.4F, 0.2F, al);
                    PowerRenderer.vert(px - ax * w * 0.4F, py - ay * w * 0.4F, pz - az * w * 0.4F, 0.2F, al);
                }

                prev = new float[] {px, py, pz, w, al};
            }
        }

        /* small hot core at the start, ground haze */
        PowerRenderer.glow(x, y, z, R * 0.25F * (1F - open * 0.7F), 0.8F, 0.8F * (1F - clamp01(p * 3F)));
        Gfx.end();
    }

    /* ---------- vertical light slit with flare and rising column ---------- */

    private static void slit(RiftClip c, float x, float y, float z, float rx, float rz, float p, float time, float L, float W, float jag, int sparks, int seed)
    {
        float open = openAnim(p);

        /* soft rising column behind (additive) */
        Gfx.blendAdd();
        Gfx.begin();

        float colH = L * 1.1F, colW = W * 5F * open;
        float baseY = y - L * 0.5F;

        for (int i = 0; i < 8; i++)
        {
            float f0 = i / 8F, f1 = (i + 1) / 8F;
            float a0 = (float) Math.pow(1F - f0, 1.4F) * 0.3F * open, a1 = (float) Math.pow(1F - f1, 1.4F) * 0.3F * open;

            PowerRenderer.vert(x - rx * colW * (1F - f0 * 0.3F), baseY + colH * f0, z - rz * colW * (1F - f0 * 0.3F), 1F, a0);
            PowerRenderer.vert(x + rx * colW * (1F - f0 * 0.3F), baseY + colH * f0, z + rz * colW * (1F - f0 * 0.3F), 1F, a0);
            PowerRenderer.vert(x + rx * colW * (1F - f1 * 0.3F), baseY + colH * f1, z + rz * colW * (1F - f1 * 0.3F), 1F, a1);
            PowerRenderer.vert(x - rx * colW * (1F - f1 * 0.3F), baseY + colH * f1, z - rz * colW * (1F - f1 * 0.3F), 1F, a1);
        }

        Gfx.end();

        /* the slit itself: reuse the rift shape (void color is the body of the light) */
        shape(x, y, z, rx, rz, c.tilt.get(), L, W, open, time, jag, seed, false, sparks);

        /* flare */
        Gfx.blendAdd();
        Gfx.begin();

        float fl = (float) Math.sin(Math.PI * clamp01(p * 1.3F));

        PowerRenderer.glow(x, y, z, W * 4F * fl, 0.4F, 0.6F * open);
        flare(x, y, z, 4, W * 9F * fl, W * 0.25F, 0.9F * open, 0.785F);
        Gfx.end();
    }
}
