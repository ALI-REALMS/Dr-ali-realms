package com.alirealms.impactfx;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.core.ValueTransform;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.pose.Transform;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

/** 3D space rift / dimensional tear / portal in front of a replay. */
public class RiftClip extends CameraClip
{
    /** 0 lens tear, 1 round portal, 2 three rifts, 3 lightning strike, 4 ground cracks, 5 wind vortex, 6 light slit, 7 energy burst, 8 electric sweep, 9 plasma orb, 10 spark shower */
    public final ValueInt type = new ValueInt("type", 0, 0, 10);
    public final ValueInt actor = new ValueInt("actor", 1, 1, 64);
    public final ValueFloat offX = new ValueFloat("offX", 0F, -50F, 50F);
    public final ValueFloat offY = new ValueFloat("offY", 1.3F, -50F, 50F);
    public final ValueFloat offZ = new ValueFloat("offZ", 0F, -50F, 50F);
    /** distance in front of the replay */
    public final ValueFloat dist = new ValueFloat("dist", 3F, -50F, 50F);
    public final ValueFloat yaw = new ValueFloat("yaw", 0F, -360F, 360F);
    /** roll of the rift in its plane: 0 = vertical, 90 = horizontal */
    public final ValueFloat tilt = new ValueFloat("tilt", 25F, -180F, 180F);
    public final ValueFloat length = new ValueFloat("length", 3.5F, 0.2F, 60F);
    public final ValueFloat width = new ValueFloat("width", 0.7F, 0.05F, 20F);
    public final ValueFloat jagged = new ValueFloat("jagged", 0.5F, 0F, 2F);
    public final ValueInt sparks = new ValueInt("sparks", 40, 0, 200);
    public final ValueFloat pulse = new ValueFloat("pulse", 1F, 0F, 6F);
    public final ValueFloat intensity = new ValueFloat("intensity", 1F, 0F, 3F);
    public final ValueInt seed = new ValueInt("seed", 11, 0, 9999);
    /** position / rotation / size of the effect, edited like a character's transform */
    public final ValueTransform xform = new ValueTransform("xform", new Transform());
    public final ValueInt core = new ValueInt("core", 0xFFFFFF);
    public final ValueInt edge = new ValueInt("edge", 0x9A2BFF);
    public final ValueInt voidColor = new ValueInt("voidColor", 0x120022);

    public RiftClip()
    {
        super();

        this.add(this.type);
        this.add(this.actor);
        this.add(this.offX);
        this.add(this.offY);
        this.add(this.offZ);
        this.add(this.dist);
        this.add(this.yaw);
        this.add(this.tilt);
        this.add(this.length);
        this.add(this.width);
        this.add(this.jagged);
        this.add(this.sparks);
        this.add(this.pulse);
        this.add(this.intensity);
        this.add(this.seed);
        this.add(this.xform);
        this.add(this.core);
        this.add(this.edge);
        this.add(this.voidColor);

        this.title.set(ImpactAddon.RIFT_LABEL);
        this.duration.set(60);
        RiftPresets.apply(this, 0);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {}

    @Override
    protected Clip create()
    {
        return new RiftClip();
    }
}
