package com.alirealms.impactfx;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;

import java.util.ArrayList;
import java.util.List;

public class UIImpactClip extends UIClip<ImpactClip>
{
    /* assigned in registerUI(), which the super constructor calls (no initializer on purpose) */
    private List<UIElement> presetButtons;

    public UITrackpad mode, threshold, worldMix, distort, grain, posterize;
    public UITrackpad actorRadius, actorHeight, aura, auraWidth;
    public UITrackpad starCount, starSize, starSeed, starRate;
    public UITrackpad speedLines, shockwave, inkBurst, flash, centerX, centerY;
    public UIColor bg, ink, accent, starColor;

    public UIImpactClip(ImpactClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    private UITrackpad f(ValueFloat v, String label, double min, double max)
    {
        return this.trackpad(v).limit(min, max).forcedLabel(IKey.raw(label));
    }

    private UITrackpad i(ValueInt v, String label, int min, int max)
    {
        return this.trackpad(v).limit(min, max).integer().forcedLabel(IKey.raw(label));
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        ImpactClip c = this.clip;
        this.presetButtons = new ArrayList<>();

        for (int p = 0; p < Presets.NAMES.length; p++)
        {
            final int index = p;
            UIButton button = new UIButton(IKey.raw(Presets.NAMES[p]), (b) ->
            {
                Presets.apply(this.clip, index);
                this.fillData();
            });

            this.presetButtons.add(button);
        }

        this.mode = this.i(c.mode, "Mode 0 Sil | 1 Manga | 2 Neg | 3 Aura | 4 Pop", 0, 4);
        this.threshold = this.f(c.threshold, "Threshold", 0, 1);
        this.worldMix = this.f(c.worldMix, "Show world", 0, 1);
        this.distort = this.f(c.distort, "Distortion", 0, 3);
        this.grain = this.f(c.grain, "Grain", 0, 1);
        this.posterize = this.i(c.posterize, "Posterize levels", 0, 16);

        this.actorRadius = this.f(c.actorRadius, "Actor radius", 0.1, 3);
        this.actorHeight = this.f(c.actorHeight, "Actor height", 0.2, 6);
        this.aura = this.f(c.aura, "Aura strength", 0, 2);
        this.auraWidth = this.f(c.auraWidth, "Aura width", 0.5, 60);

        this.starCount = this.i(c.starCount, "Stars count", 0, 40);
        this.starSize = this.f(c.starSize, "Stars size", 0.005, 0.3);
        this.starSeed = this.i(c.starSeed, "Stars seed", 0, 9999);
        this.starRate = this.f(c.starRate, "Stars reshuffle per second", 0, 40);

        this.speedLines = this.f(c.speedLines, "Speed lines", 0, 1);
        this.shockwave = this.f(c.shockwave, "Shockwave", 0, 1);
        this.inkBurst = this.f(c.inkBurst, "Ink burst", 0, 1);
        this.flash = this.f(c.flash, "Flash", 0, 1);
        this.centerX = this.f(c.centerX, "Center X", 0, 1);
        this.centerY = this.f(c.centerY, "Center Y", 0, 1);

        this.bg = this.color(c.bg);
        this.ink = this.color(c.ink);
        this.accent = this.color(c.accent);
        this.starColor = this.color(c.starColor);
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(IKey.raw("Presets"), this.presetButtons.toArray(new UIElement[0])));
        this.panels.add(this.section(IKey.raw("Look"), this.mode, this.threshold, this.worldMix, this.distort, this.grain, this.posterize));
        this.panels.add(this.section(IKey.raw("Background color"), this.bg));
        this.panels.add(this.section(IKey.raw("Character (ink) color"), this.ink));
        this.panels.add(this.section(IKey.raw("Accent color (lines, aura, shockwave)"), this.accent));
        this.panels.add(this.section(IKey.raw("Stars color"), this.starColor));
        this.panels.add(this.section(IKey.raw("Characters"), this.actorRadius, this.actorHeight, this.aura, this.auraWidth));
        this.panels.add(this.section(IKey.raw("Stars"), this.starCount, this.starSize, this.starSeed, this.starRate));
        this.panels.add(this.section(IKey.raw("Effects"), this.speedLines, this.shockwave, this.inkBurst, this.flash, this.centerX, this.centerY));
    }
}
