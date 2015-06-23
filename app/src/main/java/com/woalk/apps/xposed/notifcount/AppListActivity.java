
package com.woalk.apps.xposed.notifcount;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

public class AppListActivity extends AppCompatActivity {

  private Menu mMenu;
  private Toolbar mToolbar;
  private AppListFragment mAppListFragment;
  private LinearLayout mSearchContainer;
  private EditText mSearchView;
  private ImageButton mSearchClearButton;

  private boolean mToolbarHomeButtonAnimating = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_replaceable_searchable);

    mToolbar = (Toolbar) findViewById(R.id.toolbar1);
    setSupportActionBar(mToolbar);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle(R.string.pref_apps_increase_onupdate_title);

    mAppListFragment = new AppListFragment();

    getFragmentManager().beginTransaction().replace(android.R.id.widget_frame, mAppListFragment)
          .commit();

    setupSearchView();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    mMenu = menu;
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.list_searchable, mMenu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        break;
      case R.id.search:
        displaySearchView(true);
        break;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onBackPressed() {
    if (mAppListFragment != null
          && (mAppListFragment.hasSearchQuery()
          || mSearchContainer.getVisibility() == View.VISIBLE)) {
      displaySearchView(false);
      return;
    }
    super.onBackPressed();
  }

  protected void setupSearchView() {
    mSearchContainer = (LinearLayout) mToolbar.findViewById(R.id.search_container);
    mSearchView = (EditText) mToolbar.findViewById(R.id.search_input);
    mSearchClearButton = (ImageButton) mToolbar.findViewById(R.id.search_clear);
    mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (!mToolbarHomeButtonAnimating) {
          if (mAppListFragment != null
                && (mAppListFragment.hasSearchQuery()
                || mSearchContainer.getVisibility() == View.VISIBLE)) {
            displaySearchView(false);
            return;
          }
          onBackPressed();
        }
      }
    });
    mSearchView.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        mAppListFragment.search(s.toString());
      }

      @Override
      public void afterTextChanged(Editable s) {
      }
    });
    mSearchClearButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mSearchView.setText("");
      }
    });
    View.OnLongClickListener imgButtonLongClicker = new View.OnLongClickListener() {
      @Override
      public boolean onLongClick(View v) {
        String contentDesc = v.getContentDescription().toString();
        if (!TextUtils.isEmpty(contentDesc))
        {
          int[] pos = new int[2];
          v.getLocationInWindow(pos);

          Toast t = Toast.makeText(AppListActivity.this, contentDesc, Toast.LENGTH_SHORT);
          t.setGravity(Gravity.TOP | Gravity.LEFT,
                pos[0], pos[1] + (int) AppList.toPx(28, getResources()));
          t.show();
        }
        return true;
      }
    };
    mSearchClearButton.setOnLongClickListener(imgButtonLongClicker);
  }


  public void displaySearchView(boolean visible) {
    if (visible) {
      // Hide search button, display EditText
      mMenu.findItem(R.id.search).setVisible(false);
      mSearchContainer.setVisibility(View.VISIBLE);

      // Shift focus to the search EditText
      mSearchView.requestFocus();

      // Pop up the soft keyboard
      new Handler().postDelayed(new Runnable() {
        public void run() {
          mSearchView.dispatchTouchEvent(MotionEvent.obtain(SystemClock
                      .uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN,
                0, 0, 0));
          mSearchView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
        }
      }, 200);
    } else {
      // Hide the EditText and put the search button back on the Toolbar.
      // This sometimes fails when it isn't postDelayed(), don't know why.
      mSearchView.postDelayed(new Runnable() {
        @Override
        public void run() {
          mSearchView.setText("");
          mSearchContainer.setVisibility(View.GONE);
          mMenu.findItem(R.id.search).setVisible(true);
        }
      }, 200);

      // Hide the keyboard because the search box has been hidden
      InputMethodManager imm = (InputMethodManager)
            getSystemService(Context.INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
    }
  }
}
