package com.woalk.apps.xposed.notifcount;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A container class for threads with the purpose to load a list of all apps, each one specifically
 * designed for one purpose.
 */
public class AppList {

  private AppList() {
  }

  public static class AppListObject implements Comparable<AppListObject>, Serializable,
        Parcelable {
    private static final long serialVersionUID = 0L;

    private String mPackageName;
    private String mAppTitle;

    private SettingsHelper.AppSetting mAppSetting;

    public AppListObject() {
    }

    protected AppListObject(Parcel from) {
      setPackageName(from.readString());
      setAppTitle(from.readString());
    }

    public AppListObject(String packageName, String appTitle) {
      setPackageName(packageName);
      setAppTitle(appTitle);
    }

    public AppListObject(ApplicationInfo app, String appTitle) {
      setPackageName(app.packageName);
      setAppTitle(appTitle);
    }

    public void setPackageName(String packageName) {
      this.mPackageName = packageName;
    }

    public String getPackageName() {
      return this.mPackageName;
    }

    public void setAppTitle(String mAppTitle) {
      this.mAppTitle = mAppTitle;
    }

    public String getAppTitle() {
      return mAppTitle;
    }

    public SettingsHelper.AppSetting getAppSetting() {
      return mAppSetting;
    }

    public void setAppSetting(SettingsHelper.AppSetting app) {
      this.mAppSetting = app;
    }

    @Override
    public int compareTo(@NonNull AppListObject another) {
      return this.getAppTitle().compareToIgnoreCase(another.getAppTitle());
    }

    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      } else if (!(o instanceof AppListObject)) {
        return false;
      } else {
        AppListObject another = (AppListObject) o;
        return (this.getPackageName() == null
              && another.getPackageName() == null) // both null => equal
              || (this.getPackageName() != null
              && another.getPackageName() != null) // if both not null:
              && this.getPackageName().equals( // are pkg names equal?
              another.getPackageName())
              && this.getAppTitle().equals(another.getAppTitle()) // are titles equal?
              && (this.getAppSetting() == null && another.getAppSetting() == null // both null?
              || (this.getAppSetting() != null && another.getAppSetting() != null // if both !null:
              && this.getAppSetting().getPreferredSetting()
              == another.getAppSetting().getPreferredSetting())); // is setting equal?
      }
    }

    @Override
    public String toString() {
      return getPackageName() + "|" + getAppTitle();
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
      out.writeUTF(getPackageName());
      out.writeUTF(getAppTitle());
      out.writeObject(getAppSetting());
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException,
          ClassNotFoundException {
      setPackageName(in.readUTF());
      setAppTitle(in.readUTF());
      setAppSetting((SettingsHelper.AppSetting) in.readObject());
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeString(getPackageName());
      dest.writeString(getAppTitle());
    }

    public static final Creator<AppListObject> CREATOR = new Creator<AppListObject>() {
      @Override
      public AppListObject createFromParcel(Parcel source) {
        return new AppListObject(source);
      }

      @Override
      public AppListObject[] newArray(int size) {
        return new AppListObject[size];
      }
    };
  }

  public static class AppListAdapter extends ArrayAdapter<AppListObject> {
    private AppList.Loader mLoader;
    private LayoutInflater mLayoutInflater;
    private PackageManager mPackageManager;
    private SettingsHelper mSettingsHelper;

    private List<AppListObject> mOriginalList;
    private Filter mFilter;

    protected final boolean isWhitelist;

    public AppListAdapter(Context context, SettingsHelper settings, boolean is_whitelist) {
      super(context, 0);
      this.isWhitelist = is_whitelist;

      mOriginalList = new ArrayList<>();

      mLoader = new Loader();
      mLayoutInflater = (LayoutInflater)
            getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      mPackageManager = getContext().getPackageManager();
      mSettingsHelper = settings;

      mLoader.execute(this);
    }

    @Override
    public void addAll(Collection<? extends AppListObject> collection) {
      this.mOriginalList.addAll(collection);
      super.addAll(collection);
    }

    public void addAllFilter(Collection<? extends AppListObject> collection) {
      super.addAll(collection);
    }

    @Override
    public void add(AppListObject object) {
      this.mOriginalList.add(object);
      super.add(object);
    }

    @Override
    public void clear() {
      this.mOriginalList.clear();
      super.clear();
    }

    public void clearFilter() {
      super.clear();
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {
      final ViewHolder vh;
      final AppListObject app = getItem(position);
      if (v == null) {
        v = mLayoutInflater.inflate(R.layout.whitelist_item, parent, false);
        vh = new ViewHolder();
        vh.title = (TextView) v.findViewById(android.R.id.text1);
        vh.summary = (TextView) v.findViewById(android.R.id.text2);
        vh.icon = (ImageView) v.findViewById(android.R.id.icon);
        vh.value = (TextView) v.findViewById(R.id.value);
        vh.itemlayout = (LinearLayout) v.findViewById(R.id.itemlayout);
        vh.radioG = (RadioGroup) v.findViewById(R.id.radioG);
        vh.radio3 = (RadioButton) v.findViewById(R.id.radio3);
        vh.radio4 = (RadioButton) v.findViewById(R.id.radio4);
        v.setTag(vh);
      } else {
        vh = (ViewHolder) v.getTag();
        vh.radioG.setVisibility(View.GONE);
      }

      vh.icon.setImageDrawable(null);
      if (vh.loaderTask != null) {
        vh.loaderTask.cancel(false);
      }
      vh.loaderTask = new ItemLoaderTask();
      vh.loaderTask.execute(vh, mPackageManager, app);

      vh.title.setText(app.getAppTitle());
      vh.summary.setText(app.getPackageName());

      vh.value.setText(app.getAppSetting().toShortString());
      if (app.getAppSetting().getPreferredSetting() != (isWhitelist ? SettingsHelper.AppSetting.SETTING_STOCK :
            SettingsHelper.AppSetting.SETTING_AUTO)) {
        vh.value.setTypeface(Typeface.DEFAULT_BOLD);
      } else {
        vh.value.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
      }

      vh.itemlayout.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          int vis = vh.radioG.getVisibility();
          if (vis == View.GONE) {
            vh.radioG.setVisibility(View.VISIBLE);

            final int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            final int heightSpec = View.MeasureSpec
                  .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            vh.radioG.measure(widthSpec, heightSpec);
            ValueAnimator mAnimator = slideAnimator(0, vh.radioG.getMeasuredHeight(),
                  vh.radioG);
            mAnimator.setInterpolator(new DecelerateInterpolator());
            mAnimator.start();
          } else {
            int finalHeight = vh.radioG.getHeight();
            ValueAnimator mAnimator = slideAnimator(finalHeight, 0, vh.radioG);
            mAnimator.addListener(new Animator.AnimatorListener() {
              @Override
              public void onAnimationStart(Animator animation) {
              }

              @Override
              public void onAnimationEnd(Animator animator) {
                // Height=0, but it set visibility to GONE
                vh.radioG.setVisibility(View.GONE);
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
        vh.radio3.setVisibility(View.GONE);
        vh.radio4.setVisibility(View.GONE);
      }

      vh.radioG.setOnCheckedChangeListener(null);

      int checkedRadio = isWhitelist ? R.id.radio2 : R.id.radio0;
      switch (app.getAppSetting().getPreferredSetting()) {
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
      vh.radioG.check(checkedRadio);

      vh.radioG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
          switch (checkedId) {
            case R.id.radio0:
              app.getAppSetting().setPreferredSetting(SettingsHelper.AppSetting.SETTING_AUTO);
              if (!isWhitelist /* = isBlacklist */) {
                vh.value.setText(app.getAppSetting().toShortString());
                vh.value.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
                mSettingsHelper.removeListItem(app.getAppSetting().getPackageName());
                return;
              }
              break;
            case R.id.radio1:
              app.getAppSetting().setPreferredSetting(SettingsHelper.AppSetting.SETTING_NONE);
              break;
            case R.id.radio2:
              app.getAppSetting().setPreferredSetting(SettingsHelper.AppSetting.SETTING_STOCK);
              if (isWhitelist) {
                vh.value.setText(app.getAppSetting().toShortString());
                vh.value.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
                mSettingsHelper.removeListItem(app.getAppSetting().getPackageName());
                return;
              }
              break;
            case R.id.radio3:
              app.getAppSetting().setPreferredSetting(SettingsHelper.AppSetting.SETTING_TITLE);
              break;
            case R.id.radio4:
              app.getAppSetting().setPreferredSetting(SettingsHelper.AppSetting.SETTING_SHORTSUMMARY);
              break;
            case R.id.radio5:
              app.getAppSetting().setPreferredSetting(SettingsHelper.AppSetting.SETTING_COUNTUPDATES);
              break;
          }
          mSettingsHelper.alterListItem(app.getAppSetting());
          vh.value.setText(app.getAppSetting().toShortString());
          vh.value.setTypeface(Typeface.DEFAULT_BOLD);
        }
      });

      return v;
    }

    @Override
    public Filter getFilter() {
      if (mFilter == null) {
        mFilter = new AppFilter();
      }
      return mFilter;
    }

    /**
     * Cancel execution of the {@link Loader} embedded into this adapter.
     */
    public void cancelLoading() {
      mLoader.cancel(false);
    }

    private static class ViewHolder {
      public TextView title;
      public TextView summary;
      public ImageView icon;
      public TextView value;
      public LinearLayout itemlayout;
      public RadioGroup radioG;
      public RadioButton radio3;
      public RadioButton radio4;
      public ItemLoaderTask loaderTask;
    }

    /**
     * Load data for a list item that takes longer.<br/>
     * <i>For this task:</i> App icons.<br/>
     * <br/>
     * <b>Arguments:</b><br/>
     * Pass following arguments to {@link #execute(Object[])} in the exact order:<br/>
     * 1. The {@link ViewHolder} of the current item, containing the {@link ImageView} to show
     * the icon on.<br/>
     * 2. A valid {@link PackageManager} instance.<br/>
     * 3. The {@link ApplicationInfo} object for the current item.
     */
    private static class ItemLoaderTask extends AsyncTask<Object, Void, Drawable> {
      private PackageManager pm;
      private ViewHolder vh;

      @Override
      protected Drawable doInBackground(Object... params) {
        vh = (ViewHolder) params[0];
        pm = (PackageManager) params[1];
        AppListObject app = (AppListObject) params[2];

        try {
          return pm.getApplicationIcon(app.getPackageName());
        } catch (Throwable e) {
          return null;
        }
      }

      @Override
      protected void onPostExecute(Drawable o) {
        if (vh != null && vh.icon != null) {
          vh.icon.setImageDrawable(o);
        }
      }
    }

    private class AppFilter extends Filter {

      @Override
      protected FilterResults performFiltering(CharSequence constraint) {
        constraint = constraint.toString().toLowerCase();
        FilterResults result = new FilterResults();
        if (constraint.toString().length() > 0) {
          List<AppListObject> filteredItems = new ArrayList<>();

          for (int i = 0; i < mOriginalList.size(); i++) {
            AppListObject o = mOriginalList.get(i);
            if (o.toString().toLowerCase().contains(constraint)) {
              filteredItems.add(o);
            }
          }
          result.count = filteredItems.size();
          result.values = filteredItems;
        } else {
          synchronized (this) {
            result.values = mOriginalList;
            result.count = mOriginalList.size();
          }
        }
        return result;
      }

      @SuppressWarnings("unchecked")
      @Override
      protected void publishResults(CharSequence constraint,
                                    FilterResults results) {
        clearFilter();
        addAllFilter((ArrayList<AppListObject>) results.values);
        notifyDataSetChanged();
        notifyDataSetInvalidated();
      }
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

  /**
   * Class to load a list of apps.
   * Call {@link #execute(Object[])} with an {@link AppListAdapter} as argument (only one is
   * possible) to let the {@code Loader} fill its {@link ListView} with the app list.
   */
  protected static class Loader extends AsyncTask<AppListAdapter, List<AppListObject>,
        List<AppListObject>> {
    private AppListAdapter mAppListAdapter;

    @Override
    protected List<AppListObject> doInBackground(AppListAdapter... params) {
      mAppListAdapter = params[0];

      List<AppListObject> cachedList = null;
      File f = null;
      try {
        f = new File(mAppListAdapter.getContext().getExternalCacheDir(), "apps");
        if (f.exists()) {
          FileInputStream fis = new FileInputStream(f);
          ObjectInputStream ois = new ObjectInputStream(fis);
          Object in = ois.readObject();
          if (in instanceof List) {
            //noinspection unchecked
            cachedList = (List<AppListObject>) in;
            //noinspection unchecked
            publishProgress(cachedList);
          }
          ois.close();
        }
      } catch (Throwable e) {
        e.printStackTrace();
      }

      PackageManager pm = mAppListAdapter.getContext().getPackageManager();

      List<ApplicationInfo> appList = pm.getInstalledApplications
            (PackageManager.GET_UNINSTALLED_PACKAGES);

      List<AppListObject> appListObjects = new ArrayList<>();

      for (int i = 0; i < appList.size(); i++) {
        if (isCancelled()) {
          return null;
        }
        ApplicationInfo app = appList.get(i);
        CharSequence appName = pm.getApplicationLabel(app);

        AppListObject appObject = new AppListObject(app, appName.toString());
        appObject.setAppSetting(new SettingsHelper.AppSetting(app.packageName));

        appObject.setAppSetting(mAppListAdapter.mSettingsHelper.getSetting(app.packageName));
        if (appObject.getAppSetting() == null) {
          appObject.setAppSetting(new SettingsHelper.AppSetting(app.packageName));
          appObject.getAppSetting().setPreferredSetting(mAppListAdapter.isWhitelist ?
                SettingsHelper.AppSetting.SETTING_STOCK : SettingsHelper.AppSetting.SETTING_AUTO);
        }
        appListObjects.add(appObject);
      }

      Collections.sort(appListObjects, new Comparator<AppListObject>() {
        @Override
        public int compare(AppListObject appInfo1, AppListObject appInfo2) {
          boolean app1 = mAppListAdapter.mSettingsHelper.getListedIndex(
                appInfo1.getPackageName()) != -1;
          boolean app2 = mAppListAdapter.mSettingsHelper.getListedIndex(
                appInfo2.getPackageName()) != -1;

          if (app1 == app2) {
            return appInfo1.getAppTitle().compareToIgnoreCase(appInfo2.getAppTitle());
          } else if (app1) {
            return -1;
          } else {
            return 1;
          }
        }
      });

      if (appListObjects.equals(cachedList)) {
        return null;
      }

      try {
        if (f == null) {
          f = new File(mAppListAdapter.getContext().getExternalCacheDir(), "apps");
        }
        FileOutputStream fos = new FileOutputStream(f);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(appListObjects);
        oos.close();
      } catch (Throwable e) {
        e.printStackTrace();
      }

      return appListObjects;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onProgressUpdate(List<AppListObject>... values) {
      mAppListAdapter.clear();
      mAppListAdapter.addAll(values[0]);
      mAppListAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPostExecute(List<AppListObject> list) {
      if (list != null) {
        mAppListAdapter.clear();
        mAppListAdapter.addAll(list);
        mAppListAdapter.notifyDataSetChanged();
      }
    }
  }
}
