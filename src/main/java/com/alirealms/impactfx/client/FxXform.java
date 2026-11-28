package com.alirealms.impactfx.client;

import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Per-clip Transform (position / rotation / size) edited with the same panel as a character's transform.
 * Rotation and scale are applied around the effect's anchor by pre-multiplying the position matrix, so every
 * vertex of the effect follows without each renderer knowing about it.
 */
final class FxXform
{
    private FxXform() {}

    /** translation in the actor's frame: x = right, y = up, z = forward -> world offset {dx, dy, dz} */
    static float[] local(Transform t, float fx, float fz, float rx, float rz)
    {
        if (t == null)
        {
            return new float[] {0F, 0F, 0F};
        }

        return new float[] {rx * t.translate.x + fx * t.translate.z, t.translate.y, rz * t.translate.x + fz * t.translate.z};
    }

    private static float sc(float v)
    {
        return Math.abs(v) < 1e-4F ? 1F : v;
    }

    /** base * T(c) * R * S * T(-c); returns base itself when the transform has no rotation and no scale */
    static Matrix4f around(Matrix4f base, Transform t, float cx, float cy, float cz)
    {
        if (t == null)
        {
            return base;
        }

        Quaternionf q = t.createRotation();
        float sx = sc(t.scale.x), sy = sc(t.scale.y), sz = sc(t.scale.z);
        boolean rot = Math.abs(q.x) > 1e-5F || Math.abs(q.y) > 1e-5F || Math.abs(q.z) > 1e-5F;
        boolean scl = Math.abs(sx - 1F) > 1e-4F || Math.abs(sy - 1F) > 1e-4F || Math.abs(sz - 1F) > 1e-4F;

        if (!rot && !scl)
        {
            return base;
        }

        Matrix4f m = new Matrix4f(base).translate(cx, cy, cz);

        if (rot)
        {
            m.rotate(q);
        }

        m.scale(sx, sy, sz).translate(-cx, -cy, -cz);

        return m;
    }

    /** weapon frame: bone matrix * translate * rotate * scale */
    static Matrix4f child(Matrix4f bone, Transform t)
    {
        Matrix4f m = new Matrix4f(bone);

        if (t == null)
        {
            return m;
        }

        m.translate(t.translate.x, t.translate.y, t.translate.z).rotate(t.createRotation());
        m.scale(sc(t.scale.x), sc(t.scale.y), sc(t.scale.z));

        return m;
    }
}
