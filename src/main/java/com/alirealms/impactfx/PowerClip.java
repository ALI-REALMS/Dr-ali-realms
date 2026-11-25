package com.alirealms.impactfx;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

/** 3D energy / power effect that follows a replay (actor) of the film. */
public class PowerClip extends CameraClip
{
    public static final String[] TYPES = {"Aura", "Energy pillar", "Shockwave rings", "Charge-up orbs", "Energy burst", "Energy ball"};

    public final ValueInt type = new ValueInt("type", 0, 0, 5);
    /** 1-based number of the replay in the film (first replay = 1). */
    public final ValueInt actor = new ValueInt("actor", 1, 1, 64);
    public final ValueFloat offX = new ValueFloat("offX", 0F, -50F, 50F);
    public final ValueFloat offY = new ValueFloat("offY", 0F, -50F, 50F);
    public final ValueFloat offZ = new ValueFloat("offZ", 0F, -50F, 50F);
    public final ValueFloat radius = new ValueFloat("radius", 0.8F, 0.05F, 30F);
    public final ValueFloat height = new ValueFloat("height", 2F, 0.1F, 100F);
    public final ValueFloat scale = new ValueFloat("scale", 1F, 0.05F, 30F);
    public final ValueInt count = new ValueInt("count", 24, 1, 120);
    public final ValueFloat speed = new ValueFloat("speed", 1F, 0F, 10F);
    public final ValueFloat intensity = new ValueFloat("intensity", 1F, 0F, 3F);
    public final ValueInt seed = new ValueInt("seed", 3, 0, 9999);
    public final ValueInt core = new ValueInt("core", 0xFFF2A0);
    public final ValueInt edge = new ValueInt("edge", 0xFF8A00);

    public PowerClip()
    {
        super();

        this.add(this.type);
        this.add(this.actor);
        this.add(this.offX);
        this.add(this.offY);
        this.add(this.offZ);
        this.add(this.radius);
        this.add(this.height);
        this.add(this.scale);
        this.add(this.count);
        this.add(this.speed);
        this.add(this.intensity);
        this.add(this.seed);
        this.add(this.core);
        this.add(this.edge);

        this.title.set(ImpactAddon.POWER_LABEL);
        this.duration.set(40);
        PowerPresets.apply(this, 0);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {}

    @Override
    protected Clip create()
    {
        return new PowerClip();
    }
}
