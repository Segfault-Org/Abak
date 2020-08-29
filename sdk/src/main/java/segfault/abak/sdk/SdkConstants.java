package segfault.abak.sdk;

import androidx.annotation.RestrictTo;

public final class SdkConstants {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String META_SDK_VERSION = "sefgault.abak.sdk.version";

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String META_SETTINGS_ACTIVITY = "sefgault.abak.sdk.settings_activity";

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String META_REQUIRE_SETTINGS = "sefgault.abak.sdk.require_settings";

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String ACTION_PLUGIN_SERVICE = PluginService.class.getName() + ".ACTION";

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final int CALLBACK_CODE_PROGRESS = 1;
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String CALLBACK_EXTRA_PROGRESS = PluginService.class.getName() + ".CALLBACK_EXTRA_PROGRESS";

    public static final String EXTRA_SETTINGS = SdkConstants.class.getPackage().getName() + ".EXTRA_SETTINGS";
}
