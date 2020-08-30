package segfault.abak.backup.ui;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import segfault.abak.common.packaging.PrebuiltPackagers;
import segfault.abak.sdkclient.Plugin;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = { Build.VERSION_CODES.O_MR1 })
public class BackupOptionsTest {
    private BackupOptions options;
    private Context context = ApplicationProvider.getApplicationContext();

    @Before
    public void setup() {
        options = BackupOptions.create(
                new ArrayList<>(Arrays.asList("1", "2", "3")),
                new ArrayList<>(0),
                null,
                0
        );
    }

    @Test
    public void create() {
        assertNotNull(options);
    }

    @Test
    public void application() {
        assertEquals(options.application(), new ArrayList<>(Arrays.asList("1", "2", "3")));
    }

    @Test
    public void plugins() {
        assertEquals(options.plugins(), new ArrayList<>(0));
    }

    @Test
    public void location() {
        assertNull(options.location());
    }

    @Test
    public void resolvePackager() {
        assertEquals(PrebuiltPackagers.PREBUILT_PACKAGERS[0],
                options.resolvePackager());
    }

    @Test
    public void validateEmptyPlugin() {
        final BackupOptions o = BackupOptions.create(new ArrayList<>(Arrays.asList("a", "b")),
                new ArrayList<>(0),
                null,
                0);
        assertFalse(o.validate(context));
    }

    @Test
    public void validateNullLocation() {
        final BackupOptions o = BackupOptions.create(new ArrayList<>(Arrays.asList("a", "b")),
                new ArrayList<>(Arrays.asList(Plugin.create(ComponentName.unflattenFromString("android/android"),
                        1,
                        null,
                        null,
                        false))),
                null,
                0);
        assertFalse(o.validate(context));
    }

    @Test
    public void validateNoSettingsPlugin() {
        final BackupOptions o = BackupOptions.create(new ArrayList<>(Arrays.asList("a", "b")),
                new ArrayList<>(Arrays.asList(Plugin.create(ComponentName.unflattenFromString("android/android"),
                        1,
                        null,
                        null,
                        true))),
                Uri.parse("file:///"),
                0);
        assertFalse(o.validate(context));
    }

    @Test
    public void validateNoApplications() {
        final BackupOptions o = BackupOptions.create(new ArrayList<>(0),
                new ArrayList<>(Arrays.asList(Plugin.create(ComponentName.unflattenFromString("android/android"),
                        1,
                        null,
                        null,
                        false))),
                Uri.parse("file:///"),
                0);
        assertTrue(o.application().isEmpty());
        assertFalse(o.validate(context));
    }

    @Test
    public void validateInvalidPackager() {
        final BackupOptions o = BackupOptions.create(new ArrayList<>(0),
                new ArrayList<>(Arrays.asList(Plugin.create(ComponentName.unflattenFromString("android/android"),
                        1,
                        null,
                        null,
                        false))),
                Uri.parse("file:///"),
                -1);
        assertTrue(o.application().isEmpty());
        assertNull(o.resolvePackager());
        assertFalse(o.validate(context));
    }

    @Test
    public void validateTrue() {
        final BackupOptions o = BackupOptions.create(new ArrayList<>(Arrays.asList("a", "b")),
                new ArrayList<>(Arrays.asList(Plugin.create(ComponentName.unflattenFromString("android/android"),
                        1,
                        null,
                        null,
                        false))),
                Uri.parse("file:///"),
                0);
        assertNotNull(o.location());
        assertEquals(o.plugins().size(), 1);
        assertTrue(o.validate(context));
    }
}