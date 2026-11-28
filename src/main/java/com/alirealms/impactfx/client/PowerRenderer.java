package com.alirealms.impactfx.client;

import com.alirealms.impactfx.PowerClip;
import mchorse.bbs_mod.api.client.events.FilmEvents;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.util.math.Vec3d;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/** Draws the procedural 3D power effects (additive glow geometry), camera relative. */
public final class PowerRenderer
{
    private static final float TAU = (float) (Math.PI * 2.0);

    /* per-frame draw state */
    static Matrix4f m;
    static Vector3f camRight = new Vector3f();
    static Vector3f camUp = new Vector3f();
    static float cr, cg, cb, er, eg, eb;   // core / edge colors
    static float gain;                      // brightness * envelope

    private PowerRenderer() {}

    public static void init()
    {
        FilmEvents.RENDER_AFTER.register((controller, context) ->
        {
            if (!Compat.shadow())
            {
                render(controller, context);
            }
        });
    }

    static float hash(float x)
    {
        float s = (float) Math.sin(x * 127.1F + 311.7F) * 43758.5453F;

        return s - (float) Math.floor(s);
    }

    static float fract(float x)
    {
        return x - (float) Math.floor(x);
    }

    static void unpack(int rgb, float[] out)
    {
        out[0] = ((rgb >> 16) & 255) / 255F;
        out[1] = ((rgb >> 8) & 255) / 255F;
        out[2] = (rgb & 255) / 255F;
    }

    private static void render(BaseFilmController controller, WorldRenderContext context)
    {
        if (controller == null || controller.film == null || controller.film.camera == null)
        {
            return;
        }

        List<PowerClip> clips = controller.film.camera.getClips(PowerClip.class);

        if (clips.isEmpty())
        {
            return;
        }

        int tick = controller.getTick();
        float partial = Gfx.partial(context);
        net.minecraft.client.render.Camera camera = net.minecraft.client.MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d cam = camera.getCameraPos();
        List<Replay> replays = controller.film.replays.getList();
        boolean began = false;

        camera.getRotation().transform(1F, 0F, 0F, camRight);
        camera.getRotation().transform(0F, 1F, 0F, camUp);
        m = new Matrix4f().rotation(new org.joml.Quaternionf(camera.getRotation()).conjugate());

        Matrix4f base = m;

        for (PowerClip clip : clips)
        {
            m = base;

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

            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z))
            {
                continue;
            }

            if (!began)
            {
                Gfx.setup();
                began = true;
            }

            Transform xt = clip.xform.get();
            float[] lo = FxXform.local(xt, 0F, 1F, 1F, 0F);
            float ox = (float) (x - cam.x) + clip.offX.get() + lo[0];
            float oy = (float) (y - cam.y) + clip.offY.get() + lo[1];
            float oz = (float) (z - cam.z) + clip.offZ.get() + lo[2];

            m = FxXform.around(base, xt, ox, oy, oz);
            float[] c = new float[3];

            unpack(clip.core.get(), c);
            cr = c[0]; cg = c[1]; cb = c[2];
            unpack(clip.edge.get(), c);
            er = c[0]; eg = c[1]; eb = c[2];
            gain = clip.intensity.get() * amount;

            float time = rel / 20F * clip.speed.get();
            float progress = Math.min(1F, Math.max(0F, rel / duration));

            Gfx.begin();

            switch (clip.type.get())
            {
                case 0: aura(clip, ox, oy, oz, time); break;
                case 1: pillar(clip, ox, oy, oz, time); break;
                case 2: rings(clip, ox, oy, oz, time); break;
                case 3: charge(clip, ox, oy, oz, time); break;
                case 4: burst(clip, ox, oy, oz, time, progress); break;
                default: ball(clip, ox, oy, oz, time); break;
            }

