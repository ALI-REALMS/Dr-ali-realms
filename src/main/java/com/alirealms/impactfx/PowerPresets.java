package com.alirealms.impactfx;

public final class PowerPresets
{
    public static final String[] NAMES = {
        "Golden aura", "Blue energy pillar", "Red shockwave", "Charge-up (white)", "Energy burst (purple)", "Dark orb"
    };

    private PowerPresets() {}

    public static void apply(PowerClip c, int i)
    {
        c.offX.set(0F); c.offY.set(0F); c.offZ.set(0F);
        c.scale.set(1F); c.speed.set(1F); c.intensity.set(1F);
        c.radius.set(0.8F); c.height.set(2F); c.count.set(24);

        switch (i)
        {
            case 0:
                c.type.set(0); c.core.set(0xFFF2A0); c.edge.set(0xFF9A00); c.count.set(30); c.height.set(2.4F);
                break;
            case 1:
                c.type.set(1); c.core.set(0xE6FBFF); c.edge.set(0x1E7BFF); c.radius.set(0.7F); c.height.set(30F);
                break;
            case 2:
                c.type.set(2); c.core.set(0xFFE0D0); c.edge.set(0xFF1E1E); c.radius.set(6F); c.speed.set(1.2F);
                break;
            case 3:
                c.type.set(3); c.core.set(0xFFFFFF); c.edge.set(0x8FD0FF); c.radius.set(2.5F); c.count.set(40); c.height.set(1.6F);
                break;
            case 4:
                c.type.set(4); c.core.set(0xFFFFFF); c.edge.set(0xB000FF); c.scale.set(1.6F); c.count.set(36); c.height.set(1.2F);
                break;
            case 5:
                c.type.set(5); c.core.set(0xD9A0FF); c.edge.set(0x2A0050); c.radius.set(0.9F); c.height.set(1.8F);
                break;
            default:
                break;
        }
    }
}
