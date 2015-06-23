package com.woalk.apps.xposed.notifcount;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

public class AppListFragment extends ListFragment {

  private static SettingsHelper mSettingsHelper;
  private static AppList.AppListAdapter mAdapter;

  private boolean isWhitelist;
  private String currentQuery;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mSettingsHelper = new SettingsHelper(getActivity());

    isWhitelist = mSettingsHelper.isWhitelist();

    mAdapter = new AppList.AppListAdapter(getActivity(), mSettingsHelper, isWhitelist);
    setListAdapter(mAdapter);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    getListView().setFastScrollEnabled(true);
  }

  public boolean hasSearchQuery() {
    return currentQuery != null && !currentQuery.equals("");
  }

  public void search(String query) {
    if (!query.equals(this.currentQuery)) {
      this.currentQuery = query;
      ((ArrayAdapter) getListAdapter()).getFilter().filter(query);
    }
  }
}
