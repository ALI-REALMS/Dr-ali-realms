package com.alirealms.impactfx.client;

import com.alirealms.impactfx.SlashClip;
import mchorse.bbs_mod.api.client.events.FilmEvents;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import mchorse.bbs_mod.film.FilmMatrices;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** 3D slash / swing ribbons. Re-uses the primitives and per-frame state of PowerRenderer. */
public final class SlashRenderer
{
    private static final float PI = (float) Math.PI;

    /** per clip history of the weapon base/tip in ABSOLUTE world coordinates: {tick, bx,by,bz, tx,ty,tz} */
    private static final Map<SlashClip, ArrayDeque<double[]>> HISTORY = new WeakHashMap<>();
    private static final Vector3f TMP = new Vector3f();

    private SlashRenderer() {}

    public static void init()
    {
        FilmEvents.RENDER_AFTER.register(SlashRenderer::render);
    }

    private static float ease(float x)
    {
        x = Math.max(0F, Math.min(1F, x));

        return 1F - (1F - x) * (1F - x) * (1F - x);
    }

    private static float clamp01(float x)
    {
        return Math.max(0F, Math.min(1F, x));
    }

    private static void render(BaseFilmController controller, WorldRenderContext context)
    {
        if (controller == null || controller.film == null || controller.film.camera == null || context.matrixStack() == null)
        {
            return;
        }

        List<SlashClip> clips = controller.film.camera.getClips(SlashClip.class);

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

        for (SlashClip clip : clips)
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
            PowerRenderer.gain = clip.intensity.get() * amount;

            float yawRad = (float) Math.toRadians(ent + clip.yaw.get());
            float fx = -(float) Math.sin(yawRad), fz = (float) Math.cos(yawRad);   /* forward */
            float rx = fz, rz = -fx;                                               /* right (horizontal) */
            float cx = (float) (x - cam.x) + clip.offX.get() + fx * 0.0F;
            float cy = (float) (y - cam.y) + clip.offY.get();
            float cz = (float) (z - cam.z) + clip.offZ.get();
            float p = clamp01(rel / duration);
            float R = clip.radius.get();

            if (clip.type.get() == 6)
            {
                follow(controller, clip, replays.get(index), entity, cam, partial, rel);

                continue;
            }

            Gfx.begin();

            switch (clip.type.get())
            {
                case 0:
                    arc(cx, cy, cz, fx, fz, rx, rz, clip.tilt.get(), R, clip.arc.get(), clip.thickness.get(), clip.tail.get(), p, 1F, false);
                    break;
                case 1:
                    arc(cx, cy, cz, fx, fz, rx, rz, clip.tilt.get() + 45F, R, clip.arc.get(), clip.thickness.get(), clip.tail.get(), p, 1F, false);
                    arc(cx, cy, cz, fx, fz, rx, rz, clip.tilt.get() - 45F, R, clip.arc.get(), clip.thickness.get(), clip.tail.get(), clamp01((p - 0.2F) / 0.8F), 1F, true);
                    break;
                case 2:
                    for (int k = -1; k <= 1; k++)
                    {
                        float off = k * R * 0.28F;
                        float t = (float) Math.toRadians(clip.tilt.get());
                        float ax = (rx * (float) Math.cos(t)), az = (rz * (float) Math.cos(t));
                        float ay = (float) Math.sin(t);

                        arc(cx + ax * off, cy + ay * off, cz + az * off, fx, fz, rx, rz, clip.tilt.get(), R, clip.arc.get(), clip.thickness.get(), clip.tail.get(), clamp01((p - Math.abs(k) * 0.08F) / 0.9F), 1F, false);
                    }
                    break;
                case 3:
                    arc(cx, cy, cz, fx, fz, rx, rz, clip.tilt.get(), R, clip.arc.get(), clip.thickness.get(), clip.tail.get(), p, 1.4F, false);
                    break;
                case 4:
                    arc(cx, cy, cz, fx, fz, rx, rz, clip.tilt.get(), R, 360F, clip.thickness.get(), clip.tail.get(), p, 1F, false);
                    arc(cx, cy, cz, fx, fz, rx, rz, clip.tilt.get(), R * 0.8F, 360F, clip.thickness.get() * 0.7F, clip.tail.get(), clamp01((p - 0.1F) / 0.9F), 0.7F, true);
                    break;
                default:
                    int n = clip.count.get();
                    int seed = clip.seed.get();

                    for (int k = 0; k < n; k++)
                    {
                        float start = 0.75F * k / n;
                        float pk = clamp01((p - start) / 0.28F);

                        if (pk <= 0F || pk >= 1F && p - start > 0.5F)
                        {
                            continue;
                        }

                        float tl = (PowerRenderer.hash(k * 3.1F + seed) - 0.5F) * 160F;
                        float rr = R * (0.75F + 0.5F * PowerRenderer.hash(k * 5.7F + seed));
                        float jx = (PowerRenderer.hash(k * 7.3F + seed) - 0.5F) * R * 0.5F;
                        float jy = (PowerRenderer.hash(k * 9.9F + seed) - 0.5F) * R * 0.5F;

                        arc(cx + rx * jx, cy + jy, cz + rz * jx, fx, fz, rx, rz, tl, rr, 110F + 50F * PowerRenderer.hash(k * 1.9F + seed), clip.thickness.get(), clip.tail.get(), pk, 1F, (k & 1) == 1);
                    }
                    break;
            }

            Gfx.end();
        }

