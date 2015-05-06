
package com.woalk.apps.xposed.notifcount;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
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

    ListPreference numberSizePref = (ListPreference) findPreference("number_size");
    numberSizePref.setSummary(numberSizePref.getEntry());
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
