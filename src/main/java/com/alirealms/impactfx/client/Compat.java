package com.alirealms.impactfx.client;

import java.lang.reflect.Method;

/** Shader-pack (Iris) detection, by reflection so there is no hard dependency. */
final class Compat
{
    private static Object api;
    private static Method inUse;
    private static boolean tried;

    private Compat() {}

    static boolean iris()
    {
        try
        {
            if (!tried)
            {
                tried = true;

                Class<?> c = Class.forName("net.irisshaders.iris.api.v0.IrisApi");

                api = c.getMethod("getInstance").invoke(null);
                inUse = c.getMethod("isShaderPackInUse");
            }

            if (inUse != null && (Boolean) inUse.invoke(api))
            {
                return true;
            }
        }
        catch (Throwable t)
        {
            /* Iris is not installed */
        }

        try
        {
            return mchorse.bbs_mod.client.BBSRendering.isIrisWorldShadersEnabled();
        }
        catch (Throwable t)
        {
            return false;
        }
    }

    /** BBS renders films again in the shader pack's shadow pass (other camera/projection): effects must skip it */
    static boolean shadow()
    {
        try
        {
            return mchorse.bbs_mod.client.BBSRendering.isIrisShadowPass();
        }
        catch (Throwable t)
        {
            return false;
        }
    }
}
