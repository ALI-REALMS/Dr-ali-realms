package com.alirealms.impactfx.client;

import mchorse.bbs_mod.client.BBSRendering;

/** Shader-pack (Iris) detection. */
final class Compat
{
    private Compat() {}

    static boolean iris()
    {
        try
        {
            return BBSRendering.isIrisWorldShadersEnabled();
        }
        catch (Throwable t)
        {
            return false;
        }
    }
}
