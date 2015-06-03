
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

/**
 * Created by bbukowski on 07.08.14. Maintained by woalk since 2015/05/05.
 */
public class AppListActivity extends ListActivity {

  private static SettingsHelper mSettingsHelper;
  private static AppListAdapter mAdapter;

  private boolean isWhitelist;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mSettingsHelper = new SettingsHelper(this);
    new LoadAppsInfoTask().execute();
    getActionBar().setDisplayHomeAsUpEnabled(true);
    getActionBar().setTitle(R.string.pref_apps_increase_onupdate_title);

    this.isWhitelist = mSettingsHelper.isWhitelist();

    this.getListView().setFastScrollEnabled(true);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home)
      onBackPressed();
    return true;
  }

  private static class AppInfo {
    String title;
    SettingsHelper.AppSetting app;
    Drawable icon;
  }

  private List<AppInfo> loadApps(ProgressDialog dialog) {
    PackageManager packageManager = getPackageManager();
    List<ApplicationInfo> packages = packageManager
        .getInstalledApplications(PackageManager.GET_META_DATA);
    List<AppInfo> apps = new ArrayList<AppInfo>();

    dialog.setMax(packages.size());
    int i = 1;

    for (ApplicationInfo app : packages) {
      AppInfo appInfo = new AppInfo();
      appInfo.title = (String) app.loadLabel(packageManager);
      appInfo.app = mSettingsHelper.getSetting(app.packageName);
      if (appInfo.app == null) {
        appInfo.app = new SettingsHelper.AppSetting(app.packageName);
        appInfo.app.setPreferredSetting(isWhitelist ? SettingsHelper.AppSetting.SETTING_STOCK :
                SettingsHelper.AppSetting.SETTING_AUTO);
      }
      appInfo.icon = app.loadIcon(packageManager);
      apps.add(appInfo);
      dialog.setProgress(i++);
    }

    Collections.sort(apps, new Comparator<AppInfo>() {
      @Override
      public int compare(AppInfo appInfo1, AppInfo appInfo2) {
        boolean app1 = mSettingsHelper.getListedIndex(appInfo1.app.getPackageName()) != -1;
        boolean app2 = mSettingsHelper.getListedIndex(appInfo2.app.getPackageName()) != -1;

        if (app1 == app2) {
          return appInfo1.title.compareToIgnoreCase(appInfo2.title);
        } else if (app1) {
          return -1;
        } else if (app2) {
          return 1;
        }

        return 0;
      }
    });

    return apps;
  }

  private class LoadAppsInfoTask extends AsyncTask<Void, Void, Void> {
    ProgressDialog dialog;
    List<AppInfo> appInfos;

    @Override
    protected void onPreExecute() {
      dialog = new ProgressDialog(AppListActivity.this);
      dialog.setMessage(getString(R.string.loading));
      dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      dialog.setCancelable(false);
      dialog.show();
    }

    @Override
    protected Void doInBackground(Void... params) {
      appInfos = loadApps(dialog);
      return null;
    }

    @Override
    protected void onProgressUpdate(Void... progress) {
      mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPostExecute(Void void_) {
      super.onPostExecute(void_);
      mAdapter = new AppListAdapter(AppListActivity.this, appInfos, isWhitelist);
      setListAdapter(mAdapter);
      dialog.dismiss();
    }
  }

  private static class AppListAdapter extends ArrayAdapter<AppInfo> {
    LayoutInflater mInflater;
    private final boolean isWhitelist;

    public AppListAdapter(Context context, List<AppInfo> items, boolean is_whitelist) {
      super(context, 0, items);
      mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      this.isWhitelist = is_whitelist;
    }

    private static class Holder {
      ImageView icon;
      TextView title;
      TextView summary;
      TextView value;
      LinearLayout itemlayout;
      RadioGroup radioG;
      RadioButton radio3;
      RadioButton radio4;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      final Holder holder;
      final AppInfo item = getItem(position);
      View view;

      if (convertView == null) {
        holder = new Holder();
        view = mInflater.inflate(R.layout.whitelist_item, parent, false);
        holder.icon = (ImageView) view.findViewById(R.id.icon);
        holder.title = (TextView) view.findViewById(android.R.id.title);
        holder.summary = (TextView) view.findViewById(android.R.id.summary);
        holder.value = (TextView) view.findViewById(R.id.value);
        holder.itemlayout = (LinearLayout) view.findViewById(R.id.itemlayout);
        holder.radioG = (RadioGroup) view.findViewById(R.id.radioG);
        holder.radio3 = (RadioButton) view.findViewById(R.id.radio3);
        holder.radio4 = (RadioButton) view.findViewById(R.id.radio4);
        view.setTag(holder);
      } else {
        view = convertView;
        holder = (Holder) view.getTag();
        holder.radioG.setVisibility(View.GONE);
      }

      holder.title.setText(item.title);
      holder.summary.setText(item.app.getPackageName());
      holder.icon.setImageDrawable(item.icon);

      holder.value.setText(item.app.toShortString());
      if (item.app.getPreferredSetting() != (isWhitelist ? SettingsHelper.AppSetting.SETTING_STOCK :
              SettingsHelper.AppSetting.SETTING_AUTO))
        holder.value.setTypeface(Typeface.DEFAULT_BOLD);
      else
        holder.value.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));

      holder.itemlayout.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          int vis = holder.radioG.getVisibility();
          if (vis == View.GONE) {
            holder.radioG.setVisibility(View.VISIBLE);

            final int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            final int heightSpec = View.MeasureSpec
                .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            holder.radioG.measure(widthSpec, heightSpec);
            ValueAnimator mAnimator = slideAnimator(0, holder.radioG.getMeasuredHeight(),
                holder.radioG);
            mAnimator.setInterpolator(new DecelerateInterpolator());
            mAnimator.start();
          } else {
            int finalHeight = holder.radioG.getHeight();
            ValueAnimator mAnimator = slideAnimator(finalHeight, 0, holder.radioG);
            mAnimator.addListener(new Animator.AnimatorListener() {
              @Override
              public void onAnimationStart(Animator animation) {
              }

              @Override
              public void onAnimationEnd(Animator animator) {
                // Height=0, but it set visibility to GONE
                holder.radioG.setVisibility(View.GONE);
              }

              @Override
              public void onAnimationCancel(Animator animation) {
              }

              @Override
              public void onAnimationRepeat(Animator animation) {
              }
            });
            mAnimator.setInterpolator(new DecelerateInterpolator());
            mAnimator.start();
          }
        }
      });

      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
        holder.radio3.setVisibility(View.GONE);
        holder.radio4.setVisibility(View.GONE);
      }

      holder.radioG.setOnCheckedChangeListener(null);

      int checkedRadio = isWhitelist ? R.id.radio2 : R.id.radio0;
      switch (item.app.getPreferredSetting()) {
        case SettingsHelper.AppSetting.SETTING_NONE:
          checkedRadio = R.id.radio1;
          break;
        case SettingsHelper.AppSetting.SETTING_STOCK:
          checkedRadio = R.id.radio2;
          break;
        case SettingsHelper.AppSetting.SETTING_TITLE:
          checkedRadio = R.id.radio3;
          break;
        case SettingsHelper.AppSetting.SETTING_SHORTSUMMARY:
          checkedRadio = R.id.radio4;
          break;
        case SettingsHelper.AppSetting.SETTING_COUNTUPDATES:
          checkedRadio = R.id.radio5;
          break;
      }
      holder.radioG.check(checkedRadio);

      holder.radioG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
          switch (checkedId) {
            case R.id.radio0:
              item.app.setPreferredSetting(SettingsHelper.AppSetting.SETTING_AUTO);
              if (!isWhitelist /* = isBlacklist */) {
                holder.value.setText(item.app.toShortString());
                holder.value.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
                mSettingsHelper.removeListItem(item.app.getPackageName());
                return;
              }
              break;
            case R.id.radio1:
              item.app.setPreferredSetting(SettingsHelper.AppSetting.SETTING_NONE);
              break;
            case R.id.radio2:
              item.app.setPreferredSetting(SettingsHelper.AppSetting.SETTING_STOCK);
              if (isWhitelist) {
                holder.value.setText(item.app.toShortString());
                holder.value.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
                mSettingsHelper.removeListItem(item.app.getPackageName());
                return;
              }
              break;
            case R.id.radio3:
              item.app.setPreferredSetting(SettingsHelper.AppSetting.SETTING_TITLE);
              break;
            case R.id.radio4:
              item.app.setPreferredSetting(SettingsHelper.AppSetting.SETTING_SHORTSUMMARY);
              break;
            case R.id.radio5:
              item.app.setPreferredSetting(SettingsHelper.AppSetting.SETTING_COUNTUPDATES);
              break;
          }
          mSettingsHelper.alterListItem(item.app);
          holder.value.setText(item.app.toShortString());
          holder.value.setTypeface(Typeface.DEFAULT_BOLD);
        }
      });

      return view;
    }

    private ValueAnimator slideAnimator(int start, int end, final RadioGroup rg) {
      ValueAnimator animator = ValueAnimator.ofInt(start, end);
      animator.setDuration(this.getContext().getResources()
          .getInteger(android.R.integer.config_shortAnimTime));
      animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
          // Update Height
          int value = (Integer) valueAnimator.getAnimatedValue();
          LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) rg.getLayoutParams();
          layoutParams.height = value;
          rg.setLayoutParams(layoutParams);
        }
      });
      return animator;
    }
  }
}
