
package com.woalk.apps.xposed.notifcount;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v4.app.NotificationCompat;
import android.widget.EditText;

import java.util.Random;

/**
 * Created by bbukowski on 07.08.14. Maintained by woalk since 2015/05/05.
 */
public class SettingsFragment extends PreferenceFragment implements
    OnSharedPreferenceChangeListener {

  private Random mRandom = new Random(System.currentTimeMillis());

  @SuppressWarnings("deprecation")
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getPreferenceManager().setSharedPreferencesMode(PreferenceActivity.MODE_WORLD_READABLE);
    addPreferencesFromResource(R.xml.preferences);

    final SettingsHelper setH = new SettingsHelper(getActivity());
    if (setH.getPreferenceVersion() < 2) {
      if (setH.getCachedList().size() > 0) {
        new AlertDialog.Builder(getActivity())
            .setTitle(R.string.pref_clear_info_title)
            .setMessage(R.string.pref_clear_info_summary)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                setH.clearLists();
                setH.setPreferenceVersion(2);
              }
            })
            .create()
            .show();
      } else {
        setH.setPreferenceVersion(2);
      }
    }

    Preference testNotif = findPreference("test_notif_wo_number");
    testNotif.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        showTestNotification(false);
        return true;
      }
    });

    testNotif = findPreference("test_notif_w_number");
    testNotif.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        showTestNotification(true);
        return true;
      }
    });

    testNotif = findPreference("test_notif_wt_number");
    testNotif.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        showTestTextNumberedNotification(2);
        return true;
      }
    });

    testNotif = findPreference("test_notif_wc_number");
    testNotif.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        showTestTextNumberedNotification(3);
        return true;
      }
    });

    testNotif = findPreference("test_notif_ws_number");
    testNotif.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        showTestTextNumberedNotification(1);
        return true;
      }
    });

    EditTextPreference numberSizePref = (EditTextPreference) findPreference("number_size");
    numberSizePref.setSummary(getString(R.string.pref_appear_number_size_summary,
            numberSizePref.getText()));

    EditTextPreference numberBadgeAlphaPref = (EditTextPreference) findPreference("number_badge_alpha");
    numberBadgeAlphaPref.setSummary(getString(R.string.pref_appear_number_badge_alpha_summary,
            numberBadgeAlphaPref.getText()));

    ListPreference numberBadgeShapePref = (ListPreference) findPreference("number_badge_shape");
    numberBadgeShapePref.setSummary(numberBadgeShapePref.getEntry());

    Preference showAppIcon = findPreference("show_app_icon");
    showAppIcon.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        PackageManager packageManager = getActivity().getPackageManager();
        int state = (Boolean) newValue ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        ComponentName aliasName = new ComponentName(getActivity(), SettingsHelper.PACKAGE_NAME
            + ".Activity-Alias");
        packageManager.setComponentEnabledSetting(aliasName, state, PackageManager.DONT_KILL_APP);
        return true;
      }
    });

    Preference versionNumber = findPreference("version_number");
    PackageInfo pInfo;
    String version;
    try {
      pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
      version = pInfo.versionName;
    } catch (NameNotFoundException e) {
      version = "Error loading version information.";
      e.printStackTrace();
    }
    versionNumber.setSummary(String.format(versionNumber.getSummary().toString(), version));
  }

  private void showTestNotification(boolean setNumber) {
    Intent resultIntent = new Intent(getActivity(), SettingsActivity.class);

    PendingIntent resultPendingIntent = PendingIntent.getActivity(getActivity(), 0,
        resultIntent,
        PendingIntent.FLAG_UPDATE_CURRENT);

    NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity())
        .setContentTitle(getResources()
            .getText(R.string.app_name))
        .setContentText(getResources().getText(R.string.test_notification_text))
        .setSmallIcon(R.drawable.ic_stat_notify)
        .setContentIntent(resultPendingIntent)
        .setAutoCancel(true);

    NotificationManager notificationManager = (NotificationManager)
        getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

    if (setNumber) {
      int number = 0;
      while (number < 2) {
        number = mRandom.nextInt(30);
      }
      builder.setNumber(number);

      notificationManager.cancelAll();
    }

    Notification n = builder.build();

    notificationManager.notify(0, n);
  }

  private void showTestTextNumberedNotification(int summary1_title2_content3) {
    Intent resultIntent = new Intent(getActivity(), SettingsActivity.class);

    PendingIntent resultPendingIntent = PendingIntent.getActivity(getActivity(), 0,
        resultIntent,
        PendingIntent.FLAG_UPDATE_CURRENT);

    String content = getResources().getString(R.string.test_notification_text);
    int number = mRandom.nextInt(30);
    
    String numberString = String.format(getResources().getString(R.string.test_notification_only_text), number);
    String appName = getResources().getString(R.string.app_name);

    NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity())
        .setSmallIcon(R.drawable.ic_stat_notify)
        .setContentTitle(summary1_title2_content3 == 2 ? numberString : appName)
        .setContentText(summary1_title2_content3 == 3 ? numberString : content)
        .setContentIntent(resultPendingIntent)
        .setAutoCancel(true)
        .setStyle(new NotificationCompat.BigTextStyle()
              .bigText(summary1_title2_content3 == 3 ? numberString : content)
              .setSummaryText(summary1_title2_content3 == 1 ? numberString : appName));

    Notification n = builder.build();

    NotificationManager notificationManager = (NotificationManager)
        getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.cancelAll();
    notificationManager.notify(0, n);
    notificationManager.notify(0, n);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    Preference pref = findPreference(key);

    if (pref instanceof ListPreference) {
      ListPreference listPref = (ListPreference) pref;
      pref.setSummary(listPref.getEntry());
    } else if (pref.getKey().equals("number_size")) {
      EditTextPreference editTextPref = (EditTextPreference) pref;
      editTextPref.setSummary(getString(R.string.pref_appear_number_size_summary,
          editTextPref.getText()));
    } else if (pref.getKey().equals("number_badge_alpha")) {
      EditTextPreference editTextPref = (EditTextPreference) pref;
      if (Integer.valueOf(editTextPref.getText()) > 255)
        editTextPref.setText("255");
      editTextPref.setSummary(getString(R.string.pref_appear_number_badge_alpha_summary,
              editTextPref.getText()));
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    getPreferenceScreen().getSharedPreferences()
        .registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onPause() {
    super.onPause();
    getPreferenceScreen().getSharedPreferences()
        .unregisterOnSharedPreferenceChangeListener(this);
  }
}
