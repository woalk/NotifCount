
package com.woalk.apps.xposed.notifcount;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public class SettingsActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_replaceable);
    setSupportActionBar((Toolbar) findViewById(R.id.toolbar1));
    getFragmentManager().beginTransaction()
        .replace(android.R.id.widget_frame, new SettingsFragment())
        .commit();
  }
}
