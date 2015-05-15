
package com.woalk.apps.xposed.notifcount;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import de.robv.android.xposed.XSharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by bbukowski on 07.08.14. Maintained by woalk since 2015/05/05.
 */
public class SettingsHelper {

  public static final String PACKAGE_NAME = "com.woalk.apps.xposed.notifcount";
  private static final String PREFS = PACKAGE_NAME + "_preferences";

  private static final String NOTIFICATION_FILTER_LIST = "apps_list";
  private static final String NOTIFICATION_NUMBER_SIZE = "number_size";
  private static final String PREFERENCES_VERSION = "ver";

  private XSharedPreferences mXSharedPreferences;
  private SharedPreferences mSharedPreferences;
  private List<String> mListItems;

  // Called from module's classes.
  public SettingsHelper() {
    mXSharedPreferences = new XSharedPreferences(PACKAGE_NAME, PREFS);
    reload();
  }

  // Called from activities.
  @SuppressLint("WorldReadableFiles")
  @SuppressWarnings("deprecation")
  public SettingsHelper(Context context) {
    mSharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_WORLD_READABLE);
    reload();
  }

  public void reload() {
    if (mXSharedPreferences != null)
      mXSharedPreferences.reload();
    mListItems = getListItems();
  }

  public List<String> getListItems() {
    List<String> set = new ArrayList<String>();
    if (mSharedPreferences != null)
      set.addAll(mSharedPreferences.getStringSet(NOTIFICATION_FILTER_LIST, new HashSet<String>()));
    else if (mXSharedPreferences != null)
      set.addAll(mXSharedPreferences.getStringSet(NOTIFICATION_FILTER_LIST, new HashSet<String>()));
    return set;
  }

  public void alterListItem(AppSetting setting) {
    int listed = getListedIndex(setting.getPackageName());
    if (listed == -1) {
      mListItems.add(setting.toString());
    } else {
      mListItems.remove(listed);
      mListItems.add(setting.toString());
    }
    SharedPreferences.Editor prefEditor = mSharedPreferences.edit();
    HashSet<String> set = new HashSet<>();
    set.addAll(mListItems);
    prefEditor.putStringSet(NOTIFICATION_FILTER_LIST, set);
    prefEditor.apply();
  }

  public void removeListItem(String packageName) {
    SharedPreferences.Editor prefEditor = mSharedPreferences.edit();
    mListItems.remove(getListedIndex(packageName));
    HashSet<String> set = new HashSet<>();
    set.addAll(mListItems);
    prefEditor.putStringSet(NOTIFICATION_FILTER_LIST, set);
    prefEditor.apply();
  }

  public int getListedIndex(final String packageName) {
    return mListItems.indexOf(new Object() {
      @Override
      public boolean equals(Object other) {
        return ((String) other).contains(packageName + "==");
      }
    });
  }

  public AppSetting getSetting(String packageName) {
    int listedI = getListedIndex(packageName);
    if (listedI == -1)
      return null;
    String pref = mListItems.get(listedI);
    int i = pref.indexOf("==");
    return new AppSetting(pref.substring(0, i), Integer.valueOf(pref.substring(i + 2)));
  }

  public int getNumberSize() {
    int number = 0;
    if (mSharedPreferences != null)
      number = Integer.valueOf(mSharedPreferences.getString(NOTIFICATION_NUMBER_SIZE, "0"));
    else if (mXSharedPreferences != null)
      number = Integer.valueOf(mXSharedPreferences.getString(NOTIFICATION_NUMBER_SIZE, "0"));
    return number;
  }

  public int getPreferenceVersion() {
    int number = 0;
    if (mSharedPreferences != null)
      number = mSharedPreferences.getInt(PREFERENCES_VERSION, 0);
    return number;
  }

  public void setPreferenceVersion(int ver) {
    if (mSharedPreferences != null) {
      mSharedPreferences.edit().putInt(PREFERENCES_VERSION, ver).apply();
    }
  }

  public List<String> getCachedList() {
    return mListItems;
  }

  public void clearLists() {
    mSharedPreferences.edit()
        .remove(NOTIFICATION_FILTER_LIST)
        .remove("apps_list_extract")
        .apply();
  }

  public static class AppSetting {
    public static final int SETTING_AUTO = 0;
    public static final int SETTING_NONE = 1;
    public static final int SETTING_STOCK = 2;
    public static final int SETTING_TITLE = 3;
    public static final int SETTING_CONTENT = 4;
    public static final int SETTING_SHORTSUMMARY = 5;
    public static final int SETTING_COUNTUPDATES = 6;

    private String mPackageName;
    private int mPreferredSetting;

    public AppSetting(String packageName) {
      setPackageName(packageName);
    }

    public AppSetting(String packageName, int setting) {
      setPackageName(packageName);
      setPreferredSetting(setting);
    }

    public String getPackageName() {
      return mPackageName;
    }

    public void setPackageName(String pkg) {
      this.mPackageName = pkg;
    }

    public int getPreferredSetting() {
      return mPreferredSetting;
    }

    public void setPreferredSetting(int mPreferredSetting) {
      this.mPreferredSetting = mPreferredSetting;
    }

    @Override
    public String toString() {
      return this.getPackageName() + "==" + String.valueOf(this.getPreferredSetting());
    }

    public String toShortString() {
      switch (getPreferredSetting()) {
        case SettingsHelper.AppSetting.SETTING_AUTO:
          return "A";
        case SettingsHelper.AppSetting.SETTING_CONTENT:
          return "C";
        case SettingsHelper.AppSetting.SETTING_COUNTUPDATES:
          return "CU";
        case SettingsHelper.AppSetting.SETTING_NONE:
          return "N";
        case SettingsHelper.AppSetting.SETTING_SHORTSUMMARY:
          return "S";
        case SettingsHelper.AppSetting.SETTING_STOCK:
          return "Sy";
        case SettingsHelper.AppSetting.SETTING_TITLE:
          return "T";
        default:
          return "?";
      }
    }

  }

}
