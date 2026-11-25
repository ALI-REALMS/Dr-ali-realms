package com.alirealms.impactfx;

public final class RiftPresets
{
    public static final String[] NAMES = {
        "Purple space rift", "Red hell crack", "Blue dimension tear", "Green toxic portal", "Golden cracks", "Black void"
    };

    private RiftPresets() {}

    public static void apply(RiftClip c, int i)
    {
        c.offX.set(0F); c.offY.set(1.3F); c.offZ.set(0F); c.dist.set(3F); c.yaw.set(0F);
        c.tilt.set(25F); c.length.set(3.5F); c.width.set(0.7F); c.jagged.set(0.5F);
        c.sparks.set(40); c.pulse.set(1F); c.intensity.set(1F);

        switch (i)
        {
            case 0:
                c.type.set(0); c.core.set(0xFFFFFF); c.edge.set(0x9A2BFF); c.voidColor.set(0x120022);
                break;
            case 1:
                c.type.set(0); c.core.set(0xFFE070); c.edge.set(0xFF2A00); c.voidColor.set(0x1A0000);
                c.tilt.set(-20F); c.length.set(4.5F); c.jagged.set(0.9F);
                break;
            case 2:
                c.type.set(0); c.core.set(0xE8FCFF); c.edge.set(0x1E90FF); c.voidColor.set(0x00081A);
                c.tilt.set(70F); c.length.set(4F);
                break;
            case 3:
                c.type.set(1); c.core.set(0xE8FFD0); c.edge.set(0x39FF14); c.voidColor.set(0x001A05);
                c.length.set(3F); c.width.set(1.5F); c.tilt.set(0F); c.jagged.set(0.25F);
                break;
            case 4:
                c.type.set(2); c.core.set(0xFFFFFF); c.edge.set(0xFFB300); c.voidColor.set(0x1A1000);
                c.length.set(3F); c.width.set(0.35F);
                break;
            case 5:
                c.type.set(0); c.core.set(0x8844AA); c.edge.set(0x3A0A5A); c.voidColor.set(0x000000);
                c.length.set(5F); c.width.set(1.1F); c.tilt.set(0F);
                break;
            default:
                break;
        }
    }
}
