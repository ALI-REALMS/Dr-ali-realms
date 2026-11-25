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

    @Subscribe
    public void registerClips(RegisterCameraClipsEvent event)
    {
        event.factory.register(ID, ImpactClip.class, new ClipFactoryData(Icons.FIVE_STAR, 0xFFD23C));
        event.factory.register(POWER_ID, PowerClip.class, new ClipFactoryData(Icons.LIGHT, 0x9C6CFF));
    }

    @Subscribe
    public void registerRenderers(RegisterClipRenderersEvent event)
    {
        event.register(ImpactClip.class, new UILabelRenderer<ImpactClip>(IMPACT_LABEL));
        event.register(PowerClip.class, new UILabelRenderer<PowerClip>(POWER_LABEL));
    }

    @Subscribe
    public void registerPanels(RegisterClipPanelsEvent event)
    {
        event.register(ImpactClip.class, UIImpactClip::new);
        event.register(PowerClip.class, UIPowerClip::new);
    }



@Subscribe
public void registerLang(RegisterL10nEvent event)
{
    MapType map = new MapType();

    map.putString(
        "bbs.ui.camera.clips." + ID.toString(),
        "ALI Impact FX"
    );

    event.l10n.overwrite(map);
}

}
