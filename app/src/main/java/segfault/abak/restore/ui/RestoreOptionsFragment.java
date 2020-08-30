package segfault.abak.restore.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import java9.util.Optional;
import java9.util.stream.StreamSupport;
import moe.shizuku.preference.CheckBoxPreference;
import moe.shizuku.preference.PreferenceFragment;
import moe.shizuku.preference.PreferenceGroup;
import moe.shizuku.preference.TwoStatePreference;
import segfault.abak.R;
import segfault.abak.common.AppPluginPair;
import segfault.abak.common.AppUtils;
import segfault.abak.common.backupformat.entries.ApplicationEntryV1;
import segfault.abak.common.widgets.Nav;
import segfault.abak.restore.ApplicationEntryWithInput;
import segfault.abak.restore.RestoreOptions;
import segfault.abak.sdkclient.Plugin;
import segfault.abak.sdkclient.SDKClient;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Start fragment of restore action.
 */
public class RestoreOptionsFragment extends PreferenceFragment implements Handler.Callback {
    private static final String TAG = "RestoreOptionsFragment";

    private static final int RC_OPEN = 0;

    private static final String STATE_PKG = RestoreOptionsFragment.class.getName() +
            ".STATE_PKG";
    private static final String STATE_OPTIONS = RestoreOptionsFragment.class.getName() +
            ".STATE_OPTIONS";
    private static final String STATE_OPEN_DIALOG_DISPLAYED = RestoreOptionsFragment.class.getName() +
            ".STATE_OPEN_DIALOG_DISPLAYED";

    private static final int MSG_PARSE_DONE = 0;
    private static final int MSG_PARSE_FAIL = 1;

    private RestoreParseResult mPkg;
    private RestoreOptions mOptions;

    private boolean mOpenDialogDisplayed;
    private Thread mParseThread;

    private Handler mHandler;

    private final Queue<Message> mSuppressedMessages = new LinkedList<>();

