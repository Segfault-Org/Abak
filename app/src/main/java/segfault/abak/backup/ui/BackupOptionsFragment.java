package segfault.abak.backup.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import java9.util.Optional;
import java9.util.stream.Collectors;
import java9.util.stream.StreamSupport;
import moe.shizuku.preference.CheckBoxPreference;
import moe.shizuku.preference.Preference;
import moe.shizuku.preference.PreferenceCategory;
import moe.shizuku.preference.PreferenceFragment;
import segfault.abak.R;
import segfault.abak.common.AppUtils;
import segfault.abak.common.widgets.LongClickableCheckboxPreference;
import segfault.abak.common.widgets.Nav;
import segfault.abak.sdk.SdkConstants;
import segfault.abak.sdkclient.Plugin;
import segfault.abak.sdkclient.SDKClient;

import java.util.*;

/**
 * Start fragment of backup action.
 */
public class BackupOptionsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "BackupOptionsFragment";

    private static final int RC_SELECT_LOCATION = 0;
    private static final int RC_PLUGIN_SETTINGS = 1;

    private static final String STATE_OPTIONS = BackupOptionsFragment.class.getName() +
            ".STATE_OPTIONS";
    private static final String STATE_CURRENTLY_OPEN_PLUGIN_SETTINGS = BackupOptionsFragment.class.getName() +
            ".STATE_CURRENTLY_OPEN_PLUGIN_SETTINGS";
    private static final String STATE_PLUGIN_SETTINGS_RESULT_CODE = BackupOptionsFragment.class.getName() +
            ".STATE_PLUGIN_SETTINGS_RESULT_CODE";
    private static final String STATE_PLUGIN_SETTINGS_RESULT_INTENT = BackupOptionsFragment.class.getName() +
            ".STATE_PLUGIN_SETTINGS_RESULT_INTENT";

    private BackupOptions mOptions = BackupOptions.create(new ArrayList<>(0), new ArrayList<>(0), null);

    // Ignored from saved instance
    private ArrayList<Plugin> mAllPlugins;

    /**
     * "Backup" action. It will be constructed and assigned during onCreateOptionsMenu()
     * method. Meanwhile, the state changes of it are suppressed.
     *
     * Ignored from saved instance
     */
    private MenuItem mActionMenuItem;

    /**
     * Current opened plugin. Because the service component is unique, we use that.
     */
    private ComponentName mCurrentlyOpenedPluginServiceComponent;
    private Integer mPluginSettingsResultCode;
    private Intent mPluginSettingsResultIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            final BackupOptions saved = savedInstanceState.getParcelable(STATE_OPTIONS);
            if (saved != null) mOptions = saved;
            mCurrentlyOpenedPluginServiceComponent = savedInstanceState.getParcelable(STATE_CURRENTLY_OPEN_PLUGIN_SETTINGS);
            if (savedInstanceState.get(STATE_PLUGIN_SETTINGS_RESULT_CODE) != null) {
                mPluginSettingsResultCode = savedInstanceState.getInt(STATE_PLUGIN_SETTINGS_RESULT_CODE);
            }
            mPluginSettingsResultIntent = savedInstanceState.getParcelable(STATE_PLUGIN_SETTINGS_RESULT_INTENT);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Setup view
        addPreferencesFromResource(R.xml.preference_backup);
        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i ++) {
            getPreferenceScreen().getPreference(i).setOnPreferenceChangeListener(this);
        }

        findPreference("backup_location").setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/x-tar");
            intent.putExtra(Intent.EXTRA_TITLE, "backup.tar");
            startActivityForResult(intent, RC_SELECT_LOCATION);
            return true;
        });

        // final DialogPreference appPref = (DialogPreference) findPreference("backup_app");
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        Log.d(TAG, "onCreateOptionsMenu");
        mActionMenuItem = menu.add(0, 0, 0, R.string.backup_action);
        mActionMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        mActionMenuItem.setIcon(R.drawable.ic_baseline_send_24);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "changed()");
        if (preference.getKey().startsWith("plugin_")) {
            final Plugin plugin = mAllPlugins.get(Integer.parseInt(preference.getKey().substring("plugin_".length())));
            final boolean enabled = (Boolean) newValue;
            if (enabled) {
                if (plugin.requireSettings() && plugin.options() == null) {
                    // Open the settings UI first. Then actually check if when the user comes back.
                    openSettings(plugin);
                } else {
                    mOptions.plugins().add(plugin);
                }
            } else {
                mOptions.plugins().remove(plugin);
            }
        }
        switch (preference.getKey()) {
            case "backup_app":
                final List<String> lst = Arrays.asList(newValue.toString().replace("\n", ";").trim().split(";"));
                Log.d(TAG, lst.toString());
                final Set<String> dup = new HashSet<>(lst.size());
                for (final String app : lst) {
                    if (!dup.add(app)) {
                        Toast.makeText(requireContext(), R.string.backup_app_dup, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
                mOptions = BackupOptions.create(new ArrayList<>(lst),
                        mOptions.plugins(),
                        mOptions.location());
                break;
        }
        // Validate the options.
        dataToUI();
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_OPTIONS, mOptions);
        outState.putParcelable(STATE_CURRENTLY_OPEN_PLUGIN_SETTINGS, mCurrentlyOpenedPluginServiceComponent);
        if (mPluginSettingsResultCode != null) outState.putInt(STATE_PLUGIN_SETTINGS_RESULT_CODE, mPluginSettingsResultCode);
        outState.putParcelable(STATE_PLUGIN_SETTINGS_RESULT_INTENT, mPluginSettingsResultIntent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RC_SELECT_LOCATION:
                if (resultCode != Activity.RESULT_OK) return;
                mOptions = BackupOptions.create(mOptions.application(),
                        mOptions.plugins(),
                        data.getData());
                dataToUI();
                break;
            case RC_PLUGIN_SETTINGS:
                // Because onActivityResult is called before onResume, let's suppress until the plugin list is refreshed.
                mPluginSettingsResultCode = resultCode;
                // The intent could be null, so it will not be taken into consideration.
                mPluginSettingsResultIntent = data;
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // Restore suppression.
        dataToUI();
    }

    /**
     * Refresh the UI according to new data.
     */
    private void dataToUI() {
        // Suppress if it is not available right now.
        if (mActionMenuItem != null) {
            mActionMenuItem.setEnabled(mOptions.validate(requireContext()));
        }
        findPreference("backup_app").setSummary(getString(R.string.backup_app_summary, mOptions.application().size()));
        findPreference("backup_location").setSummary(mOptions.location() != null ?
                mOptions.location().toString()
                : null);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                final Bundle args = new Bundle();
                args.putParcelable(BackupProgressFragment.EXTRA_OPTIONS,
                        mOptions.toThreadOptions());
                Nav.get(this).navigate(R.id.action_fragment_backup_to_dialog_backup, args);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Selected plugins may change. We have to reload here.
        Log.d(TAG, "Refreshing plugins");
        mAllPlugins = new ArrayList<>(SDKClient.listPlugins(requireContext()));
        mOptions = BackupOptions.create(mOptions.application(),
                new ArrayList<>(StreamSupport.stream(mOptions.plugins())
                        .filter(plugin -> mAllPlugins.contains(plugin))
                        .collect(Collectors.toList())),
                mOptions.location());
        // Also the UI
        final PreferenceCategory category = (PreferenceCategory) findPreference("backup_include");
        category.removeAll();
        StreamSupport.stream(mAllPlugins)
                .map(plugin -> {
                    final CheckBoxPreference preference;
                    boolean selected = mOptions.plugins().contains(plugin);
                    if (selected) {
                        // Replace with the selected one
                        plugin = mOptions.plugins().get(mOptions.plugins().indexOf(plugin));
                    }
                    if (plugin.settingsActivity() != null) {
                        final Plugin p1 = plugin; // TODO: Ugly design
                        preference = new LongClickableCheckboxPreference(requireContext(), view -> {
                            if (!mOptions.plugins().contains(p1)) {
                                // The plugin is not selected. Ignore.
                                Log.i(TAG, "The plugin is not selected.");
                                return false;
                            }
                            final Plugin latestPlugin = mOptions.plugins().get(mOptions.plugins().indexOf(p1));
                            if (latestPlugin.settingsActivity() == null) return false; // This will not happen actually.
                            openSettings(latestPlugin);
                            return true;
                        });
                    } else {
                        preference = new CheckBoxPreference(requireContext());
                    }
                    preference.setChecked(selected);
                    preference.setTitle(plugin.loadTitle(requireContext()));
                    preference.setPersistent(false);
                    preference.setKey("plugin_" + mAllPlugins.indexOf(plugin));
                    preference.setOnPreferenceChangeListener(BackupOptionsFragment.this);
                    final CharSequence name = AppUtils.appName(plugin.component().getPackageName(), requireContext());
                    preference.setSummaryOff(Html.fromHtml(getString(plugin.requireSettings() ? R.string.backup_plugin_summary_conf_required : R.string.backup_plugin_summary,
                            name)));
                    preference.setSummaryOn(Html.fromHtml(getString(plugin.settingsActivity() != null ? R.string.backup_plugin_summary_conf : R.string.backup_plugin_summary,
                            name)));
                    return preference;
                })
                .forEach(category::addPreference);

        // Resume the suppression
        if (mPluginSettingsResultCode != null &&
        mCurrentlyOpenedPluginServiceComponent != null) {
            Log.d(TAG, "Received options update: " + mCurrentlyOpenedPluginServiceComponent);
            // Resolve the plugin
            final Optional<Plugin> pluginO = StreamSupport.stream(mAllPlugins)
                    .filter(plugin -> mCurrentlyOpenedPluginServiceComponent.equals(plugin.component()))
                    .findFirst();
            final Optional<Plugin> pluginSelected = StreamSupport.stream(mOptions.plugins())
                    .filter(plugin -> mCurrentlyOpenedPluginServiceComponent.equals(plugin.component()))
                    .findFirst();
            // The plugin may be uninstalled when configuring.
            if (pluginO.isPresent()) {
                Plugin targetPlugin = pluginO.get();
                // Override if it is selected.
                final boolean selected = pluginSelected.isPresent();
                if (selected) targetPlugin = pluginSelected.get();
                Log.i(TAG, "Plugin: " + targetPlugin + ", selected: " + selected);
                if (mPluginSettingsResultCode == Activity.RESULT_OK) {
                    // Setting a null Intent will result in the options to be set to zero.
                    final Bundle bundle = mPluginSettingsResultIntent == null ? null :
                            mPluginSettingsResultIntent.getBundleExtra(SdkConstants.EXTRA_SETTINGS);
                    Log.d(TAG, "New bundle: " + bundle + " (" + (bundle != null ? bundle.size() : "-1") + ")");
                    final Plugin newPlugin = Plugin.create(targetPlugin.component(),
                            targetPlugin.version(),
                            bundle,
                            targetPlugin.settingsActivity(),
                            targetPlugin.requireSettings());
                    if (selected) {
                        final int index = mOptions.plugins().indexOf(targetPlugin);
                        // Check if it is required to set and the user cleared the option.
                        if (newPlugin.requireSettings() && newPlugin.options() == null) {
                            Log.e(TAG, "The user cleared the option, removing");
                            mOptions.plugins().remove(index);
                            // Update UI, and onPreferenceChange will be called again, which does not matter.
                            final CheckBoxPreference preference = (CheckBoxPreference) findPreference("plugin_" + mAllPlugins.indexOf(targetPlugin));
                            preference.setChecked(false);
                        } else {
                            Log.d(TAG, "Updating plugin at " + index);
                            mOptions.plugins().set(index, newPlugin);
                        }
                    } else {
                        // Check before selecting.
                        if ((newPlugin.requireSettings() && newPlugin.options() != null) ||
                                (!newPlugin.requireSettings())) {
                            // Select now. See onPreferenceChange
                            // Do a manual select.
                            Log.d(TAG, "Appending the plugin.");
                            mOptions.plugins().add(newPlugin);
                            // Update UI, and onPreferenceChange will be called again, which does not matter.
                            final CheckBoxPreference preference = (CheckBoxPreference) findPreference("plugin_" + mAllPlugins.indexOf(targetPlugin));
                            preference.setChecked(true);
                        } else {
                            Log.w(TAG, "User does not set while it is required. Ignoring.");
                        }
                    }
                } else {
                    Log.w(TAG, "The operation is cancelled by user.");
                }
            } else {
                Log.w(TAG, "The plugin is uninstalled, ignoring.");
            }
        }
        // Always reset
        mPluginSettingsResultCode = null;
        mPluginSettingsResultIntent = null;
        mCurrentlyOpenedPluginServiceComponent = null;
    }

    private void openSettings(@NonNull Plugin plugin) {
        mCurrentlyOpenedPluginServiceComponent = plugin.component();
        startActivityForResult(new Intent()
                .setComponent(plugin.settingsActivity())
                .putExtra(SdkConstants.EXTRA_SETTINGS, plugin.options()), RC_PLUGIN_SETTINGS);
    }
}
