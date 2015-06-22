
package com.woalk.apps.xposed.notifcount;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AppListActivity extends ListActivity {

  private static SettingsHelper mSettingsHelper;
  private static AppList.AppListAdapter mAdapter;

  private boolean isWhitelist;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mSettingsHelper = new SettingsHelper(this);

    getActionBar().setDisplayHomeAsUpEnabled(true);
    getActionBar().setTitle(R.string.pref_apps_increase_onupdate_title);

    this.isWhitelist = mSettingsHelper.isWhitelist();

    mAdapter = new AppList.AppListAdapter(this, mSettingsHelper, isWhitelist);
    this.setListAdapter(mAdapter);

    this.getListView().setFastScrollEnabled(true);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home)
      onBackPressed();
    return true;
  }
}