    /**
     * "Restore" action. It will be constructed and assigned during onCreateOptionsMenu()
     * method. Meanwhile, the state changes of it are suppressed.
     *
     * Ignored from saved instance
     */
    private MenuItem mActionMenuItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mHandler = new Handler(Looper.myLooper(), this);
        if (savedInstanceState != null) {
            mPkg = savedInstanceState.getParcelable(STATE_PKG);
            mOptions = savedInstanceState.getParcelable(STATE_OPTIONS);
            mOpenDialogDisplayed = savedInstanceState.getBoolean(STATE_OPEN_DIALOG_DISPLAYED);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preference_restore);
        displayPkg();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        mActionMenuItem = menu.add(0, 0, 0, R.string.restore_action);
        mActionMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        mActionMenuItem.setIcon(R.drawable.ic_baseline_send_24);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_PKG, mPkg);
        outState.putParcelable(STATE_OPTIONS, mOptions);
        outState.putBoolean(STATE_OPEN_DIALOG_DISPLAYED, mOpenDialogDisplayed);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        validateMenu();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                final Bundle args = new Bundle();
                args.putParcelable(RestoreProgressFragment.EXTRA_RESTORE_OPTIONS,
                        mOptions);
                Nav.get(this).navigate(R.id.action_fragment_restore_to_dialog_restore, args);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mPkg == null && !mOpenDialogDisplayed && mParseThread == null) {
            final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("*/*");
            startActivityForResult(intent, RC_OPEN);
            mOpenDialogDisplayed = true;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RC_OPEN:
                mOpenDialogDisplayed = false;
                if (resultCode == Activity.RESULT_OK &&
                data.getData() != null) {
                    startParse(data.getData());
                } else {
                    Nav.get(this).navigateUp();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void onDestroy() {
        if (mParseThread != null) {
            mParseThread.interrupt();
        }
        super.onDestroy();
    }

    @Override
    public boolean handleMessage(@NonNull Message message) {
        // Suppress if we are not in the front.
        if (!isResumed()) {
            Log.d(TAG, "Suppressing message");
            mSuppressedMessages.add(Message.obtain(message));
            return false;
        }
        return doHandleMsg(message);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Resume the suppression
        while (true) {
            final Message msg = mSuppressedMessages.poll();
            if (msg == null) {
                Log.d(TAG, "We had gone through all messages.");
                break;
            }
            doHandleMsg(msg);
            msg.recycle();
        }
        displayPkg();
    }

    private void startParse(@NonNull Uri uri) {
        mParseThread = new Thread(new ParsePkgThread(uri, new ParsePkgThread.Callback() {
            @Override
            @WorkerThread
            public void success(@NonNull RestoreParseResult result) {
                if (mParseThread.isInterrupted()) return;
                mHandler.sendMessage(mHandler.obtainMessage(MSG_PARSE_DONE, result));
            }

            @Override
            @WorkerThread
            public void fail(@NonNull Throwable e) {
                if (mParseThread.isInterrupted()) return;
                mHandler.sendEmptyMessage(MSG_PARSE_FAIL);
            }
        }, requireContext()));
        mParseThread.start();
    }

    private boolean doHandleMsg(@NonNull Message message) {
        switch (message.what) {
            case MSG_PARSE_DONE:
                mPkg = (RestoreParseResult) message.obj;
                mOptions = RestoreOptions.create(new ArrayList<>(mPkg.entries().size()), mPkg.extractedFile());
                displayPkg();
                return true;
            case MSG_PARSE_FAIL:
                Toast.makeText(requireContext(), R.string.restore_parse_fail, Toast.LENGTH_LONG).show();
                Nav.get(this).navigateUp();
                return true;
        }
        return false;
    }

    @UiThread
    private void displayPkg() {
        if (!isResumed()) return;
        if (mPkg == null) return;
        getPreferenceScreen().setEnabled(true);
        ((PreferenceGroup) findPreference("restore_include")).removeAll();
        StreamSupport.stream(mPkg.entries())
                .filter(entry -> entry instanceof ApplicationEntryWithInput)
                .map(entry -> (ApplicationEntryWithInput) entry)
                .map(applicationEntry -> {
                    final ApplicationEntryV1 entry = applicationEntry.entry();
                    final File data = applicationEntry.data();

                    final Plugin plugin = SDKClient.resolvePlugin(entry.pluginComponent(), requireContext());
                    final TwoStatePreference preference = new CheckBoxPreference(requireContext());

                    if (plugin == null || plugin.version() < entry.pluginVersion()) {
                        // No longer available, remove.
                        final Optional<RestoreOptions.DataAppPluginPair> existing = StreamSupport.stream(mOptions.apps())
                                .filter(pair -> {
                                    // Do a swallow match
                                    return pair.pair().application().equals(entry.application()) &&
                                            pair.pair().plugin().component().equals(entry.pluginComponent());
                                })
                                .findFirst();
                        if (existing.isPresent())
                            mOptions.apps().remove(existing.get());
                    }

                    if (plugin == null) {
                        // The plugin is not installed
                        preference.setTitle(entry.pluginComponent().flattenToShortString());
                        preference.setEnabled(false);
                        preference.setChecked(false);
                        preference.setSummary(R.string.restore_plugin_not_found);
                    } else if (plugin.version() < entry.pluginVersion()) {
                        // The plugin is outdated
                        preference.setTitle(plugin.loadTitle(requireContext()));
                        preference.setEnabled(false);
                        preference.setChecked(false);
                        preference.setSummary(R.string.restore_plugin_outdated);
                    } else {
                        final RestoreOptions.DataAppPluginPair pair =
                                RestoreOptions.DataAppPluginPair.create(
                                        AppPluginPair.create(entry.application(), plugin),
                                data);
                        preference.setTitle(plugin.loadTitle(requireContext()));
                        preference.setEnabled(true);
                        preference.setChecked(mOptions.apps().contains(pair));
                        preference.setSummary(Html.fromHtml(getString(
                                R.string.restore_plugin_summary,
                                AppUtils.appName(plugin.component().getPackageName(), requireContext()),
                                AppUtils.appName(entry.application(), requireContext())
                        )));
                        preference.setOnPreferenceChangeListener((pref, newValue) -> {
                            if ((boolean) newValue)
                                mOptions.apps().add(pair);
                            else
                                mOptions.apps().remove(pair);
                            validateMenu();
                            return true;
                        });
                    }
                    return preference;
                })
                .forEach(preference -> {
                    ((PreferenceGroup) findPreference("restore_include")).addPreference(preference);
                });
        validateMenu();
    }

    private void validateMenu() {
        if (mActionMenuItem != null && mOptions != null) {
            mActionMenuItem.setEnabled(mOptions.validate(requireContext()));
        }
    }
}
