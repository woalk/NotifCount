
package de.bbukowski.notifcount;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v4.app.NotificationCompat;

import java.util.Random;

/**
 * Created by bbukowski on 07.08.14.
 */

public class SettingsFragment extends PreferenceFragment {

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
}
