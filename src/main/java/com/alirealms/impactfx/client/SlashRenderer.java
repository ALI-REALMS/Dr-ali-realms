package com.alirealms.impactfx.client;

import com.alirealms.impactfx.SlashClip;
import mchorse.bbs_mod.api.client.events.FilmEvents;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import mchorse.bbs_mod.film.FilmMatrices;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import mchorse.bbs_mod.utils.pose.Transform;

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
        FilmEvents.RENDER_AFTER.register((controller, context) ->
        {
            if (!Compat.shadow())
            {
                render(controller, context);
            }
        });
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
        if (controller == null || controller.film == null || controller.film.camera == null)
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
        net.minecraft.client.render.Camera camera = net.minecraft.client.MinecraftClient.getInstance().gameRenderer.getCamera();
        Vec3d cam = camera.getCameraPos();
        List<Replay> replays = controller.film.replays.getList();
        boolean began = false;

        camera.getRotation().transform(1F, 0F, 0F, PowerRenderer.camRight);
        camera.getRotation().transform(0F, 1F, 0F, PowerRenderer.camUp);
        PowerRenderer.m = new Matrix4f().rotation(new org.joml.Quaternionf(camera.getRotation()).conjugate());

        Matrix4f base = PowerRenderer.m;

        for (SlashClip clip : clips)
        {
            PowerRenderer.m = base;

            int duration = Math.max(1, clip.duration.get());
            float rel = (tick - clip.tick.get()) + partial;
            boolean inside = clip.isInside(tick);

            /* the follow-hand trail keeps fading out after the clip ends instead of vanishing */
            boolean linger = !inside && clip.type.get() == 6 && rel > duration && rel < duration + 2F + clip.tail.get() * 14F;

            if (!clip.enabled.get() || (!inside && !linger))
            {
                continue;
            }

            float amount = linger ? 1F : clip.envelope.factorEnabled(duration, rel);

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
                follow(controller, clip, replays.get(index), entity, cam, partial, rel, !linger, x, y, z);

                continue;
            }

            /* attach the whole effect to a bone (any body part): explicit toggle, or simply by giving a bone name / number */
            boolean attached = clip.attach.get() == 1 || clip.boneIndex.get() > 0 || !clip.bone.get().trim().isEmpty();
            Transform xt = clip.xform.get();

            if (attached)
            {
                Matrix4f bm = boneMatrix(controller, clip, replays.get(index), entity, cam, partial, x, y, z);
                int axs = clip.axis.get();
                float dax = axs == 0 ? 1F : axs == 1 ? -1F : 0F, day = axs == 2 ? 1F : axs == 3 ? -1F : 0F, daz = axs == 4 ? 1F : axs == 5 ? -1F : 0F;

                bm.transformPosition(0F, 0F, 0F, TMP);

                float ox = TMP.x, oy = TMP.y, oz = TMP.z;

                /* the transform's translation is expressed in the bone's own space, the offsets stay in world axes */
                bm.transformPosition(xt.translate.x, xt.translate.y, xt.translate.z, TMP);

                cx = TMP.x + clip.offX.get();
                cy = TMP.y + clip.offY.get() - 1.1F;
                cz = TMP.z + clip.offZ.get();

                if (clip.followRot.get() == 1)
                {
                    bm.transformPosition(dax, day, daz, TMP);

                    float hx = TMP.x - ox, hz = TMP.z - oz;
                    float hl = (float) Math.sqrt(hx * hx + hz * hz);

                    if (hl > 0.2F)
                    {
                        fx = hx / hl; fz = hz / hl;
                        rx = fz; rz = -fx;
                    }
                }
            }
            else
            {
                float[] lo = FxXform.local(xt, fx, fz, rx, rz);

                cx += lo[0]; cy += lo[1]; cz += lo[2];
            }

            PowerRenderer.m = FxXform.around(base, xt, cx, cy, cz);

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
        int bi = clip.boneIndex.get();

        if (bi > 0)
        {
            List<String> all = FormUtilsClient.getBones(form);

            if (all != null && !all.isEmpty())
            {
                return all.get((bi - 1) % all.size());
            }
        }

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

    /** camera-relative matrix of the chosen bone; falls back to the actor's chest so an effect never silently disappears */
    private static Matrix4f boneMatrix(BaseFilmController controller, SlashClip clip, Replay replay, IEntity entity, Vec3d cam, float partial,
                                       double ex, double ey, double ez)
    {
        Form form = entity.getForm();
        Matrix4f bm = null;

        if (form != null)
        {
            String bone = pickBone(clip, form);

            if (bone != null)
            {
                bm = FilmMatrices.getBoneCompositeMatrix(controller.getEntities(), entity, replay, cam.x, cam.y, cam.z, partial, bone, false);
            }
        }

        if (bm == null)
        {
            bm = new Matrix4f().translation((float) (ex - cam.x), (float) (ey - cam.y) + 1.1F, (float) (ez - cam.z));
        }

        return bm;
    }

    /** Weapon trail that follows a bone of the replay (hand / sword / any part), built from the bone history. */
    private static void follow(BaseFilmController controller, SlashClip clip, Replay replay, IEntity entity, Vec3d cam, float partial, float rel,
                               boolean live, double ex, double ey, double ez)
    {
        float reach = clip.reach.get();
        int axis = clip.axis.get();
        float ax = axis == 0 ? 1F : axis == 1 ? -1F : 0F;
        float ay = axis == 2 ? 1F : axis == 3 ? -1F : 0F;
        float az = axis == 4 ? 1F : axis == 5 ? -1F : 0F;

        ArrayDeque<double[]> hist = HISTORY.computeIfAbsent(clip, (k) -> new ArrayDeque<>());
        double[] last = hist.peekLast();

        if (last != null && (rel < last[0] - 0.01F || (live && rel - last[0] > 4F)))
        {
            hist.clear();
            last = null;
        }

        double tx = 0, ty = 0, tz = 0;
        float brx = 0, bry = 0, brz = 0, trx = 0, try_ = 0, trz = 0;

        if (live)
        {
            Matrix4f bm = FxXform.child(boneMatrix(controller, clip, replay, entity, cam, partial, ex, ey, ez), clip.xform.get());

            bm.transformPosition(ax * reach * 0.3F, ay * reach * 0.3F, az * reach * 0.3F, TMP);

            brx = TMP.x; bry = TMP.y; brz = TMP.z;

            double bx = TMP.x + cam.x, by = TMP.y + cam.y, bz = TMP.z + cam.z;

            bm.transformPosition(ax * reach, ay * reach, az * reach, TMP);

            trx = TMP.x; try_ = TMP.y; trz = TMP.z;
            tx = TMP.x + cam.x; ty = TMP.y + cam.y; tz = TMP.z + cam.z;

            if (last == null || rel > last[0] + 0.001F)
            {
                hist.addLast(new double[] {rel, bx, by, bz, tx, ty, tz});
            }
        }

        float life = 2F + clip.tail.get() * 14F;

        while (hist.size() > 400 || (!hist.isEmpty() && rel - hist.peekFirst()[0] > life))
        {
            hist.pollFirst();
        }

        if (hist.size() < 2 && !live)
        {
            return;
        }

        Gfx.begin();

        /* the blade itself, visible even when the hand is not moving */
        if (live)
        {
            RiftRenderer.ribbon(brx, bry, brz, trx, try_, trz, reach * 0.035F, reach * 0.02F, 0.6F, 0.35F, 0.6F);
            RiftRenderer.ribbon(brx, bry, brz, trx, try_, trz, reach * 0.012F, reach * 0.008F, 0F, 0.9F, 1F);
            RiftRenderer.ribbonHot(brx, bry, brz, trx, try_, trz, reach * 0.006F, 1F);
        }

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

        if (live)
        {
            PowerRenderer.glow((float) (tx - cam.x), (float) (ty - cam.y), (float) (tz - cam.z), reach * 0.12F, 0.5F, 0.9F);
        }

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
