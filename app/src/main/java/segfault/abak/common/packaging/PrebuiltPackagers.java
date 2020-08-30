package segfault.abak.common.packaging;

import segfault.abak.common.packaging.tar.TarPackager;

public final class PrebuiltPackagers {
    public static final Packager[] PREBUILT_PACKAGERS = new Packager[]{
            new TarPackager()
    };
}
