package segfault.abak.backup;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import segfault.abak.BuildConfig;
import segfault.abak.sdkclient.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

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
                null
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
    public void validateEmptyPlugin() {
        final BackupOptions o = BackupOptions.create(new ArrayList<>(Arrays.asList("a", "b")),
                new ArrayList<>(0),
                null);
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
                null);
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
                Uri.parse("file:///"));
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
                Uri.parse("file:///"));
        assertTrue(o.application().isEmpty());
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
                Uri.parse("file:///"));
        assertNotNull(o.location());
        assertEquals(o.plugins().size(), 1);
        assertTrue(o.validate(context));
    }
}