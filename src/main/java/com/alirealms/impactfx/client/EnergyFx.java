package com.alirealms.impactfx.client;

import com.alirealms.impactfx.RiftClip;

/**
 * Blender-style building blocks: motion-streak sparks, forked electric arcs, swirling energy wisps,
 * thin ground rings. Everything emits vertices into the batch the caller has already opened
 * (additive), using PowerRenderer's current core/edge colours.
 */
final class EnergyFx
{
    private static final float TAU = 6.2831855F;

    private EnergyFx() {}

    private static float h(float x)
    {
        return PowerRenderer.hash(x);
    }

    private static float c01(float v)
    {
        return Math.max(0F, Math.min(1F, v));
    }

    /* ------------------------------------------------------------ sparks */

    /**
     * Sparks that fly out of a point with a motion-blur streak (hot head, fading tail), ease-out speed and gravity.
     * upBias 0 = sphere, 1 = upward cone.
     */
    static void streaks(float ox, float oy, float oz, int count, float speed, float gravity, float rate, float time, int seed,
                        float width, float vis, float upBias)
    {
        if (vis <= 0.01F || count <= 0)
        {
            return;
        }

        for (int k = 0; k < count; k++)
        {
            float t0 = time * rate * 0.5F + h(k * 1.3F + seed);
            float ph = t0 - (float) Math.floor(t0);
            float cyc = (float) Math.floor(t0);
            float rs = k * 7.7F + cyc * 13.1F + seed;
            float th = h(rs + 1F) * TAU;
            float cp = h(rs + 2F) * 2F - 1F;

            cp = cp + (Math.abs(cp) * 0.8F + 0.2F - cp) * upBias;

            float sn = (float) Math.sqrt(Math.max(0F, 1F - cp * cp));
            float dx = sn * (float) Math.cos(th), dy = cp, dz = sn * (float) Math.sin(th);
            float sp = speed * (0.35F + h(rs + 3F));
            float[] head = spark(ox, oy, oz, dx, dy, dz, sp, gravity, ph);
            float[] tail = spark(ox, oy, oz, dx, dy, dz, sp, gravity, Math.max(0F, ph - 0.06F));
            float fade = (float) Math.pow(1F - ph, 0.8F) * vis * (ph < 0.03F ? ph / 0.03F : 1F);
            float w = width * (0.6F + 0.8F * h(rs + 4F));

            RiftRenderer.ribbonG(tail[0], tail[1], tail[2], head[0], head[1], head[2], w * 0.15F, w, 1F, 0.1F, 0F, fade * 0.9F);
            RiftRenderer.ribbonHot(tail[0], tail[1], tail[2], head[0], head[1], head[2], w * 0.35F, fade * 0.8F);
        }
    }

    private static float[] spark(float ox, float oy, float oz, float dx, float dy, float dz, float sp, float g, float ph)
    {
        float e = 1F - (1F - ph) * (1F - ph);
        float d = sp * e;

        return new float[] {ox + dx * d, oy + dy * d - g * ph * ph * sp * 0.5F, oz + dz * d};
    }

    /* ------------------------------------------------------------ arcs */

