package com.alirealms.impactfx;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;

import java.util.ArrayList;
import java.util.List;

public class UIPowerClip extends UIClip<PowerClip>
{
    /* assigned in registerUI(), which the super constructor calls (no initializers on purpose) */
    private List<UIElement> presetButtons;

    public UITrackpad type, actor, offX, offY, offZ, radius, height, scale, count, speed, intensity, seed;
    public UIPropTransform xform;
    public UIColor core, edge;

    public UIPowerClip(PowerClip clip, IUIClipsDelegate editor)
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

        PowerClip c = this.clip;

        this.presetButtons = new ArrayList<>();

        for (int p = 0; p < PowerPresets.NAMES.length; p++)
        {
            final int index = p;

            this.presetButtons.add(new UIButton(IKey.raw(PowerPresets.NAMES[p]), (b) ->
            {
                PowerPresets.apply(this.clip, index);
                this.fillData();
            }));
        }

        this.type = this.i(c.type, "Type 0 Aura|1 Pillar|2 Rings|3 Charge|4 Burst|5 Ball", 0, 5);
        this.actor = this.i(c.actor, "Replay number (follows it)", 1, 64);
        this.offX = this.f(c.offX, "Offset X", -50, 50);
        this.offY = this.f(c.offY, "Offset Y", -50, 50);
        this.offZ = this.f(c.offZ, "Offset Z", -50, 50);
        this.radius = this.f(c.radius, "Radius", 0.05, 30);
        this.height = this.f(c.height, "Height", 0.1, 100);
        this.scale = this.f(c.scale, "Size", 0.05, 30);
        this.count = this.i(c.count, "Count", 1, 120);
        this.speed = this.f(c.speed, "Speed", 0, 10);
        this.intensity = this.f(c.intensity, "Brightness", 0, 3);
        this.seed = this.i(c.seed, "Seed", 0, 9999);
        this.xform = this.transform(c.xform);
        this.core = this.color(c.core);
        this.edge = this.color(c.edge);
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(IKey.raw("Presets"), this.presetButtons.toArray(new UIElement[0])));
        this.panels.add(this.section(IKey.raw("Effect"), this.type, this.actor, this.offX, this.offY, this.offZ));
        this.panels.add(this.section(IKey.raw("Transform: move / rotate / scale (like a character)"), this.xform));
        this.panels.add(this.section(IKey.raw("Core color"), this.core));
        this.panels.add(this.section(IKey.raw("Edge color"), this.edge));
        this.panels.add(this.section(IKey.raw("Shape"), this.radius, this.height, this.scale, this.count, this.speed, this.intensity, this.seed));
    }
}
