
package com.woalk.apps.xposed.notifcount;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v4.app.NotificationCompat;

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
        showTestTextNumberedNotification(false);
        return true;
      }
    });

    testNotif = findPreference("test_notif_ws_number");
    testNotif.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        showTestTextNumberedNotification(true);
        return true;
      }
    });

    ListPreference numberSizePref = (ListPreference) findPreference("number_size");
    numberSizePref.setSummary(numberSizePref.getEntry());

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

    if (setNumber) {
      int number = 0;
      while (number < 2) {
        number = mRandom.nextInt(30);
      }
      builder.setNumber(number);
    }

    Notification n = builder.build();

    NotificationManager notificationManager = (NotificationManager)
        getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.notify(0, n);
  }

  private void showTestTextNumberedNotification(boolean summaryTrue_titleFalse) {
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
        .setContentTitle(summaryTrue_titleFalse ? appName : numberString)
        .setContentText(content)
        .setContentIntent(resultPendingIntent)
        .setAutoCancel(true)
        .setStyle(new NotificationCompat.BigTextStyle()
            .bigText(content)
            .setSummaryText(summaryTrue_titleFalse ? numberString : appName));

    Notification n = builder.build();

    NotificationManager notificationManager = (NotificationManager)
        getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.notify(0, n);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    Preference pref = findPreference(key);

    if (pref instanceof ListPreference) {
      ListPreference listPref = (ListPreference) pref;
      pref.setSummary(listPref.getEntry());
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
