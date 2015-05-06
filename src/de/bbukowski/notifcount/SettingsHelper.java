
package de.bbukowski.notifcount;

import android.content.Context;
import android.content.SharedPreferences;

import de.robv.android.xposed.XSharedPreferences;

import java.util.HashSet;

/**
 * Created by bbukowski on 07.08.14.
 */
public class SettingsHelper {

  private static final String PACKAGE_NAME = "de.bbukowski.notifcount";
  private static final String PREFS = PACKAGE_NAME + "_preferences";

  private static final String NOTIFICATION_FILTER_LIST = "apps_list";
  private static final String NOTIFICATION_NUMBER_SIZE = "number_size";

  private XSharedPreferences mXSharedPreferences;
  private SharedPreferences mSharedPreferences;
  private HashSet<String> mListItems;

  // Called from module's classes.
  public SettingsHelper() {
    mXSharedPreferences = new XSharedPreferences(PACKAGE_NAME, PREFS);
  }

  // Called from activities.
  @SuppressWarnings("deprecation")
  public SettingsHelper(Context context) {
    mSharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_WORLD_READABLE);
  }

  public void reload() {
    mXSharedPreferences.reload();
    mListItems = getListItems();
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
    prefEditor.apply();
  }

  public boolean isListed(String s) {
    if (mListItems == null)
      mListItems = getListItems();
    return mListItems.contains(s);
  }

  public int getNumberSize() {
    int number = 0;
    if (mSharedPreferences != null)
      number = Integer.valueOf(mSharedPreferences.getString(NOTIFICATION_NUMBER_SIZE, "0"));
    else if (mXSharedPreferences != null)
      number = Integer.valueOf(mXSharedPreferences.getString(NOTIFICATION_NUMBER_SIZE, "0"));
    return number;
  }

}
