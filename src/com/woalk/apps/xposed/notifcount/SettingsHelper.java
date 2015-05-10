
package com.woalk.apps.xposed.notifcount;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import de.robv.android.xposed.XSharedPreferences;

import java.util.HashSet;

/**
 * Created by bbukowski on 07.08.14. Maintained by woalk since 2015/05/05.
 */
public class SettingsHelper {

  public static final String PACKAGE_NAME = "com.woalk.apps.xposed.notifcount";
  private static final String PREFS = PACKAGE_NAME + "_preferences";

  private static final String NOTIFICATION_FILTER_LIST = "apps_list";
  private static final String NOTIFICATION_EXTRACT_FILTER_LIST = "apps_list_extract";
  private static final String NOTIFICATION_NUMBER_SIZE = "number_size";

  private XSharedPreferences mXSharedPreferences;
  private SharedPreferences mSharedPreferences;
  private HashSet<String> mListItems;
  private HashSet<String> mListItemsExtract;

  // Called from module's classes.
  public SettingsHelper() {
    mXSharedPreferences = new XSharedPreferences(PACKAGE_NAME, PREFS);
  }

  // Called from activities.
  @SuppressLint("WorldReadableFiles")
  @SuppressWarnings("deprecation")
  public SettingsHelper(Context context) {
    mSharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_WORLD_READABLE);
  }

  public void reload() {
    mXSharedPreferences.reload();
    mListItems = getListItems();
    mListItemsExtract = getListItemsExtract();
  }

  public HashSet<String> getListItems() {
    HashSet<String> set = new HashSet<String>();
    if (mSharedPreferences != null)
      set.addAll(mSharedPreferences.getStringSet(NOTIFICATION_FILTER_LIST, set));
    else if (mXSharedPreferences != null)
      set.addAll(mXSharedPreferences.getStringSet(NOTIFICATION_FILTER_LIST, set));
    return set;
  }

  public void addListItem(String listItem) {
    mListItems.add(listItem);
    SharedPreferences.Editor prefEditor = mSharedPreferences.edit();
    prefEditor.putStringSet(NOTIFICATION_FILTER_LIST, mListItems);
    prefEditor.apply();
  }

  public void removeListItem(String listItem) {
    SharedPreferences.Editor prefEditor = mSharedPreferences.edit();
    mListItems.remove(listItem);
    prefEditor.putStringSet(NOTIFICATION_FILTER_LIST, mListItems);
    mListItemsExtract.remove(listItem);
    prefEditor.putStringSet(NOTIFICATION_EXTRACT_FILTER_LIST, mListItemsExtract);
    prefEditor.apply();
  }

  public boolean isListed(String s) {
    if (mListItems == null)
      mListItems = getListItems();
    return mListItems.contains(s);
  }

  public HashSet<String> getListItemsExtract() {
    HashSet<String> set = new HashSet<String>();
    if (mSharedPreferences != null)
      set.addAll(mSharedPreferences.getStringSet(NOTIFICATION_EXTRACT_FILTER_LIST, set));
    else if (mXSharedPreferences != null)
      set.addAll(mXSharedPreferences.getStringSet(NOTIFICATION_EXTRACT_FILTER_LIST, set));
    return set;
  }

  public void addListItemExtract(String listItem) {
    mListItemsExtract.add(listItem);
    SharedPreferences.Editor prefEditor = mSharedPreferences.edit();
    prefEditor.putStringSet(NOTIFICATION_EXTRACT_FILTER_LIST, mListItemsExtract);
    prefEditor.apply();
  }

  public void removeListItemExtract(String listItem) {
    SharedPreferences.Editor prefEditor = mSharedPreferences.edit();
    mListItemsExtract.remove(listItem);
    prefEditor.putStringSet(NOTIFICATION_EXTRACT_FILTER_LIST, mListItemsExtract);
    prefEditor.apply();
  }

  public boolean isListedExtract(String s) {
    if (mListItemsExtract == null)
      mListItemsExtract = getListItemsExtract();
    return mListItemsExtract.contains(s);
  }

  public int getNumberSize() {
    int number = 0;
    if (mSharedPreferences != null)
      number = Integer.valueOf(mSharedPreferences.getString(NOTIFICATION_NUMBER_SIZE, "0"));
    else if (mXSharedPreferences != null)
      number = Integer.valueOf(mXSharedPreferences.getString(NOTIFICATION_NUMBER_SIZE, "0"));
    return number;
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
