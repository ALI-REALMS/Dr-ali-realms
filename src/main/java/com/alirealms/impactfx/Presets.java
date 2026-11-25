package com.alirealms.impactfx;

public final class Presets
{
    public static final String[] NAMES = {
        "Charcoal Slam", "Color Swap (aura)", "Manga Speed Lines", "Spider-Verse Pop", "White Flash Hit", "Negative Shock"
    };

    private Presets() {}

    /** Resets the visual values of the clip to the given preset (timing is untouched). */
    public static void apply(ImpactClip c, int i)
    {
        /* defaults */
        c.mode.set(0); c.bg.set(0xFFFFFF); c.ink.set(0x000000); c.accent.set(0x000000); c.starColor.set(0x000000);
        c.threshold.set(0.5F); c.worldMix.set(0F); c.distort.set(0.6F); c.grain.set(0.25F); c.posterize.set(0);
        c.aura.set(0F); c.auraWidth.set(8F);
        c.starCount.set(0); c.starSize.set(0.03F); c.starRate.set(6F);
        c.speedLines.set(0F); c.shockwave.set(0F); c.inkBurst.set(0F); c.flash.set(0F);
        c.centerX.set(0.5F); c.centerY.set(0.5F);

        switch (i)
        {
            case 0: /* Charcoal Slam */
                c.mode.set(0); c.distort.set(1.2F); c.grain.set(0.45F);
                c.starCount.set(14); c.starSize.set(0.035F);
                c.inkBurst.set(0.5F); c.speedLines.set(0.25F);
                break;
            case 1: /* Color Swap aura */
                c.mode.set(3); c.bg.set(0x101018); c.ink.set(0xFFFFFF); c.accent.set(0xFF2D6F);
                c.aura.set(1.4F); c.auraWidth.set(14F); c.distort.set(0.9F);
                c.starCount.set(10); c.starColor.set(0xFFE14D);
                break;
            case 2: /* Manga speed lines */
                c.mode.set(1); c.threshold.set(0.45F); c.accent.set(0x000000);
                c.speedLines.set(0.8F); c.distort.set(0.5F);
                c.starCount.set(6);
                break;
            case 3: /* Spider-Verse pop */
                c.mode.set(4); c.bg.set(0xFFF4C2); c.ink.set(0x101020); c.accent.set(0xFF2D8A);
                c.worldMix.set(0.25F); c.posterize.set(4);
                c.starCount.set(16); c.starColor.set(0x19D3F3); c.starSize.set(0.04F);
                c.shockwave.set(0.6F); c.distort.set(0.7F);
                break;
            case 4: /* White flash hit */
                c.mode.set(0); c.distort.set(0.3F); c.grain.set(0.1F);
                c.flash.set(1F); c.starCount.set(8); c.shockwave.set(0.4F);
                break;
            case 5: /* Negative shock */
                c.mode.set(2); c.bg.set(0xFFFFFF); c.ink.set(0x000000);
                c.accent.set(0xFF0000); c.shockwave.set(0.8F); c.distort.set(0.8F);
                c.starCount.set(10); c.starColor.set(0xFFFFFF);
                break;
            default:
                break;
        }
    }
}
