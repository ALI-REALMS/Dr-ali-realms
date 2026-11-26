package com.alirealms.impactfx;

import mchorse.bbs_mod.api.BBSAddonMod;
import mchorse.bbs_mod.api.Subscribe;
import mchorse.bbs_mod.api.client.events.RegisterClipPanelsEvent;
import mchorse.bbs_mod.api.client.events.RegisterClipRenderersEvent;
import mchorse.bbs_mod.api.client.events.RegisterL10nEvent;
import mchorse.bbs_mod.api.events.RegisterCameraClipsEvent;
import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public class ImpactAddon implements BBSAddonMod
{
    public static final Link ID = new Link("impactfx", "impact_frame");
    public static final Link POWER_ID = new Link("impactfx", "power_fx");
    public static final String IMPACT_LABEL = "ALI impact vfx";
    public static final String POWER_LABEL = "ALI power fx";
    public static final Link SLASH_ID = new Link("impactfx", "slash_fx");
    public static final String SLASH_LABEL = "ALI slash fx";
    public static final Link RIFT_ID = new Link("impactfx", "rift_fx");
    public static final String RIFT_LABEL = "ALI rift fx";

    @Subscribe
    public void registerClips(RegisterCameraClipsEvent event)
    {
        event.factory.register(ID, ImpactClip.class, new ClipFactoryData(Icons.FIVE_STAR, 0xFFD23C));
        event.factory.register(POWER_ID, PowerClip.class, new ClipFactoryData(Icons.LIGHT, 0x9C6CFF));
        event.factory.register(SLASH_ID, SlashClip.class, new ClipFactoryData(Icons.SHAPES, 0x4FC3F7));
        event.factory.register(RIFT_ID, RiftClip.class, new ClipFactoryData(Icons.PARTICLE, 0xB04CFF));
    }

    @Subscribe
    public void registerRenderers(RegisterClipRenderersEvent event)
    {
        event.register(ImpactClip.class, new UILabelRenderer<ImpactClip>(IMPACT_LABEL));
        event.register(PowerClip.class, new UILabelRenderer<PowerClip>(POWER_LABEL));
        event.register(SlashClip.class, new UILabelRenderer<SlashClip>(SLASH_LABEL));
        event.register(RiftClip.class, new UILabelRenderer<RiftClip>(RIFT_LABEL));
    }

    @Subscribe
    public void registerPanels(RegisterClipPanelsEvent event)
    {
        event.register(ImpactClip.class, UIImpactClip::new);
        event.register(PowerClip.class, UIPowerClip::new);
        event.register(SlashClip.class, UISlashClip::new);
        event.register(RiftClip.class, UIRiftClip::new);
    }



    @Subscribe
    public void registerLang(RegisterL10nEvent event)
    {
        MapType map = new MapType();

        name(event, map, ID, IMPACT_LABEL);
        name(event, map, POWER_ID, POWER_LABEL);
        name(event, map, SLASH_ID, SLASH_LABEL);
        name(event, map, RIFT_ID, RIFT_LABEL);

        event.l10n.overwrite(map);
    }

    /** L10n.overwrite only replaces keys that already exist, so create the key first. */
    private static void name(RegisterL10nEvent event, MapType map, Link link, String label)
    {
        String key = "bbs.ui.camera.clips." + link.toString();

        event.l10n.getKey(key);
        map.putString(key, label);
    }
}
