package honkhonk.threadwatch;


import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;

import honkhonk.threadwatch.helpers.Common;
import honkhonk.threadwatch.managers.ThreadDataManager;
import honkhonk.threadwatch.models.ThreadModel;

public class SettingsActivity extends AppCompatPreferenceActivity {
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragment {
        public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
            final Resources resources = SettingsFragment.this.getResources();

            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(this.getActivity(),
                        resources.getString(R.string.pref_backup_fail_toast),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (requestCode == Common.EXPORT_REQUEST_CODE) {
                writeBackupFile(resultData.getData());
            } else if (requestCode == Common.IMPORT_REQUEST_CODE) {
                restoreBackupFromFile(resultData.getData());
            }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_main);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("pref_refresh_rate"));

            final boolean canVibrate = getActivity()
                .getIntent().getExtras().getBoolean(Common.PREFS_CAN_VIBRATE, true);

            final Preference vibratePref = findPreference("pref_notify_vibrate");
            if (!canVibrate) {
                vibratePref.setEnabled(false);
                vibratePref.setSummary(getResources()
                    .getString(R.string.pref_notify_vibrate_disabled));
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibratePref.setEnabled(false);
                vibratePref.setSummary(R.string.pref_notify_vibrate_sysmanaged);
            }

            final Preference importPreference = findPreference("pref_backup_import");
            importPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    importFile();
                    return false;
                }
            });

            final Preference exportPreference = findPreference("pref_backup_export");
            exportPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    exportFile();
                    return false;
                }
            });

            // Disable export if you don't have any threads to export
            final ArrayList<ThreadModel> threads = ThreadDataManager.getThreadList(getActivity());
            if (threads.size() == 0) {
                exportPreference.setEnabled(false);
                exportPreference.setSummary(R.string.pref_backup_export_disabled);
            }
        }

        /**
         * Launches an intent to open a file
         */
        private void importFile() {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

            // Filter to only show results that can be "opened", such as a
            // file (as opposed to a list of contacts or timezones)
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");

            startActivityForResult(intent, Common.IMPORT_REQUEST_CODE);
        }

        /**
         * Launches an intent to set an output file location
         */
        private void exportFile() {
            final String time = Calendar.getInstance().getTime().toString().replace(" ", "_");
            String filename = "threadwatch_backup_" + time + ".json";

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, filename);

            startActivityForResult(intent, Common.EXPORT_REQUEST_CODE);
        }

        /**
         * Writes out the thread list as a JSON string to a file
         * @param outputFileLocation Backup file location
         */
        private void writeBackupFile(final Uri outputFileLocation) {
            final Resources resources = SettingsFragment.this.getResources();

            try {
                ParcelFileDescriptor fileHandle =
                        SettingsFragment.this.getActivity().getContentResolver().
                                openFileDescriptor(outputFileLocation, "w");

                FileOutputStream fileOutputStream =
                        new FileOutputStream(
                                fileHandle.getFileDescriptor());

                String listDataAsJson = ThreadDataManager.getThreadListAsString(getActivity());
                fileOutputStream.write(listDataAsJson.getBytes());
                fileOutputStream.close();
                fileHandle.close();
            } catch (Exception e) {
                Toast.makeText(this.getActivity(),
                        resources.getString(R.string.pref_backup_export_fail_toast),
                        Toast.LENGTH_SHORT).show();
            }

            Toast.makeText(this.getActivity(),
                    resources.getString(R.string.pref_backup_export_ok_toast),
                    Toast.LENGTH_SHORT).show();
        }

        /**
         * Reads and restores from a backup JSON file if successfully validated
         * @param inputFileLocation Backup file location
         */
        private void restoreBackupFromFile(final Uri inputFileLocation) {
            final Resources resources = SettingsFragment.this.getResources();
            String backupDataAsString = "";
            boolean restoreFailed = false;

            try {
                InputStream inputStream =
                        getActivity().getContentResolver().openInputStream(inputFileLocation);
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(
                                inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String currentLine;
                while ((currentLine = reader.readLine()) != null) {
                    stringBuilder.append(currentLine);
                }
                inputStream.close();

                backupDataAsString = stringBuilder.toString();
            } catch (Exception e) {
                restoreFailed = true;
            }

            // Parse backup data to validate
            try {
                final ArrayList<ThreadModel>backedUpThreads = (new Gson()).fromJson(backupDataAsString,
                        new TypeToken<ArrayList<ThreadModel>>() {}.getType());
                if (backedUpThreads.size() == 0) {
                    restoreFailed = true;
                } else {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                    dialog.setTitle(R.string.pref_backup_import_confirm_title);
                    dialog.setMessage(R.string.pref_backup_import_confirm);
                    dialog.setNeutralButton(R.string.pref_backup_import, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ThreadDataManager.updateThreadList(getActivity(), backedUpThreads);

                            // Thread list should refresh
                            Intent shouldRefreshIntent = new Intent();
                            shouldRefreshIntent.putExtra(Common.SETTINGS_CLOSED_SHOULD_REFRESH, true);
                            getActivity().setResult(Activity.RESULT_OK, shouldRefreshIntent);

                            Toast.makeText(SettingsFragment.this.getActivity(),
                                    resources.getString(R.string.pref_backup_import_success),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                    dialog.create();
                    dialog.show();
                }
            } catch (Exception e) {
                restoreFailed = true;
            }

            if (restoreFailed) {
                Toast.makeText(this.getActivity(),
                        resources.getString(R.string.pref_backup_import_fail),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
