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

import java.util.ArrayList;
import java.util.List;

public class UIRiftClip extends UIClip<RiftClip>
{
    private List<UIElement> presetButtons;

    public UITrackpad type, actor, offX, offY, offZ, dist, yaw, tilt, length, width, jagged, sparks, pulse, intensity, seed;
    public UIColor core, edge, voidColor;

    public UIRiftClip(RiftClip clip, IUIClipsDelegate editor)
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

        RiftClip c = this.clip;

        this.presetButtons = new ArrayList<>();

        for (int p = 0; p < RiftPresets.NAMES.length; p++)
        {
            final int index = p;

            this.presetButtons.add(new UIButton(IKey.raw(RiftPresets.NAMES[p]), (b) ->
            {
                RiftPresets.apply(this.clip, index);
                this.fillData();
            }));
        }

        this.type = this.i(c.type, "Type 0 Tear|1 Portal|2 3 rifts|3 Lightning|4 Ground cracks|5 Wind orb|6 Light slit|7 Energy burst", 0, 7);
        this.actor = this.i(c.actor, "Replay number (in front of it)", 1, 64);
        this.offX = this.f(c.offX, "Offset X", -50, 50);
        this.offY = this.f(c.offY, "Offset Y (height)", -50, 50);
        this.offZ = this.f(c.offZ, "Offset Z", -50, 50);
        this.dist = this.f(c.dist, "Distance in front", -50, 50);
        this.yaw = this.f(c.yaw, "Turn (degrees)", -360, 360);
        this.tilt = this.f(c.tilt, "Tilt 0 upright | 90 flat", -180, 180);
        this.length = this.f(c.length, "Length", 0.2, 60);
        this.width = this.f(c.width, "Width", 0.05, 20);
        this.jagged = this.f(c.jagged, "Jagged edge", 0, 2);
        this.sparks = this.i(c.sparks, "Sparks", 0, 200);
        this.pulse = this.f(c.pulse, "Pulse speed", 0, 6);
        this.intensity = this.f(c.intensity, "Brightness", 0, 3);
        this.seed = this.i(c.seed, "Seed", 0, 9999);
        this.core = this.color(c.core);
        this.edge = this.color(c.edge);
        this.voidColor = this.color(c.voidColor);
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(IKey.raw("Presets"), this.presetButtons.toArray(new UIElement[0])));
        this.panels.add(this.section(IKey.raw("Placement"), this.type, this.actor, this.dist, this.offX, this.offY, this.offZ, this.yaw, this.tilt));
        this.panels.add(this.section(IKey.raw("Core (glow) color"), this.core));
        this.panels.add(this.section(IKey.raw("Edge color"), this.edge));
        this.panels.add(this.section(IKey.raw("Inside (void) color"), this.voidColor));
        this.panels.add(this.section(IKey.raw("Shape"), this.length, this.width, this.jagged, this.sparks, this.pulse, this.intensity, this.seed));
    }
}