        if (began)
        {
            Gfx.cleanup();
        }
    }

    private static String pickBone(SlashClip clip, Form form)
    {
        String name = clip.bone.get();

        if (name != null && !name.trim().isEmpty())
        {
            return name.trim();
        }

        List<String> bones = FormUtilsClient.getBones(form);

        if (bones == null || bones.isEmpty())
        {
            return null;
        }

        String[] hints = {"hand_r", "right_hand", "righthand", "hand_right", "r_hand", "arm_r", "right_arm", "rightarm", "arm_right", "r_arm", "hand", "arm"};

        for (String hint : hints)
        {
            for (String b : bones)
            {
                if (b.toLowerCase().equals(hint))
                {
                    return b;
                }
            }
        }

        for (String hint : hints)
        {
            for (String b : bones)
            {
                if (b.toLowerCase().contains(hint))
                {
                    return b;
                }
            }
        }

        return null;
    }

    /** Weapon trail that follows a bone of the replay (hand / sword), built from the bone history. */
    private static void follow(BaseFilmController controller, SlashClip clip, Replay replay, IEntity entity, Vec3d cam, float partial, float rel)
    {
        Form form = entity.getForm();

        if (form == null)
        {
            return;
        }

        String bone = pickBone(clip, form);

        if (bone == null)
        {
            return;
        }

        Matrix4f bm = FilmMatrices.getBoneCompositeMatrix(controller.getEntities(), entity, replay, cam.x, cam.y, cam.z, partial, bone, false);

        if (bm == null)
        {
            return;
        }

        float reach = clip.reach.get();
        int axis = clip.axis.get();
        float ax = axis == 0 ? 1F : axis == 1 ? -1F : 0F;
        float ay = axis == 2 ? 1F : axis == 3 ? -1F : 0F;
        float az = axis == 4 ? 1F : axis == 5 ? -1F : 0F;

        ArrayDeque<double[]> hist = HISTORY.computeIfAbsent(clip, (k) -> new ArrayDeque<>());
        double[] last = hist.peekLast();

        if (last != null && (rel < last[0] - 0.01F || rel - last[0] > 4F))
        {
            hist.clear();
            last = null;
        }

        bm.transformPosition(ax * reach * 0.3F, ay * reach * 0.3F, az * reach * 0.3F, TMP);

        double bx = TMP.x + cam.x, by = TMP.y + cam.y, bz = TMP.z + cam.z;

        bm.transformPosition(ax * reach, ay * reach, az * reach, TMP);

        double tx = TMP.x + cam.x, ty = TMP.y + cam.y, tz = TMP.z + cam.z;

        if (last == null || rel > last[0] + 0.001F)
        {
            hist.addLast(new double[] {rel, bx, by, bz, tx, ty, tz});
        }

        float life = 2F + clip.tail.get() * 14F;

        while (hist.size() > 400 || (!hist.isEmpty() && rel - hist.peekFirst()[0] > life))
        {
            hist.pollFirst();
        }

        if (hist.size() < 2)
        {
            return;
        }

        Gfx.begin();

        double[] prev = null;

        for (double[] cur : hist)
        {
            if (prev != null)
            {
                float a0 = (float) Math.pow(clamp01(1F - (rel - (float) prev[0]) / life), 1.5);
                float a1 = (float) Math.pow(clamp01(1F - (rel - (float) cur[0]) / life), 1.5);

                PowerRenderer.vert((float) (prev[1] - cam.x), (float) (prev[2] - cam.y), (float) (prev[3] - cam.z), 1F, a0 * 0.2F);
                PowerRenderer.vert((float) (prev[4] - cam.x), (float) (prev[5] - cam.y), (float) (prev[6] - cam.z), 0F, a0);
                PowerRenderer.vert((float) (cur[4] - cam.x), (float) (cur[5] - cam.y), (float) (cur[6] - cam.z), 0F, a1);
                PowerRenderer.vert((float) (cur[1] - cam.x), (float) (cur[2] - cam.y), (float) (cur[3] - cam.z), 1F, a1 * 0.2F);
            }

            prev = cur;
        }

        PowerRenderer.glow((float) (tx - cam.x), (float) (ty - cam.y), (float) (tz - cam.z), reach * 0.12F, 0.5F, 0.9F);
        Gfx.end();
    }

    /**
     * One crescent ribbon. The arc lies in the plane spanned by the forward direction and an axis that is the
     * horizontal "right" vector rotated by tilt towards up. p = sweep progress 0..1.
     */
    private static void arc(float cx, float cy, float cz, float fx, float fz, float rx, float rz, float tiltDeg,
                            float R, float arcDeg, float thick, float tail, float p, float width, boolean flip)
    {
        if (p <= 0F)
        {
            return;
        }

        float t = (float) Math.toRadians(tiltDeg);
        float ax = rx * (float) Math.cos(t), ay = (float) Math.sin(t), az = rz * (float) Math.cos(t);
        float arcRad = (float) Math.toRadians(arcDeg);
        float head = ease(p) * (1F + tail);
        float fade = 1F - clamp01((p - 0.65F) / 0.35F);
        int segs = Math.max(12, (int) (arcDeg / 6F));

        for (int pass = 0; pass < 2; pass++)
        {
            float th = pass == 0 ? thick : thick * 0.3F;
            float a0 = pass == 0 ? 0.55F : 1F;
            float edgeT = pass == 0 ? 1F : 0.25F;

            for (int i = 0; i < segs; i++)
            {
                float u0 = (float) i / segs, u1 = (float) (i + 1) / segs;
                float v0 = vis(u0, head, tail) * fade * a0;
                float v1 = vis(u1, head, tail) * fade * a0;

                if (v0 <= 0.001F && v1 <= 0.001F)
                {
                    continue;
                }

                float[] o0 = pt(cx, cy, cz, fx, fz, ax, ay, az, R, arcRad, flip ? 1F - u0 : u0);
                float[] o1 = pt(cx, cy, cz, fx, fz, ax, ay, az, R, arcRad, flip ? 1F - u1 : u1);
                float[] i0 = pt(cx, cy, cz, fx, fz, ax, ay, az, R * (1F - th * (float) Math.sin(PI * u0)), arcRad, flip ? 1F - u0 : u0);
                float[] i1 = pt(cx, cy, cz, fx, fz, ax, ay, az, R * (1F - th * (float) Math.sin(PI * u1)), arcRad, flip ? 1F - u1 : u1);

                PowerRenderer.vert(i0[0], i0[1], i0[2], 0F, v0);
                PowerRenderer.vert(i1[0], i1[1], i1[2], 0F, v1);
                PowerRenderer.vert(o1[0], o1[1], o1[2], edgeT, v1 * 0.35F);
                PowerRenderer.vert(o0[0], o0[1], o0[2], edgeT, v0 * 0.35F);
            }
        }

        /* bright head */
        float hu = Math.min(1F, head);

        if (head > 0.02F && head < 1F + tail * 0.5F)
        {
            float[] h = pt(cx, cy, cz, fx, fz, ax, ay, az, R * (1F - thick * 0.5F * (float) Math.sin(PI * hu)), arcRad, flip ? 1F - hu : hu);

            PowerRenderer.glow(h[0], h[1], h[2], R * 0.12F * width, 0.5F, 0.9F * fade);
        }
    }

    private static float vis(float u, float head, float tail)
    {
        if (u > head)
        {
            return 0F;
        }

        return clamp01(1F - (head - u) / tail) * (float) Math.sin(PI * clamp01(u * 0.9F + 0.05F));
    }

    private static float[] pt(float cx, float cy, float cz, float fx, float fz, float ax, float ay, float az,
                              float R, float arcRad, float u)
    {
        float th = arcRad * (u - 0.5F);
        float cf = (float) Math.cos(th) * R, sa = (float) Math.sin(th) * R;

        return new float[] {cx + fx * cf + ax * sa, cy + ay * sa, cz + fz * cf + az * sa};
    }
}
