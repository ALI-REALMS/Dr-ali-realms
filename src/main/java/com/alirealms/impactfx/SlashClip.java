package com.alirealms.impactfx;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

/** 3D sword slash / swing effect in front of a replay. */
public class SlashClip extends CameraClip
{
    public final ValueInt type = new ValueInt("type", 0, 0, 5);
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