            Gfx.end();
        }

        if (began)
        {
            Gfx.cleanup();
        }
    }

    /* ------------------------------------------------------------ primitives */

    static void vert(float x, float y, float z, float t, float a)
    {
        /* t: 0 = core color, 1 = edge color */
        Gfx.v(m, x, y, z, cr + (er - cr) * t, cg + (eg - cg) * t, cb + (eb - cb) * t, a * gain);
    }

    /** white-hot vertex (blend of white into the core colour) */
    static void vertHot(float x, float y, float z, float a)
    {
        Gfx.v(m, x, y, z, 0.6F + cr * 0.4F, 0.6F + cg * 0.4F, 0.6F + cb * 0.4F, a * gain);
    }

    /** gaussian-like falloff 0..1 -> alpha factor */
    static float falloff(float r)
    {
        return (float) Math.exp(-3.2F * r * r) * (1F - r * r * r);
    }

    /** camera facing soft glow disc (smooth, bloom-like falloff built from rings) */
    static void glow(float x, float y, float z, float size, float t, float alpha)
    {
        int n = 16;
        int rings = 5;

        for (int k = 0; k < rings; k++)
        {
            float r0 = (float) k / rings, r1 = (float) (k + 1) / rings;
            float f0 = falloff(r0), f1 = k + 1 == rings ? 0F : falloff(r1);
            float tt0 = t * r0, tt1 = t * r1;

            for (int i = 0; i < n; i++)
            {
                float a0 = i * TAU / n, a1 = (i + 1) * TAU / n;
                float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);

                ring(x, y, z, size * r0, c0, s0, tt0, alpha * f0);
                ring(x, y, z, size * r0, c1, s1, tt0, alpha * f0);
                ring(x, y, z, size * r1, c1, s1, tt1, alpha * f1);
                ring(x, y, z, size * r1, c0, s0, tt1, alpha * f1);
            }
        }
    }

    private static void ring(float x, float y, float z, float rad, float c, float s, float t, float a)
    {
        vert(x + (camRight.x * c + camUp.x * s) * rad, y + (camRight.y * c + camUp.y * s) * rad, z + (camRight.z * c + camUp.z * s) * rad, t, a);
    }

    /** flat horizontal glow disc on the ground */
    private static void groundDisc(float x, float y, float z, float radius, float alpha)
    {
        int n = 28;
        int rings = 5;

        for (int k = 0; k < rings; k++)
        {
            float r0 = (float) k / rings, r1 = (float) (k + 1) / rings;
            float f0 = falloff(r0), f1 = k + 1 == rings ? 0F : falloff(r1);

            for (int i = 0; i < n; i++)
            {
                float a0 = i * TAU / n, a1 = (i + 1) * TAU / n;
                float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);

                vert(x + c0 * radius * r0, y, z + s0 * radius * r0, r0, alpha * f0);
                vert(x + c1 * radius * r0, y, z + s1 * radius * r0, r0, alpha * f0);
                vert(x + c1 * radius * r1, y, z + s1 * radius * r1, r1, alpha * f1);
                vert(x + c0 * radius * r1, y, z + s0 * radius * r1, r1, alpha * f1);
            }
        }
    }

    /** flat annulus on the ground */
    private static void annulus(float x, float y, float z, float inner, float outer, float alphaInner, float alphaOuter, float t)
    {
        int n = 56;

        for (int i = 0; i < n; i++)
        {
            float a0 = i * TAU / n, a1 = (i + 1) * TAU / n;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);

            vert(x + c0 * inner, y, z + s0 * inner, 0F, alphaInner);
            vert(x + c1 * inner, y, z + s1 * inner, 0F, alphaInner);
            vert(x + c1 * outer, y, z + s1 * outer, t, alphaOuter);
            vert(x + c0 * outer, y, z + s0 * outer, t, alphaOuter);
        }
    }

    /** vertical flame-like strip that always faces the camera horizontally */
    private static void flame(float x, float y, float z, float width, float height, float sway, float t, float alpha)
    {
        float hx = -x, hz = -z;
        float len = (float) Math.sqrt(hx * hx + hz * hz);

        if (len < 1e-4F)
        {
            return;
        }

        float rx = hz / len, rz = -hx / len;   /* horizontal right vector */

        vert(x - rx * width, y, z - rz * width, 0F, alpha);
        vert(x + rx * width, y, z + rz * width, 0F, alpha);
        vert(x + rx * width * 0.15F + sway, y + height, z + rz * width * 0.15F, t, 0F);
        vert(x - rx * width * 0.15F + sway, y + height, z - rz * width * 0.15F, t, 0F);
    }

    /* ------------------------------------------------------------ effects */

    private static void aura(PowerClip c, float ox, float oy, float oz, float time)
    {
        int n = c.count.get();
        float R = c.radius.get() * c.scale.get();
        float H = c.height.get() * c.scale.get();
        int seed = c.seed.get();

        groundDisc(ox, oy + 0.02F, oz, R * 1.5F, 0.55F + 0.2F * (float) Math.sin(time * 6F));

        for (int i = 0; i < n; i++)
        {
            float ph = fract(time * 0.7F + hash(i * 1.37F + seed) );
            float ang = hash(i * 2.11F + seed) * TAU;
            float r = R * (0.55F + 0.6F * hash(i * 5.3F + seed));
            float rise = ph * H * 0.9F;
            float fx = ox + (float) Math.cos(ang) * r;
            float fz = oz + (float) Math.sin(ang) * r;
            float w = R * (0.28F + 0.22F * hash(i * 9.1F + seed)) * (1F - ph * 0.6F);
            float h = H * (0.35F + 0.35F * hash(i * 3.9F + seed));
            float sway = (float) Math.sin(time * 5F + i) * 0.12F * c.scale.get();

            flame(fx, oy + rise * 0.5F, fz, w, h, sway, ph, (float) Math.pow(1F - ph, 1.4F) * 0.9F);
        }

        /* inner glow around the body */
        glow(ox, oy + H * 0.5F, oz, R * 1.6F, 1F, 0.18F + 0.06F * (float) Math.sin(time * 9F));
    }

    private static void pillar(PowerClip c, float ox, float oy, float oz, float time)
    {
        float R = c.radius.get() * c.scale.get();
        float H = c.height.get();
        int sides = 28, stacks = 8;

        groundDisc(ox, oy + 0.02F, oz, R * 2.8F, 0.8F);

        for (int layer = 0; layer < 3; layer++)
        {
            float rad = R * (layer == 0 ? 1F : layer == 1 ? 0.62F : 0.3F) * (1F + 0.06F * (float) Math.sin(time * 8F + layer));
            float baseAlpha = layer == 0 ? 0.22F : layer == 1 ? 0.4F : 0.8F;
            float tcol = layer == 0 ? 1F : layer == 1 ? 0.5F : 0F;

            for (int s = 0; s < sides; s++)
            {
                float a0 = s * TAU / sides, a1 = (s + 1) * TAU / sides;
                float x0 = (float) Math.cos(a0) * rad, z0 = (float) Math.sin(a0) * rad;
                float x1 = (float) Math.cos(a1) * rad, z1 = (float) Math.sin(a1) * rad;

                for (int k = 0; k < stacks; k++)
                {
                    float y0 = H * k / stacks, y1 = H * (k + 1) / stacks;
                    float f0 = (float) Math.pow(1F - (float) k / stacks, 0.6F) * (0.75F + 0.25F * (float) Math.sin(y0 * 0.8F - time * 12F));
                    float f1 = (float) Math.pow(1F - (float) (k + 1) / stacks, 0.6F) * (0.75F + 0.25F * (float) Math.sin(y1 * 0.8F - time * 12F));

                    vert(ox + x0, oy + y0, oz + z0, tcol, baseAlpha * f0);
                    vert(ox + x1, oy + y0, oz + z1, tcol, baseAlpha * f0);
                    vert(ox + x1, oy + y1, oz + z1, tcol, baseAlpha * f1);
                    vert(ox + x0, oy + y1, oz + z0, tcol, baseAlpha * f1);
                }
            }
        }

        glow(ox, oy + 0.3F, oz, R * 3F, 1F, 0.5F);
    }

    private static void rings(PowerClip c, float ox, float oy, float oz, float time)
    {
        float R = c.radius.get() * c.scale.get();
        int n = Math.max(1, Math.min(8, c.count.get() / 8 + 1));

        for (int k = 0; k < n; k++)
        {
            float q = fract(time * 0.5F + (float) k / n);
            float r = q * R;
            float width = (0.12F + 0.25F * (1F - q)) * c.scale.get() * (1F + R * 0.05F);
            float a = (float) Math.pow(1F - q, 1.5F);

            annulus(ox, oy + 0.03F, oz, Math.max(0F, r - width), r, 0F, a * 0.9F, 0F);
            annulus(ox, oy + 0.03F, oz, r, r + width * 1.6F, a * 0.9F, 0F, 1F);
        }

        groundDisc(ox, oy + 0.03F, oz, R * 0.35F, 0.5F * (float) Math.max(0F, 1F - fract(time * 0.5F)));
    }

    private static void charge(PowerClip c, float ox, float oy, float oz, float time)
    {
        int n = c.count.get();
        float R = c.radius.get() * c.scale.get();
        float cy = oy + c.height.get() * c.scale.get() * 0.55F;
        int seed = c.seed.get();
        float sum = 0F;

        for (int i = 0; i < n; i++)
        {
            float ph = fract(time * 0.45F + hash(i * 1.7F + seed));
            float rad = (1F - ph) * R;
            float ang = hash(i * 3.3F + seed) * TAU + ph * 7F;
            float yy = cy + (hash(i * 7.7F + seed) - 0.5F) * R * 0.9F * (1F - ph);
            float size = c.scale.get() * (0.05F + 0.08F * ph);

            glow(ox + (float) Math.cos(ang) * rad, yy, oz + (float) Math.sin(ang) * rad, size, 1F, 0.9F * (0.3F + ph));
            sum += ph;
        }

        float core = c.scale.get() * (0.12F + 0.5F * (sum / n)) * (1F + 0.1F * (float) Math.sin(time * 14F));

        glow(ox, cy, oz, core, 1F, 0.8F);
    }

    private static void burst(PowerClip c, float ox, float oy, float oz, float time, float progress)
    {
        int n = c.count.get();
        float L = c.scale.get() * 2.5F;
        float cy = oy + c.height.get() * 0.5F;
        int seed = c.seed.get();
        float q = fract(time * 0.5F);

        for (int i = 0; i < n; i++)
        {
            float u = hash(i * 1.3F + seed) * 2F - 1F;
            float a = hash(i * 4.1F + seed) * TAU;
            float s = (float) Math.sqrt(1F - u * u);
            float dx = s * (float) Math.cos(a), dy = u, dz = s * (float) Math.sin(a);
            float len = L * (0.5F + hash(i * 6.7F + seed)) * (1F - (1F - q) * (1F - q));
            float w = c.scale.get() * 0.06F * (1F - q * 0.6F);
            float px = ox, py = cy, pz = oz;

            /* side vector perpendicular to the ray and the view direction */
            float vx = -(px + dx * len * 0.5F), vy = -(py + dy * len * 0.5F), vz = -(pz + dz * len * 0.5F);
            float sx = dy * vz - dz * vy, sy = dz * vx - dx * vz, sz = dx * vy - dy * vx;
            float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);

            if (sl < 1e-5F)
            {
                continue;
            }

            sx = sx / sl * w; sy = sy / sl * w; sz = sz / sl * w;

            float a0 = (1F - q) * 1F;

            vert(px - sx, py - sy, pz - sz, 0F, a0);
            vert(px + sx, py + sy, pz + sz, 0F, a0);
            vert(px + dx * len, py + dy * len, pz + dz * len, 1F, 0F);
            vert(px + dx * len, py + dy * len, pz + dz * len, 1F, 0F);
        }

        glow(ox, cy, oz, L * 0.35F * (1F - q * 0.5F), 1F, 0.9F * (1F - q * 0.6F));
    }

    private static void ball(PowerClip c, float ox, float oy, float oz, float time)
    {
        float R = c.radius.get() * c.scale.get() * (1F + 0.08F * (float) Math.sin(time * 9F));
        float cy = oy + c.height.get() * 0.5F;
        int lon = 20, lat = 12;

        for (int i = 0; i < lat; i++)
        {
            float p0 = (float) Math.PI * i / lat - (float) Math.PI / 2F;
            float p1 = (float) Math.PI * (i + 1) / lat - (float) Math.PI / 2F;

            for (int j = 0; j < lon; j++)
            {
                float t0 = j * TAU / lon + time * 0.6F, t1 = (j + 1) * TAU / lon + time * 0.6F;

                sphereVert(ox, cy, oz, R, p0, t0);
                sphereVert(ox, cy, oz, R, p0, t1);
                sphereVert(ox, cy, oz, R, p1, t1);
                sphereVert(ox, cy, oz, R, p1, t0);
            }
        }

        glow(ox, cy, oz, R * 1.6F, 1F, 0.45F);
    }

    private static void sphereVert(float ox, float oy, float oz, float R, float phi, float theta)
    {
        float nx = (float) (Math.cos(phi) * Math.cos(theta));
        float ny = (float) Math.sin(phi);
        float nz = (float) (Math.cos(phi) * Math.sin(theta));
        float px = ox + nx * R, py = oy + ny * R, pz = oz + nz * R;
        float vl = (float) Math.sqrt(px * px + py * py + pz * pz);
        float facing = Math.abs((nx * -px + ny * -py + nz * -pz) / Math.max(vl, 1e-4F));
        float rim = (float) Math.pow(1F - facing, 1.6F);

        vert(px, py, pz, rim, 0.18F + 0.7F * rim);
    }
}