    /** forked electric arc between two points (midpoint displacement, three layers + white-hot core) */
    static void arc(float x0, float y0, float z0, float x1, float y1, float z1, int seed, float width, float alpha, boolean fork)
    {
        if (alpha <= 0.01F)
        {
            return;
        }

        int segs = 16;
        float[][] pts = new float[segs + 1][3];
        float len = (float) Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0) + (z1 - z0) * (z1 - z0));

        /* multi-octave jitter */
        for (int i = 0; i <= segs; i++)
        {
            float f = (float) i / segs;
            float env = (float) Math.sin(Math.PI * f);
            float j = len * 0.16F * env;
            float o1 = h(i * 1.9F + seed) - 0.5F, o2 = h(i * 4.1F + seed + 7F) - 0.5F, o3 = h(i * 2.7F + seed + 13F) - 0.5F;

            pts[i][0] = x0 + (x1 - x0) * f + o1 * j * 2F;
            pts[i][1] = y0 + (y1 - y0) * f + o2 * j * 2F;
            pts[i][2] = z0 + (z1 - z0) * f + o3 * j * 2F;
        }

        for (int i = 0; i < segs; i++)
        {
            float[] a = pts[i], b = pts[i + 1];

            RiftRenderer.ribbon(a[0], a[1], a[2], b[0], b[1], b[2], width * 7F, width * 7F, 1F, alpha * 0.10F, alpha * 0.10F);
            RiftRenderer.ribbon(a[0], a[1], a[2], b[0], b[1], b[2], width * 2.8F, width * 2.8F, 0.6F, alpha * 0.32F, alpha * 0.32F);
            RiftRenderer.ribbon(a[0], a[1], a[2], b[0], b[1], b[2], width, width, 0F, alpha * 0.95F, alpha * 0.95F);
            RiftRenderer.ribbonHot(a[0], a[1], a[2], b[0], b[1], b[2], width * 0.45F, alpha);
        }

        if (fork)
        {
            int i = 4 + (int) (h(seed * 0.37F) * 7F);
            float[] a = pts[i];
            float fx = a[0] + (h(seed + 3F) - 0.5F) * len * 0.5F;
            float fy = a[1] + (h(seed + 5F) - 0.5F) * len * 0.5F;
            float fz = a[2] + (h(seed + 9F) - 0.5F) * len * 0.5F;

            arc(a[0], a[1], a[2], fx, fy, fz, seed + 101, width * 0.6F, alpha * 0.7F, false);
        }
    }

    /** short arcs jumping off the edge of a tear (flickers ~14 times per second) */
    static void arcsAlong(float[] sx, float[] sy, float[] sz, int n, float qx, float qy, float qz, float W, float time, int seed, float open)
    {
        if (open <= 0.05F)
        {
            return;
        }

        int step = (int) Math.floor(time * 14F);

        for (int k = 0; k < 3; k++)
        {
            float r = h(step * 3.7F + k * 5.3F + seed);

            if (r < 0.35F)
            {
                continue;
            }

            int i = 3 + (int) (h(step * 1.3F + k * 2.9F + seed) * (n - 6));
            float side = h(step * 9.1F + k + seed) < 0.5F ? -1F : 1F;
            float l = W * (0.7F + 1.6F * h(step * 2.2F + k * 7F + seed));
            float ex = sx[i] + qx * side * l + (h(step + k * 11F) - 0.5F) * W;
            float ey = sy[i] + qy * side * l + (h(step * 1.7F + k * 4F) - 0.5F) * W;
            float ez = sz[i] + qz * side * l + (h(step * 2.3F + k * 9F) - 0.5F) * W;

            arc(sx[i], sy[i], sz[i], ex, ey, ez, step * 31 + k * 7 + seed, Math.max(0.006F, W * 0.012F), open * 0.9F, true);
        }
    }

    /* ------------------------------------------------------------ wisps */

    /** swirling energy ribbon: spiral around a vertical axis, thin at both ends */
    static void wisp(float cx, float cy, float cz, float radius, float height, float turns, float phase, float spin,
                     float time, float widthMax, float alpha, float t0, float t1)
    {
        int n = 28;
        float px = 0, py = 0, pz = 0;

        for (int i = 0; i <= n; i++)
        {
            float s = (float) i / n;
            float ang = phase + s * turns * TAU + time * spin;
            float r = radius * (0.2F + 0.8F * (float) Math.pow(s, 0.7F)) * (1F + 0.12F * (float) Math.sin(s * 9F + time * 3F + phase));
            float x = cx + (float) Math.cos(ang) * r;
            float z = cz + (float) Math.sin(ang) * r;
            float y = cy + (s - 0.45F) * height + 0.12F * height * (float) Math.sin(s * 7F + phase + time * 2F);

            if (i > 0)
            {
                float env = (float) Math.pow(Math.sin(Math.PI * (s - 0.5F / n)), 0.8F);
                float w = widthMax * env;
                float a = alpha * env;

                RiftRenderer.ribbonG(px, py, pz, x, y, z, w, w, t0 + (t1 - t0) * (s - 1F / n), t0 + (t1 - t0) * s, a, a);
                RiftRenderer.ribbonG(px, py, pz, x, y, z, w * 0.35F, w * 0.35F, 0F, 0F, a * 0.8F, a * 0.8F);
            }

            px = x; py = y; pz = z;
        }
    }

    /* ------------------------------------------------------------ ground ring */

    /** thin glowing ring lying on the ground, feathered, with a white-hot inner line */
    static void ring(float cx, float gy, float cz, float radius, float width, float alpha, float time, int seed)
    {
        if (alpha <= 0.01F || radius <= 0.01F)
        {
            return;
        }

        int n = 72;
        float[] off = {-1F, -0.5F, 0F, 0.5F, 1F};
        float[] fa = {0F, 0.35F, 1F, 0.35F, 0F};

        for (int i = 0; i < n; i++)
        {
            float a0 = i * TAU / n, a1 = (i + 1) * TAU / n;
            float r0 = radius * (1F + 0.035F * (float) Math.sin(a0 * 5F + time * 4F) + 0.02F * (h(i * 1.3F + seed) - 0.5F));
            float r1 = radius * (1F + 0.035F * (float) Math.sin(a1 * 5F + time * 4F) + 0.02F * (h((i + 1) * 1.3F + seed) - 0.5F));
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);

            for (int k = 0; k < 4; k++)
            {
                PowerRenderer.vert(cx + c0 * (r0 + width * off[k]), gy, cz + s0 * (r0 + width * off[k]), 0.35F, alpha * fa[k]);
                PowerRenderer.vert(cx + c0 * (r0 + width * off[k + 1]), gy, cz + s0 * (r0 + width * off[k + 1]), 0.35F, alpha * fa[k + 1]);
                PowerRenderer.vert(cx + c1 * (r1 + width * off[k + 1]), gy, cz + s1 * (r1 + width * off[k + 1]), 0.35F, alpha * fa[k + 1]);
                PowerRenderer.vert(cx + c1 * (r1 + width * off[k]), gy, cz + s1 * (r1 + width * off[k]), 0.35F, alpha * fa[k]);
            }

            PowerRenderer.vertHot(cx + c0 * (r0 - width * 0.3F), gy, cz + s0 * (r0 - width * 0.3F), 0F);
            PowerRenderer.vertHot(cx + c0 * r0, gy, cz + s0 * r0, alpha);
            PowerRenderer.vertHot(cx + c1 * r1, gy, cz + s1 * r1, alpha);
            PowerRenderer.vertHot(cx + c1 * (r1 - width * 0.3F), gy, cz + s1 * (r1 - width * 0.3F), 0F);
        }
    }

    /* ------------------------------------------------------------ composite */

    /** type 7: energy burst (wisps + arcs + streak sparks + ground rings + core glow) */
    static void burst(RiftClip c, float x, float gy, float z, float cy, float p, float time, float L, float W, float jag, int sparks, int seed,
                      float vr, float vg, float vb)
    {
        float vis = c01(p / 0.06F) * (1F - c01((p - 0.72F) / 0.28F));

        if (vis <= 0.01F)
        {
            return;
        }

        float g = 1F - (float) Math.pow(1F - c01(p / 0.18F), 3F);
        float pulse = 0.85F + 0.15F * (float) Math.sin(time * 6.2832F * 1.7F);
        float step = (float) Math.floor(time * 16F);

        Gfx.blendAdd();
        Gfx.begin();

        /* ground light + core */
        RiftRenderer.glowDisc(x, gy, z, W * 2.6F * g, 0.5F * vis);
        PowerRenderer.glow(x, cy, z, W * 2.4F * g, 0.7F, 0.38F * vis * pulse);
        PowerRenderer.glow(x, cy, z, W * 1.0F * g, 0.3F, 0.75F * vis * pulse);
        PowerRenderer.glow(x, cy, z, W * 0.38F * g, 0F, 1F * vis);

        /* wisps: second colour = voidColor */
        float scr = PowerRenderer.cr, scg = PowerRenderer.cg, scb = PowerRenderer.cb;

        PowerRenderer.cr = PowerRenderer.er; PowerRenderer.cg = PowerRenderer.eg; PowerRenderer.cb = PowerRenderer.eb;
        PowerRenderer.er = vr; PowerRenderer.eg = vg; PowerRenderer.eb = vb;

        for (int k = 0; k < 8; k++)
        {
            float rs = k * 4.1F + seed;
            float dir = k % 2 == 0 ? 1F : -1F;

            wisp(x, cy, z, W * (0.8F + 0.9F * h(rs)) * g, L * (0.5F + 0.4F * h(rs + 1F)), 0.5F + 0.6F * h(rs + 2F), h(rs + 3F) * TAU,
                dir * (0.8F + h(rs + 4F)), time, W * (0.1F + 0.12F * h(rs + 5F)), 0.55F * vis, 0F, 1F);
        }

        /* restore colours */
        PowerRenderer.er = PowerRenderer.cr; PowerRenderer.eg = PowerRenderer.cg; PowerRenderer.eb = PowerRenderer.cb;
        PowerRenderer.cr = scr; PowerRenderer.cg = scg; PowerRenderer.cb = scb;

        /* tight bright wisps with the main colours */
        for (int k = 0; k < 4; k++)
        {
            float rs = k * 6.3F + seed + 50F;

            wisp(x, cy, z, W * (0.35F + 0.3F * h(rs)) * g, L * 0.45F, 0.9F + 0.5F * h(rs + 1F), h(rs + 3F) * TAU,
                (k % 2 == 0 ? 1F : -1F) * (1.4F + h(rs + 4F)), time, W * 0.06F, 0.6F * vis, 0F, 0.8F);
        }

        /* electric arcs, re-rolled 16x a second */
        for (int k = 0; k < 6; k++)
        {
            float rs = step * 5.3F + k * 2.9F + seed;

            if (h(rs) < 0.2F)
            {
                continue;
            }

            float th = h(rs + 1F) * TAU, cp = h(rs + 2F) * 1.4F - 0.5F;
            float sn = (float) Math.sqrt(Math.max(0F, 1F - cp * cp));
            float rr = W * (0.9F + 1.1F * h(rs + 3F)) * g;

            arc(x, cy, z, x + sn * (float) Math.cos(th) * rr, cy + cp * rr, z + sn * (float) Math.sin(th) * rr, (int) (rs * 10F), Math.max(0.006F, W * 0.012F),
                vis * (0.6F + 0.4F * h(rs + 4F)), true);
        }

        /* sparks */
        streaks(x, cy, z, sparks * 4, L * 0.9F, 0.7F, 1.3F, time, seed, W * 0.02F, vis, 0.25F);

        /* rings on the ground */
        ring(x, gy + 0.01F, z, W * 1.6F * g, W * 0.07F, 0.95F * vis, time, seed);

        for (int k = 0; k < 2; k++)
        {
            float ph = PowerRenderer.fract(time * 0.5F + k * 0.5F);

            ring(x, gy + 0.005F, z, W * (0.4F + 3.4F * ph) * g, W * 0.12F * (1F - 0.5F * ph), 0.5F * (1F - ph) * vis, time, seed + 9);
        }

        /* lens flare rays */
        RiftRenderer.flare(x, cy, z, 6, W * 2.2F * g * pulse, W * 0.02F, 0.45F * vis, time * 0.25F);

        Gfx.end();
    }
}
