package com.alirealms.impactfx;

public final class SlashPresets
{
    public static final String[] NAMES = {
        "Blue crescent slash", "Red X slash", "Triple claw", "Swing trail (white)", "Spin slash (green)", "Flurry (gold)"
    };

    private SlashPresets() {}

    public static void apply(SlashClip c, int i)
    {
        c.offX.set(0F); c.offY.set(1.1F); c.offZ.set(0F); c.yaw.set(0F);
        c.tilt.set(45F); c.radius.set(2F); c.arc.set(140F); c.thickness.set(0.35F);
        c.tail.set(0.7F); c.count.set(5); c.intensity.set(1F);

        switch (i)
        {
            case 0:
                c.type.set(0); c.core.set(0xFFFFFF); c.edge.set(0x2E9BFF);
                break;
            case 1:
                c.type.set(1); c.core.set(0xFFE8E0); c.edge.set(0xFF1E2E); c.radius.set(2.2F); c.arc.set(120F);
                break;
            case 2:
                c.type.set(2); c.core.set(0xFFFFFF); c.edge.set(0xB026FF); c.radius.set(2.4F); c.arc.set(90F); c.thickness.set(0.25F);
                break;
            case 3:
                c.type.set(3); c.core.set(0xFFFFFF); c.edge.set(0xBFD8FF); c.radius.set(1.8F); c.arc.set(230F);
                c.thickness.set(0.08F); c.tail.set(0.6F); c.tilt.set(20F);
                break;
            case 4:
                c.type.set(4); c.core.set(0xE8FFE0); c.edge.set(0x22DD44); c.radius.set(2.2F); c.arc.set(360F);
                c.thickness.set(0.12F); c.tilt.set(0F); c.tail.set(0.8F); c.offY.set(1F);
                break;
            case 5:
                c.type.set(5); c.core.set(0xFFFFFF); c.edge.set(0xFFB300); c.count.set(7); c.radius.set(2.4F);
                break;
            default:
                break;
        }
    }
}
