package com.alirealms.impactfx;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

/** 3D sword slash / swing effect in front of a replay. */
public class SlashClip extends CameraClip
{
    public final ValueInt type = new ValueInt("type", 0, 0, 6);
    public final ValueInt actor = new ValueInt("actor", 1, 1, 64);
    public final ValueFloat offX = new ValueFloat("offX", 0F, -50F, 50F);
    public final ValueFloat offY = new ValueFloat("offY", 1.1F, -50F, 50F);
    public final ValueFloat offZ = new ValueFloat("offZ", 0F, -50F, 50F);
    /** extra rotation around the vertical axis (degrees) */
    public final ValueFloat yaw = new ValueFloat("yaw", 0F, -360F, 360F);
    /** tilt of the slash plane: 0 = horizontal, 90 = vertical, 45 = diagonal */
    public final ValueFloat tilt = new ValueFloat("tilt", 45F, -180F, 180F);
    public final ValueFloat radius = new ValueFloat("radius", 2F, 0.2F, 30F);
    public final ValueFloat arc = new ValueFloat("arc", 140F, 20F, 360F);
    public final ValueFloat thickness = new ValueFloat("thickness", 0.35F, 0.02F, 1F);
    public final ValueFloat tail = new ValueFloat("tail", 0.7F, 0.1F, 1F);
    public final ValueInt count = new ValueInt("count", 5, 1, 20);
    public final ValueFloat intensity = new ValueFloat("intensity", 1F, 0F, 3F);
    public final ValueInt seed = new ValueInt("seed", 5, 0, 9999);
    /** type 6: bone of the replay model to follow (empty = auto-detect the right hand/arm) */
    public final ValueString bone = new ValueString("bone", "");
    /** distance from the bone origin to the weapon tip */
    public final ValueFloat reach = new ValueFloat("reach", 1.2F, 0.1F, 10F);
    /** bone axis the weapon points along: 0 +X, 1 -X, 2 +Y, 3 -Y, 4 +Z, 5 -Z */
    public final ValueInt axis = new ValueInt("axis", 3, 0, 5);
    /** 0 = in front of the actor (offsets), 1 = attached to a bone (any body part) for all types */
    public final ValueInt attach = new ValueInt("attach", 0, 0, 1);
    /** 0 = use the bone name / auto, k = the k-th bone of the model (scrub it to pick a body part) */
    public final ValueInt boneIndex = new ValueInt("boneIndex", 0, 0, 200);
    /** when attached: 1 = the slash direction follows the bone's direction */
    public final ValueInt followRot = new ValueInt("followRot", 0, 0, 1);
    public final ValueInt core = new ValueInt("core", 0xFFFFFF);
    public final ValueInt edge = new ValueInt("edge", 0x2E9BFF);

    public SlashClip()
    {
        super();

        this.add(this.type);
        this.add(this.actor);
        this.add(this.offX);
        this.add(this.offY);
        this.add(this.offZ);
        this.add(this.yaw);
        this.add(this.tilt);
        this.add(this.radius);
        this.add(this.arc);
        this.add(this.thickness);
        this.add(this.tail);
        this.add(this.count);
        this.add(this.intensity);
        this.add(this.seed);
        this.add(this.bone);
        this.add(this.reach);
        this.add(this.axis);
        this.add(this.attach);
        this.add(this.boneIndex);
        this.add(this.followRot);
        this.add(this.core);
        this.add(this.edge);

        this.title.set(ImpactAddon.SLASH_LABEL);
        this.duration.set(10);
        SlashPresets.apply(this, 0);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {}

    @Override
    protected Clip create()
    {
        return new SlashClip();
    }
}
