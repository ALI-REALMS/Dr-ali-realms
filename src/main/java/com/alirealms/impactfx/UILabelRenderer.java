package com.alirealms.impactfx;

import mchorse.bbs_mod.ui.film.UIClips;
import mchorse.bbs_mod.ui.film.clips.renderer.UIClipRenderer;
import mchorse.bbs_mod.utils.clips.Clip;

/** Short fixed timeline label for our clips. */
public class UILabelRenderer<T extends Clip> extends UIClipRenderer<T>
{
    private final String label;

    public UILabelRenderer(String label)
    {
        this.label = label;
    }

    @Override
    public String getDefaultLabel(UIClips panel, T clip)
    {
        return this.label;
    }
}
