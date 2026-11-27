package dev.adryh01.yafabric;

import java.util.UUID;

/** Kept for the cape mixin: the lookup itself lives in YaNet. */
public final class CapeNet {
    private CapeNet() {
    }

    public static boolean hasCape(UUID id) {
        return YaNet.hasCape(id);
    }
}
