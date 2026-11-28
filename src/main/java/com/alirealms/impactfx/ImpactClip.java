package com.alirealms.impactfx;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

/** Impact-frame VFX clip. Rendering happens client-side in ImpactRenderer. */
public class ImpactClip extends CameraClip
{
    /** 0 silhouette, 1 threshold (manga), 2 negative, 3 aura, 4 halftone pop */
    public final ValueInt mode = new ValueInt("mode", 0, 0, 4);
    public final ValueInt bg = new ValueInt("bg", 0xFFFFFF);
    public final ValueInt ink = new ValueInt("ink", 0x000000);
    /** character colour: 0 fixed, 1 auto contrast, 2 invert backdrop, 3 real character inverted */
    public final ValueFloat charMode = new ValueFloat("charMode", 3F, 0F, 3F);
    public final ValueFloat charShade = new ValueFloat("charShade", 0.85F, 0F, 1F);
    public final ValueInt inkAlt = new ValueInt("inkAlt", 0xFFFFFF);
    public final ValueInt accent = new ValueInt("accent", 0x000000);
    public final ValueInt starColor = new ValueInt("starColor", 0x000000);

    public final ValueFloat threshold = new ValueFloat("threshold", 0.5F, 0F, 1F);
    public final ValueFloat worldMix = new ValueFloat("worldMix", 0F, 0F, 1F);
    public final ValueFloat distort = new ValueFloat("distort", 0.6F, 0F, 3F);
    public final ValueFloat grain = new ValueFloat("grain", 0.25F, 0F, 1F);
    public final ValueInt posterize = new ValueInt("posterize", 0, 0, 16);

    public final ValueFloat actorRadius = new ValueFloat("actorRadius", 0.55F, 0.1F, 3F);
    public final ValueFloat actorHeight = new ValueFloat("actorHeight", 1.9F, 0.2F, 6F);
    public final ValueFloat aura = new ValueFloat("aura", 0F, 0F, 2F);
    public final ValueFloat auraWidth = new ValueFloat("auraWidth", 8F, 0.5F, 60F);

    public final ValueInt starCount = new ValueInt("starCount", 12, 0, 40);
    public final ValueFloat starSize = new ValueFloat("starSize", 0.03F, 0.005F, 0.3F);
    public final ValueInt starSeed = new ValueInt("starSeed", 7, 0, 9999);
    public final ValueFloat starRate = new ValueFloat("starRate", 6F, 0F, 40F);

    public final ValueFloat speedLines = new ValueFloat("speedLines", 0F, 0F, 1F);
    public final ValueFloat shockwave = new ValueFloat("shockwave", 0F, 0F, 1F);
    public final ValueFloat inkBurst = new ValueFloat("inkBurst", 0F, 0F, 1F);
    public final ValueFloat flash = new ValueFloat("flash", 0F, 0F, 1F);
    public final ValueFloat centerX = new ValueFloat("centerX", 0.5F, 0F, 1F);
    public final ValueFloat centerY = new ValueFloat("centerY", 0.5F, 0F, 1F);

    /** overlay shape: 0 none, 1 starburst, 2 lightning burst, 3 light rain, 4 speed lines + splats, 5 radial ink, 6 diagonal ink, 7 ink eye */
    public final ValueInt shape = new ValueInt("shape", 0, 0, 7);
    public final ValueFloat shapeScale = new ValueFloat("shapeScale", 1F, 0.2F, 3F);
    public final ValueFloat shapeSpeed = new ValueFloat("shapeSpeed", 1F, 0F, 5F);
    public final ValueFloat shapeDensity = new ValueFloat("shapeDensity", 1F, 0.2F, 3F);
    public final ValueFloat shapeGlow = new ValueFloat("shapeGlow", 1F, 0F, 3F);
    public final ValueFloat shapeRot = new ValueFloat("shapeRot", 0F, -180F, 180F);
    public final ValueFloat shapeFill = new ValueFloat("shapeFill", 1F, 0F, 1F);
    public final ValueInt shapeCol1 = new ValueInt("shapeCol1", 0xFFFFFF);
    public final ValueInt shapeCol2 = new ValueInt("shapeCol2", 0xFFFFFF);

    public ImpactClip()
    {
        super();

        this.add(this.mode);
        this.add(this.bg);
        this.add(this.ink);
        this.add(this.charMode);
        this.add(this.inkAlt);
        this.add(this.charShade);
        this.add(this.accent);
        this.add(this.starColor);
        this.add(this.threshold);
        this.add(this.worldMix);
        this.add(this.distort);
        this.add(this.grain);
        this.add(this.posterize);
        this.add(this.actorRadius);
        this.add(this.actorHeight);
        this.add(this.aura);
        this.add(this.auraWidth);
        this.add(this.starCount);
        this.add(this.starSize);
        this.add(this.starSeed);
        this.add(this.starRate);
        this.add(this.speedLines);
        this.add(this.shockwave);
        this.add(this.inkBurst);
        this.add(this.flash);
        this.add(this.centerX);
        this.add(this.centerY);
        this.add(this.shape);
        this.add(this.shapeScale);
        this.add(this.shapeSpeed);
        this.add(this.shapeDensity);
        this.add(this.shapeGlow);
        this.add(this.shapeRot);
        this.add(this.shapeFill);
        this.add(this.shapeCol1);
        this.add(this.shapeCol2);

        this.title.set(ImpactAddon.IMPACT_LABEL);
        this.duration.set(10);
        Presets.apply(this, 0);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        /* Nothing: this clip only affects the rendered image. */
    }

    @Override
    protected Clip create()
    {
        return new ImpactClip();
    }
}
