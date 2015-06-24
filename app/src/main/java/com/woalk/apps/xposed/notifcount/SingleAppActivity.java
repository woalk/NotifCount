package com.woalk.apps.xposed.notifcount;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

/**
 * Created by woalk on 04/06/15.
 */
public class SingleAppActivity extends AppCompatActivity {

  public static final String INTENT_ACTION = SettingsHelper.PACKAGE_NAME + ".SINGLEAPP";
  public static final String INTENT_EXTRA_PACKAGE_NAME = SettingsHelper.PACKAGE_NAME +
		".EXTRA_PACKAGE_NAME";

  private SettingsHelper mSettingsHelper;
  private PackageManager pm;

  private SettingsHelper.AppSetting app;
  private boolean isWhitelist;
  private ApplicationInfo appInfo;

  private RadioGroup radioG;
  private TextView title;
  private TextView summary;
  private TextView value;
  private ImageView icon;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.activity_replaceable);

	View v = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE))
					.inflate(R.layout.whitelist_item, (ViewGroup) findViewById(android.R.id.content), false);
	((ViewGroup) findViewById(android.R.id.widget_frame)).addView(v);

	setSupportActionBar((Toolbar) findViewById(R.id.toolbar1));

	pm = getPackageManager();

	Intent intent = getIntent();
	if (!intent.getExtras().containsKey(INTENT_EXTRA_PACKAGE_NAME)) {
	  showErrorDialog(getText(R.string.single_app_error_misc));
	  return;
	}
	try {
	  appInfo = pm.getApplicationInfo(intent.getStringExtra(INTENT_EXTRA_PACKAGE_NAME), 0);
	} catch (Throwable e) {
	  e.printStackTrace();
	  showErrorDialog(getText(R.string.single_app_error_pkg_not_found));
	  return;
	}

	mSettingsHelper = new SettingsHelper(this);
	isWhitelist = mSettingsHelper.isWhitelist();

	app = mSettingsHelper.getSetting(appInfo.packageName);
	if (app == null) {
	  app = new SettingsHelper.AppSetting(appInfo.packageName);
	  app.setPreferredSetting(isWhitelist ? SettingsHelper.AppSetting.SETTING_STOCK :
			SettingsHelper.AppSetting.SETTING_AUTO);
	}

	findViewById(R.id.itemlayout).setClickable(false);

	title = (TextView) findViewById(android.R.id.text1);
	summary = (TextView) findViewById(android.R.id.text2);
	value = (TextView) findViewById(R.id.value);
	icon = (ImageView) findViewById(android.R.id.icon);

	title.setText(pm.getApplicationLabel(appInfo));
	summary.setText(app.getPackageName());
	icon.setImageDrawable(pm.getApplicationIcon(appInfo));

	value.setText(app.toShortString());
	if (app.getPreferredSetting() != (isWhitelist ? SettingsHelper.AppSetting.SETTING_STOCK :
		  SettingsHelper.AppSetting.SETTING_AUTO))
	  value.setTypeface(Typeface.DEFAULT_BOLD);
	else
	  value.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));

	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
	  findViewById(R.id.radio3).setVisibility(View.GONE);
	  findViewById(R.id.radio4).setVisibility(View.GONE);
	}

	radioG = (RadioGroup) findViewById(R.id.radioG);
	radioG.setVisibility(View.VISIBLE);
	int checkedRadio = isWhitelist ? R.id.radio2 : R.id.radio0;
	switch (app.getPreferredSetting()) {
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
	radioG.check(checkedRadio);
	radioG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
	  @Override
	  public void onCheckedChanged(RadioGroup group, int checkedId) {
		switch (checkedId) {
		  case R.id.radio0:
			app.setPreferredSetting(SettingsHelper.AppSetting.SETTING_AUTO);
			if (!isWhitelist /* = isBlacklist */) {
			  value.setText(app.toShortString());
			  value.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
			  mSettingsHelper.removeListItem(app.getPackageName());
			  return;
			}
			break;
		  case R.id.radio1:
			app.setPreferredSetting(SettingsHelper.AppSetting.SETTING_NONE);
			break;
		  case R.id.radio2:
			app.setPreferredSetting(SettingsHelper.AppSetting.SETTING_STOCK);
			if (isWhitelist) {
			  value.setText(app.toShortString());
			  value.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
			  mSettingsHelper.removeListItem(app.getPackageName());
			  return;
			}
			break;
		  case R.id.radio3:
			app.setPreferredSetting(SettingsHelper.AppSetting.SETTING_TITLE);
			break;
		  case R.id.radio4:
			app.setPreferredSetting(SettingsHelper.AppSetting.SETTING_SHORTSUMMARY);
			break;
		  case R.id.radio5:
			app.setPreferredSetting(SettingsHelper.AppSetting.SETTING_COUNTUPDATES);
			break;
		}
		mSettingsHelper.alterListItem(app);
		value.setText(app.toShortString());
		value.setTypeface(Typeface.DEFAULT_BOLD);
	  }
	});
  }

  protected void showErrorDialog(CharSequence message) {
	AlertDialog.Builder b = new AlertDialog.Builder(this)
		  .setTitle(R.string.single_app_error_title)
		  .setMessage(message);
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
	  b.setPositiveButton(android.R.string.ok, null)
	  	.setOnDismissListener(new DialogInterface.OnDismissListener() {
		  @Override
		  public void onDismiss(DialogInterface dialog) {
			SingleAppActivity.this.finish();
		  }
		});
	} else {
	  b.setCancelable(false);
	  b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
		  SingleAppActivity.this.finish();
		}
	  });
	}
	b.create().show();
  }
}
