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
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;

import java.util.ArrayList;
import java.util.List;

public class UISlashClip extends UIClip<SlashClip>
{
    private List<UIElement> presetButtons;

    public UITextbox bone;
    public UITrackpad reach, axis, attach, boneIndex, followRot;
    public UITrackpad type, actor, offX, offY, offZ, yaw, tilt, radius, arc, thickness, tail, count, intensity, seed;
    public UIColor core, edge;

    public UISlashClip(SlashClip clip, IUIClipsDelegate editor)
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

        SlashClip c = this.clip;

        this.presetButtons = new ArrayList<>();

        for (int p = 0; p < SlashPresets.NAMES.length; p++)
        {
            final int index = p;

            this.presetButtons.add(new UIButton(IKey.raw(SlashPresets.NAMES[p]), (b) ->
            {
                SlashPresets.apply(this.clip, index);
                this.fillData();
            }));
        }

        this.type = this.i(c.type, "Type 0 Crescent|1 X|2 Claw|3 Swing|4 Spin|5 Flurry|6 Follow hand", 0, 6);
        this.actor = this.i(c.actor, "Replay number (follows it)", 1, 64);
        this.offX = this.f(c.offX, "Offset X", -50, 50);
        this.offY = this.f(c.offY, "Offset Y (height)", -50, 50);
        this.offZ = this.f(c.offZ, "Offset Z", -50, 50);
        this.yaw = this.f(c.yaw, "Turn (degrees)", -360, 360);
        this.tilt = this.f(c.tilt, "Tilt 0 flat | 90 vertical", -180, 180);
        this.radius = this.f(c.radius, "Radius", 0.2, 30);
        this.arc = this.f(c.arc, "Arc angle", 20, 360);
        this.thickness = this.f(c.thickness, "Thickness", 0.02, 1);
        this.tail = this.f(c.tail, "Trail length", 0.1, 1);
        this.count = this.i(c.count, "Count (flurry)", 1, 20);
        this.intensity = this.f(c.intensity, "Brightness", 0, 3);
        this.seed = this.i(c.seed, "Seed", 0, 9999);
        this.bone = this.textbox(100, c.bone);
        this.reach = this.f(c.reach, "Weapon reach", 0.1, 10);
        this.axis = this.i(c.axis, "Weapon axis 0 +X|1 -X|2 +Y|3 -Y|4 +Z|5 -Z", 0, 5);
        this.attach = this.i(c.attach, "Attach to bone 0 off | 1 on (all types)", 0, 1);
        this.boneIndex = this.i(c.boneIndex, "Bone number (0 = name/auto, scrub to pick a body part)", 0, 200);
        this.followRot = this.i(c.followRot, "Direction follows bone 0 no | 1 yes", 0, 1);
        this.core = this.color(c.core);
        this.edge = this.color(c.edge);
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(this.section(IKey.raw("Presets"), this.presetButtons.toArray(new UIElement[0])));
        this.panels.add(this.section(IKey.raw("Effect"), this.type, this.actor, this.offX, this.offY, this.offZ, this.yaw, this.tilt));
        this.panels.add(this.section(IKey.raw("Attach to a body part (offset Y 1.1 = exactly on the bone)"), this.attach, this.boneIndex, this.followRot));
        this.panels.add(this.section(IKey.raw("Bone name (empty = auto right hand) + weapon reach/axis"), this.bone, this.reach, this.axis));
        this.panels.add(this.section(IKey.raw("Core color"), this.core));
        this.panels.add(this.section(IKey.raw("Edge color"), this.edge));
        this.panels.add(this.section(IKey.raw("Shape"), this.radius, this.arc, this.thickness, this.tail, this.count, this.intensity, this.seed));
    }
}
