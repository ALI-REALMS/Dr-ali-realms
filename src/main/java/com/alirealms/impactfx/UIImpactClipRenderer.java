package com.alirealms.impactfx;

import mchorse.bbs_mod.ui.film.UIClips;
import mchorse.bbs_mod.ui.film.clips.renderer.UIClipRenderer;

/** Short timeline label for the clip. */
public class UIImpactClipRenderer extends UIClipRenderer<ImpactClip>
{
    public static final String LABEL = "ALI impact vfx";

    @Override
    public String getDefaultLabel(UIClips panel, ImpactClip clip)
    {
        return LABEL;
    }
}
