package com.alirealms.impactfx;

public final class RiftPresets
{
    public static final String[] NAMES = {
        "Purple space rift", "Red hell crack", "Blue dimension tear", "Green toxic portal", "Golden cracks", "Black void",
        "Blue lightning strike", "Gold lightning + cracks", "Ground cracks (dark)", "Wind shred orb", "Orange light slit", "Magenta energy burst", "Blue plasma burst", "Fire burst",
        "Electric sweep (orange)", "Electric sweep (blue)", "Plasma orb", "Spark shower"
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
            case 6:
                c.type.set(3); c.core.set(0xFFFFFF); c.edge.set(0x2E7BFF); c.voidColor.set(0x000010);
                c.offY.set(0.02F); c.dist.set(4F); c.length.set(14F); c.width.set(1.6F); c.jagged.set(1F); c.sparks.set(30); c.pulse.set(1F);
                break;
            case 7:
                c.type.set(3); c.core.set(0xFFFFFF); c.edge.set(0xFFB020); c.voidColor.set(0x000000);
                c.offY.set(0.02F); c.dist.set(4F); c.length.set(14F); c.width.set(1.8F); c.jagged.set(1.2F); c.sparks.set(40);
                break;
            case 8:
                c.type.set(4); c.core.set(0xFFB070); c.edge.set(0xFF3010); c.voidColor.set(0x000000);
                c.offY.set(0.02F); c.dist.set(4F); c.length.set(6F); c.width.set(1.2F); c.sparks.set(14);
                break;
            case 9:
                c.type.set(5); c.core.set(0xFFFFFF); c.edge.set(0x8FE3F0); c.voidColor.set(0x000000);
                c.offY.set(1.2F); c.dist.set(3F); c.length.set(2F); c.width.set(1.3F); c.sparks.set(12); c.jagged.set(1F);
                break;
            case 10:
                c.type.set(6); c.core.set(0xFFFFFF); c.edge.set(0xFF8A00); c.voidColor.set(0xFFB040);
                c.offY.set(1.6F); c.dist.set(3F); c.tilt.set(0F); c.length.set(4.5F); c.width.set(0.22F); c.jagged.set(0.15F); c.sparks.set(30);
                break;
            case 11:
                c.type.set(7); c.core.set(0xFFFFFF); c.edge.set(0xFF2BD6); c.voidColor.set(0x2B3BFF);
                c.offY.set(0.05F); c.dist.set(3F); c.length.set(3F); c.width.set(1.1F); c.sparks.set(60);
                break;
            case 12:
                c.type.set(7); c.core.set(0xF2FFFF); c.edge.set(0x20C8FF); c.voidColor.set(0x1020FF);
                c.offY.set(0.05F); c.dist.set(3F); c.length.set(3.5F); c.width.set(1.2F); c.sparks.set(60);
                break;
            case 13:
                c.type.set(7); c.core.set(0xFFF2B0); c.edge.set(0xFF7A10); c.voidColor.set(0xC01800);
                c.offY.set(0.05F); c.dist.set(3F); c.length.set(3.5F); c.width.set(1.2F); c.sparks.set(70);
                break;
            case 14:
                c.type.set(8); c.core.set(0xFFFFFF); c.edge.set(0xFF9A3A); c.voidColor.set(0x000000);
                c.offY.set(1.4F); c.dist.set(3.5F); c.tilt.set(10F); c.length.set(4F); c.width.set(0.55F); c.jagged.set(1F); c.sparks.set(60);
                break;
            case 15:
                c.type.set(8); c.core.set(0xFFFFFF); c.edge.set(0x3DA5FF); c.voidColor.set(0x000000);
                c.offY.set(1.4F); c.dist.set(3.5F); c.tilt.set(-15F); c.length.set(4.5F); c.width.set(0.55F); c.jagged.set(1F); c.sparks.set(60);
                break;
            case 16:
                c.type.set(9); c.core.set(0xFFFFFF); c.edge.set(0xB04BFF); c.voidColor.set(0x000000);
                c.offY.set(1.4F); c.dist.set(2.5F); c.width.set(0.6F); c.sparks.set(40);
                break;
            case 17:
                c.type.set(10); c.core.set(0xFFF0C0); c.edge.set(0xFF8A20); c.voidColor.set(0x000000);
                c.offY.set(0.1F); c.dist.set(3F); c.length.set(3F); c.width.set(0.6F); c.sparks.set(50);
                break;
            default:
                break;
        }
    }
}
