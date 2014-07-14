package de.bbukowski.notifcount;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class MainActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Button button = (Button) findViewById(R.id.showTestNotif);
    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        showTestNotification();
      }
    });
  }

  private void showTestNotification() {
    Intent resultIntent = new Intent(this, MainActivity.class);

    PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0,
        resultIntent,
        PendingIntent.FLAG_UPDATE_CURRENT);

    Notification n = new Notification.Builder(this)
        .setContentTitle(getResources()
            .getText(R.string.app_name))
        .setContentText(getResources().getText(R.string.test_notification_text))
        .setSmallIcon(R.drawable.ic_stat_notify)
        .setContentIntent(resultPendingIntent)
        .setNumber(5)
        .setAutoCancel(true).build();

    NotificationManager notificationManager = (NotificationManager)
        getSystemService(Context.NOTIFICATION_SERVICE);
    notificationManager.notify(0, n);
  }
}
