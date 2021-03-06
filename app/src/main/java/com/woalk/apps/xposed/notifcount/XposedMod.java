
package com.woalk.apps.xposed.notifcount;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.service.notification.NotificationListenerService.RankingMap;
import android.service.notification.StatusBarNotification;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.htc.preference.HtcPreference;
import com.woalk.apps.xposed.notifcount.SettingsHelper.AppSetting;

import java.util.HashMap;
import java.util.List;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

public class XposedMod implements IXposedHookLoadPackage,
    IXposedHookZygoteInit, IXposedHookInitPackageResources {

  private static final String PKG_SYSTEMUI = "com.android.systemui";
  private static final String CLASS_STATUSBARICONVIEW = PKG_SYSTEMUI
      + ".statusbar.StatusBarIconView";
  private static final String CLASS_STATUSBARICON = "com.android.internal.statusbar.StatusBarIcon";
  private static final String CLASS_STATUSBARMANAGERSERVICE = "com.android.server.StatusBarManagerService";
  private static final String CLASS_BASESTATUSBAR = PKG_SYSTEMUI + ".statusbar.BaseStatusBar";
  private static final String CLASS_COMMANDQUEUE = PKG_SYSTEMUI + ".statusbar.CommandQueue";
  private static final String CLASS_PHONESTATUSBAR = PKG_SYSTEMUI
      + ".statusbar.phone.PhoneStatusBar";
  private static final String CLASS_STATUSBARNOTIFICATION_API15 = "com.android.internal.statusbar.StatusBarNotification";
  private static final String PKG_SETTINGS = "com.android.settings";
  private static final String CLASS_APPNOTIFICATIONSETTINGS_API21 = PKG_SETTINGS + ".notification.AppNotificationSettings";
  private static final String PKG_HTCPREFERENCE = "com.htc.preference";
  private static final String CLASS_NOTIFICATION_MANAGER = "android.app.NotificationManager";

  private static SettingsHelper mSettingsHelper;
  private static String MODULE_PATH = null;
  private XResources mRes;
  private XModuleResources mModRes;

  @Override
  public void initZygote(StartupParam startupParam) throws Throwable {
    MODULE_PATH = startupParam.modulePath;

    if (mSettingsHelper == null) {
      mSettingsHelper = new SettingsHelper();
    }

    // if (Build.VERSION.SDK_INT >= 16) {
    // hookNotificationInboxStyle();
    // }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
        hookAutoDecide_update_api18();
      else
        hookAutoDecide_update_api15();
    }
  }

  @Override
  public void handleInitPackageResources(
      XC_InitPackageResources.InitPackageResourcesParam resparam)
      throws Throwable {
    if (!resparam.packageName.equals(PKG_SYSTEMUI))
      return;

    mModRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);
    mRes = resparam.res;
    mRes.setReplacement(PKG_SYSTEMUI, "drawable", "notification_number_text_color",
          mModRes.fwd(R.drawable.notification_number_text_color));

    // for hookSystemIntegration
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      mSettingsHelper.reload();
      if (mSettingsHelper.getShouldDoSystemIntegration()) {
        mRes.setReplacement(PKG_SYSTEMUI, "menu", "notification_popup_menu",
              mModRes.fwd(R.menu.notification_popup_menu));
      }
    }
  }

  @Override
  public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam)
      throws Throwable {
    if (PKG_SETTINGS.equals(lpparam.packageName) &&
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      mSettingsHelper.reload();
      if (mSettingsHelper.getShouldDoSystemIntegration()) {
        hookSystemIntegration_api21(lpparam.classLoader);
      }
    }

    if (mSettingsHelper.getShouldAddAlternatePrivate() &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      hookAlternatePrivate_api21(lpparam.classLoader);
    }

    if (!PKG_SYSTEMUI.equals(lpparam.packageName))
      return;

    findAndHookConstructor(CLASS_STATUSBARICONVIEW, lpparam.classLoader,
        Context.class, String.class, Notification.class,
        new XC_MethodHook() {

          @Override
          protected void afterHookedMethod(MethodHookParam param)
              throws Throwable {
            Context context = (Context) param.args[0];
            final Resources res = context.getResources();
            final float numberSize = mSettingsHelper.getNumberSize();
            final int numberShape = mSettingsHelper.getNumberBadgeShape();
            final float borderWidth = mSettingsHelper.getBadgeBorderWidth();
            final int numberAlpha = mSettingsHelper.getBadgeAlpha();

            Paint mNumberPain = (Paint) XposedHelpers
                .getObjectField(param.thisObject, "mNumberPain");
            mNumberPain.setTypeface(Typeface.DEFAULT_BOLD);
            mNumberPain.setTextSize(toPx(numberSize, res));
            mNumberPain.setColor(mSettingsHelper.getNumberColor());

            mRes.setReplacement(PKG_SYSTEMUI, "drawable", "ic_notification_overlay",
                  new XResources.DrawableLoader() {
                    @Override
                    public Drawable newDrawable(XResources xResources, int i) throws Throwable {
                      Shape shape1; Shape shape2;
                      float cornersPx = toPx((numberSize + 2) / 3, xResources);
                      switch (numberShape) {
                        case SettingsHelper.NUMBER_SHAPE_OVAL:
                          shape1 = new OvalShape();
                          shape2 = new OvalShape();
                          break;
                        case SettingsHelper.NUMBER_SHAPE_RECTANGLE:
                          shape1 = new RectShape();
                          shape2 = new RectShape();
                          break;
                        case SettingsHelper.NUMBER_SHAPE_RECTANGULAR_CIRCLE:
                          cornersPx = toPx((numberSize + 2) / 2, xResources);
                        case SettingsHelper.NUMBER_SHAPE_ROUND_RECTANGLE:
                          float[] corners = new float[]{
                              cornersPx, cornersPx, cornersPx, cornersPx, cornersPx, cornersPx,
                              cornersPx, cornersPx
                          };
                          shape1 = new RoundRectShape(corners, null, null);
                          shape2 = new RoundRectShape(corners, null, null);
                          break;
                        default:
                          shape1 = new OvalShape();
                          shape2 = new OvalShape();
                      }

                      int px = (int) toPx(borderWidth, xResources);
                      ShapeDrawable a = new ShapeDrawable(shape1);
                      a.setPadding(px, px, px, px);
                      a.getPaint().setColor(mSettingsHelper.getBadgeBorderColor());
                      int px2 = (int) (toPx(1, xResources));
                      ShapeDrawable b = new ShapeDrawable(shape2);
                      b.setPadding(px2, px2, px2, px2);
                      b.getPaint().setColor(mSettingsHelper.getBadgeColor());
                      Drawable l[] = new Drawable[] { a, b };
                      LayerDrawable layer = new LayerDrawable(l);
                      layer.setAlpha(numberAlpha);
                      return layer;
                    }
                  });
          }
        });

    findAndHookMethod(CLASS_STATUSBARICONVIEW, lpparam.classLoader, "set",
            CLASS_STATUSBARICON, new XC_MethodHook() {

              @Override
              protected void beforeHookedMethod(MethodHookParam param)
                      throws Throwable {
                Object icon = param.args[0];
                int number = XposedHelpers.getIntField(icon, "number");

                mSettingsHelper.reload();

                mRes.setReplacement(PKG_SYSTEMUI, "bool",
                        "config_statusBarShowNumber",
                        number > 1);
              }
            });

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      mSettingsHelper.reload();
      if (mSettingsHelper.getShouldDoSystemIntegration()) {
        hookSystemIntegration_api16(lpparam.classLoader);
      }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      hookAutoDecide_all_api21(lpparam.classLoader);
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      hookAutoDecide_new_api18(lpparam.classLoader);
    } else {
      hookAutoDecide_new_api15(lpparam.classLoader);
    }
  }

  private void hookSystemIntegration_api16(ClassLoader loader) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      final Class<?> baseStatusBarClass = findClass(CLASS_BASESTATUSBAR, loader);
      final Class<?> commandQueueClass = findClass(CLASS_COMMANDQUEUE, loader);
      findAndHookMethod(baseStatusBarClass, "getNotificationLongClicker",
          new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(final MethodHookParam mhParam) throws Throwable {
                  /*
                  This method is a complete clone of the original Android method, with the
                  modification needed to make the wanted addition work.
                  The original code stayed the same between 4.1.1 and 4.4.4, so this should work
                  on all Android versions. However, Custom ROMs may have problems.

                  The code is available here:
                  https://github.com/android/platform_frameworks_base/blob/android-4.4.4_r2.0.1/packages/SystemUI/src/com/android/systemui/statusbar/BaseStatusBar.java#L388
                  https://github.com/android/platform_frameworks_base/blob/android-4.1.1_r1/packages/SystemUI/src/com/android/systemui/statusbar/BaseStatusBar.java#L308
                  It belongs to the AOSP and is licensed under Apache License 2.0
                    http://www.apache.org/licenses/LICENSE-2.0.
                  ('AS IS'.)
                   */
              return new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                  final String packageNameF = (String) v.getTag();
                  if (packageNameF == null) return false;
                  if (v.getWindowToken() == null) return false;
                  final Context mContext = (Context) getObjectField(mhParam.thisObject, "mContext");
                  PopupMenu mNotificationBlamePopup = new PopupMenu(mContext, v);
                  int id_menu = mContext.getResources().getIdentifier("notification_popup_menu",
                      "menu", PKG_SYSTEMUI);
                  mNotificationBlamePopup.getMenuInflater().inflate(id_menu,
                      mNotificationBlamePopup.getMenu());

                  final int id_inspect_item = mContext.getResources().getIdentifier(
                      "notification_inspect_item", "id", PKG_SYSTEMUI);
                  MenuItem inspectItem = mNotificationBlamePopup.getMenu()
                      .findItem(id_inspect_item);
                  int id_inspect_title = mContext.getResources().getIdentifier(
                      "status_bar_recent_inspect_item_title", "string", PKG_SYSTEMUI);
                  inspectItem.setTitle(id_inspect_title);

                  mNotificationBlamePopup.setOnMenuItemClickListener(new PopupMenu
                      .OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                      if (item.getItemId() == id_inspect_item) {
                        startApplicationDetailsActivity(packageNameF, mContext);
                      } else if (item.getItemId() == android.R.id.custom) {
                        startNotificationCountSettins(packageNameF, mContext);
                      } else {
                        return false;
                      }
                      String method_animateCollapse =
                            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) ?
                                  "animateCollapsePanels" : "animateCollapse";
                      try {
                        baseStatusBarClass.getMethod(method_animateCollapse, int.class)
                              .invoke(mhParam.thisObject, 0);
                      } catch (Throwable e) {
                        e.printStackTrace();
                      }
                      return true;
                    }
                  });
                  mNotificationBlamePopup.show();

                  setObjectField(mhParam.thisObject, "mNotificationBlamePopup",
                        mNotificationBlamePopup);

                  return true;
                }
              };
            }
          });
    }
  }

  private void hookSystemIntegration_api21(final ClassLoader loader) {
    final Class<?> appNotifClass = findClass(CLASS_APPNOTIFICATIONSETTINGS_API21, loader);
    findAndHookMethod(appNotifClass, "onActivityCreated", Bundle.class, new XC_MethodHook() {
      @Override
      protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        if (!(param.thisObject instanceof PreferenceFragment)) {
          hookSystemIntegration_api21_htc(loader, param);
          return;
        }
        final PreferenceFragment thisObj = (PreferenceFragment) param.thisObject;
        Preference pref = new Preference(thisObj.getActivity());
        Resources res = thisObj.getActivity().getPackageManager()
              .getResourcesForApplication(SettingsHelper.PACKAGE_NAME);
        pref.setTitle(res.getText(R.string.single_app_menu_item_title));
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
          @Override
          public boolean onPreferenceClick(Preference preference) {
            startNotificationCountSettins(thisObj.getActivity().getIntent().getStringExtra
                  ("app_package"), thisObj.getActivity());
            return true;
          }
        });
        thisObj.getPreferenceScreen().addPreference(pref);
      }
    });
  }

  private void hookSystemIntegration_api21_htc(ClassLoader loader,
                                               XC_MethodHook.MethodHookParam param)
          throws PackageManager.NameNotFoundException {
    XposedBridge.log("HTC hook called!");
    final Fragment thisObj = (Fragment) param.thisObject;
    Object pref = XposedHelpers.newInstance(
            findClass(PKG_HTCPREFERENCE + ".HtcPreference", loader), thisObj.getActivity());
    Resources res = thisObj.getActivity().getPackageManager()
            .getResourcesForApplication(SettingsHelper.PACKAGE_NAME);
    XposedHelpers.callMethod(pref, "setTitle",
            res.getText(R.string.single_app_menu_item_title));
    XposedHelpers.callMethod(pref, "setOnPreferenceClickListener",
            new HtcPreference.OnPreferenceClickListener() {
              @Override
              public boolean onPreferenceClick(HtcPreference preference) {
                startNotificationCountSettins(thisObj.getActivity().getIntent().getStringExtra
                        ("app_package"), thisObj.getActivity());
                return true;
              }
            });
    XposedHelpers.callMethod(XposedHelpers.callMethod(thisObj, "getPreferenceScreen"),
            "addPreference", pref);
  }

  @TargetApi(21)
  private void hookAlternatePrivate_api21(ClassLoader loader) {
    XposedHelpers.findAndHookMethod(CLASS_NOTIFICATION_MANAGER, loader, "notify", String.class,
            int.class, Notification.class, new XC_MethodHook() {
              @Override
              protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Context context = (Context) XposedHelpers.getObjectField(param.thisObject,
                        "mContext");
                Resources res = context.getPackageManager()
                        .getResourcesForApplication(SettingsHelper.PACKAGE_NAME);
                Notification notification = (Notification) param.args[2];

                mSettingsHelper.reload();
                autoApplyNumber(notification, mSettingsHelper.getSetting(context.getPackageName()));

                if (notification.number > 0 && notification.publicVersion == null) {
                  notification.publicVersion = new Notification.Builder(context)
                          .setContentTitle(context.getPackageManager().getApplicationLabel(
                                  context.getApplicationInfo()))
                          .setContentText(res.getString(R.string.alternate_private_text,
                                  notification.number))
                          .setSmallIcon(notification.icon)
                          .setColor(notification.color)
                          .setVisibility(Notification.VISIBILITY_PUBLIC)
                          .build();
                }
              }
            });
  }

  /*
  Method copied from AOSP source.
  See the big comment block above in hookSystemIntegration for the origin of this method.
  (Few lines above the referenced method in the original code file)
   */
  private void startApplicationDetailsActivity(String packageName, Context context) {
    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null));
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(intent);
  }

  private void startNotificationCountSettins(String packageName, Context context) {
    Intent intent = new Intent(SingleAppActivity.INTENT_ACTION);
    intent.setPackage(SettingsHelper.PACKAGE_NAME);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.putExtra(SingleAppActivity.INTENT_EXTRA_PACKAGE_NAME,
          packageName);
    context.startActivity(intent);
  }

  @TargetApi(16)
  private void hookNotificationInboxStyle() {
    XposedHelpers.findAndHookMethod(Notification.InboxStyle.class, "buildStyled",
            Notification.class, new XC_MethodHook() {

              @Override
              protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Notification n = (Notification) param.getResult();
                if (n.number == 0) {
                  List<?> mTexts = (List<?>) XposedHelpers.getObjectField(
                          param.thisObject, "mTexts");
                  n.number = mTexts.size();
                }
              }
            });
  }

  @TargetApi(21)
  private void hookAutoDecide_all_api21(ClassLoader loader) {
    Class<?> clazz = XposedHelpers.findClass(CLASS_BASESTATUSBAR, loader);
    XposedHelpers.findAndHookMethod(clazz, "updateNotification", StatusBarNotification.class,
        RankingMap.class, new XC_MethodHook() {

          @Override
          protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            StatusBarNotification sbn = (StatusBarNotification) param.args[0];


            final String key = sbn.getKey();
            boolean wasHeadsUp = (boolean) XposedHelpers.callMethod(param.thisObject,
                "isHeadsUp", key);
            Object oldEntry;
            if (wasHeadsUp) {
              Object mHeadsUpNotificationView = XposedHelpers.getObjectField(param.thisObject,
                      "mHeadsUpNotificationView");
              oldEntry = XposedHelpers.callMethod(mHeadsUpNotificationView, "getEntry");
            } else {
              Object mNotificationData = XposedHelpers.getObjectField(param.thisObject,
                      "mNotificationData");
              oldEntry = XposedHelpers.callMethod(mNotificationData, "get", key);
            }
            if (oldEntry == null) {
              return;
            }

            final StatusBarNotification oldSbn = (StatusBarNotification) XposedHelpers
                .getObjectField(oldEntry, "notification");

            mSettingsHelper.reload();

            if (!mSettingsHelper.getShouldAddAlternatePrivate()) {
              autoApplyNumber(sbn.getNotification(), oldSbn.getNotification(),
                      mSettingsHelper.getSetting(sbn.getPackageName()));
            }
          }
        });
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      hookAutoDecide_new_api23(loader);
    } else {
      hookAutoDecide_new_api21(loader);
    }
  }

  @TargetApi(21)
  private void hookAutoDecide_new_api21(ClassLoader loader) {
    Class<?> clazz2 = XposedHelpers.findClass(CLASS_PHONESTATUSBAR, loader);
    XposedHelpers.findAndHookMethod(clazz2, "addNotification", StatusBarNotification.class,
            RankingMap.class, new XC_MethodHook() {

              @Override
              protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                StatusBarNotification sbn = (StatusBarNotification) param.args[0];
                mSettingsHelper.reload();

                if (!mSettingsHelper.getShouldAddAlternatePrivate()) {
                  autoApplyNumber(sbn.getNotification(),
                          mSettingsHelper.getSetting(sbn.getPackageName()));
                }
              }
            });
  }

  @TargetApi(21)
  private void hookAutoDecide_new_api23(ClassLoader loader) {
    Class<?> clazz = XposedHelpers.findClass(CLASS_PHONESTATUSBAR, loader);
    Class<?> clazz_entry = XposedHelpers.findClass(
            "com.android.systemui.statusbar.NotificationData.Entry", loader);

    XposedHelpers.findAndHookMethod(clazz, "addNotification", StatusBarNotification.class,
            RankingMap.class, clazz_entry, new XC_MethodHook() {

              @Override
              protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                StatusBarNotification sbn = (StatusBarNotification) param.args[0];
                mSettingsHelper.reload();

                if (!mSettingsHelper.getShouldAddAlternatePrivate()) {
                  autoApplyNumber(sbn.getNotification(),
                          mSettingsHelper.getSetting(sbn.getPackageName()));
                }
              }
            });
  }

  @TargetApi(18)
  private void hookAutoDecide_update_api18() {
    Class<?> clazz = XposedHelpers.findClass(CLASS_STATUSBARMANAGERSERVICE, null);
    XposedHelpers.findAndHookMethod(clazz, "updateNotification", IBinder.class,
        StatusBarNotification.class, new XC_MethodHook() {

          @SuppressWarnings("unchecked")
          @Override
          protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            IBinder key = (IBinder) param.args[0];
            StatusBarNotification sbn = (StatusBarNotification) param.args[1];

            HashMap<IBinder, StatusBarNotification> mNotifications = (HashMap<IBinder,
                StatusBarNotification>) XposedHelpers
                .getObjectField(param.thisObject, "mNotifications");

            mSettingsHelper.reload();

            if (mNotifications.containsKey(key)) {
              StatusBarNotification oldSbn = mNotifications.get(key);
              autoApplyNumber(sbn.getNotification(), oldSbn.getNotification(),
                  mSettingsHelper.getSetting(sbn.getPackageName()));
            } else {
              autoApplyNumber(sbn.getNotification(),
                  mSettingsHelper.getSetting(sbn.getPackageName()));
            }
          }
        });
  }

  @TargetApi(18)
  private void hookAutoDecide_new_api18(ClassLoader loader) {
    Class<?> clazz = XposedHelpers.findClass(CLASS_PHONESTATUSBAR, loader);
    XposedHelpers.findAndHookMethod(clazz, "addNotification", IBinder.class,
        StatusBarNotification.class, new XC_MethodHook() {

          @Override
          protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            StatusBarNotification sbn = (StatusBarNotification) param.args[1];

            mSettingsHelper.reload();

            autoApplyNumber(sbn.getNotification(),
                    mSettingsHelper.getSetting(sbn.getPackageName()));
          }
        });
  }

  private void hookAutoDecide_update_api15() {
    Class<?> clazz = XposedHelpers.findClass(CLASS_STATUSBARMANAGERSERVICE, null);
    Class<?> clazzSbn = XposedHelpers.findClass(CLASS_STATUSBARNOTIFICATION_API15, null);

    XposedHelpers.findAndHookMethod(clazz, "updateNotification", IBinder.class,
        clazzSbn, new XC_MethodHook() {

          @SuppressWarnings("unchecked")
          @Override
          protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            IBinder key = (IBinder) param.args[0];
            Object sbn = param.args[1];

            Notification notification = (Notification) XposedHelpers.getObjectField(
                sbn, "notification");

            String pkg = (String) XposedHelpers.getObjectField(
                sbn, "pkg");
            HashMap<IBinder, ?> mNotifications = (HashMap<IBinder, ?>) XposedHelpers
                .getObjectField(param.thisObject, "mNotifications");

            mSettingsHelper.reload();

            if (mNotifications.containsKey(key)) {
              Object oldSbn = mNotifications.get(key);
              Notification oldNotification = (Notification) XposedHelpers
                  .getObjectField(oldSbn, "notification");
              autoApplyNumber(notification, oldNotification, mSettingsHelper.getSetting(pkg));
            } else {
              autoApplyNumber(notification, mSettingsHelper.getSetting(pkg));
            }
          }
        });
  }

  private void hookAutoDecide_new_api15(ClassLoader loader) {
    Class<?> clazz = XposedHelpers.findClass(CLASS_PHONESTATUSBAR, loader);
    Class<?> clazzSbn = XposedHelpers.findClass(CLASS_STATUSBARNOTIFICATION_API15, null);

    XposedHelpers.findAndHookMethod(clazz, "addNotification", IBinder.class,
        clazzSbn, new XC_MethodHook() {

          @Override
          protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            Object sbn = param.args[1];

            Notification notification = (Notification) XposedHelpers.getObjectField(
                    sbn, "notification");
            if (notification.number == 0) {
              String pkg = (String) XposedHelpers.getObjectField(
                  sbn, "pkg");

              mSettingsHelper.reload();

              autoApplyNumber(notification, mSettingsHelper.getSetting(pkg));
            }
          }
        });
  }

  private static void autoApplyNumber(Notification newNotif, Notification oldNotif,
      AppSetting setting) {
    // If no settings could be found, apply default settings
    if (setting == null)
      setting = new AppSetting(null, mSettingsHelper.isWhitelist() ? AppSetting.SETTING_STOCK :
              AppSetting.SETTING_AUTO);

    if (setting.getPreferredSetting() == AppSetting.SETTING_NONE) {
      // Remove notification number.
      newNotif.number = 0;
      return;
    }

    if (newNotif.number != 0 || setting.getPreferredSetting() == AppSetting.SETTING_STOCK)
      // Notification already has a number. Setting a number is not needed.
      return;

    // This only works on KitKat or higher.
    // Also, ignore this if the app should only count updates.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
        && setting.getPreferredSetting() != AppSetting.SETTING_COUNTUPDATES) {
      // If the number should be found in the content, extract it from there and nowhere else.
      if (setting.getPreferredSetting() == AppSetting.SETTING_CONTENT) {
        extractNumberFromContent(newNotif);
        return;
      }
      // Try to find a number in the title.
      // If SETTING_SHORTSUMMARY is set, jump directly to checking the summary
      // (ignore title).
      if (setting.getPreferredSetting() == AppSetting.SETTING_SHORTSUMMARY
          || !extractNumberFromTitle(newNotif)) {
        // If not found in the title, try to find in the summary.
        // If SETTING_TITLE is set, ignore checking for the summary.
        if (setting.getPreferredSetting() == AppSetting.SETTING_TITLE
            || extractNumberFromSummery(newNotif)
            || setting.getPreferredSetting() == AppSetting.SETTING_SHORTSUMMARY)
          return;
      } else
        return;
    }

    if (oldNotif != null) {
      // Everything before did not work, auto-increase on update.
      if (oldNotif.number == 0)
        newNotif.number = 2;
      else
        newNotif.number = oldNotif.number + 1;
    }
  }

  private static void autoApplyNumber(Notification notif, AppSetting setting) {
    autoApplyNumber(notif, null, setting);
  }

  @TargetApi(19)
  private static boolean extractNumberFromTitle(Notification notification) {
    String notification_text = notification.extras
        .getString(Notification.EXTRA_TITLE);
    return setNotificationNumberFromText(notification, notification_text);
  }

  @TargetApi(19)
  private static boolean extractNumberFromContent(Notification notification) {
    String notification_text = notification.extras.getString(Notification.EXTRA_TEXT);
    return setNotificationNumberFromText(notification, notification_text);
  }

  @TargetApi(19)
  private static boolean extractNumberFromSummery(Notification notification) {
    String notification_text = notification.extras
          .getString(Notification.EXTRA_SUMMARY_TEXT);
    return setNotificationNumberFromText(notification, notification_text);
  }

  private static boolean setNotificationNumberFromText(Notification notification, String text) {
    if (text != null) {
      int i = findFirstIntegerInString(text);
      if (i == 0)
        return false;
      else {
        notification.number = i;
        return true;
      }
    } else
      return false;
  }

  private static int findFirstIntegerInString(String str) {
    int i = 0;
    while (i < (str.length() - 1) && !Character.isDigit(str.charAt(i)))
      i++;
    int j = i;
    while (j < (str.length() - 1) && Character.isDigit(str.charAt(j)))
      j++;
    String intstr = str.substring(i, j);
    return !intstr.equals("") ? Integer.parseInt(intstr) : 0;
  }

  private float toPx(float dp, Resources res) {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics());
  }
}
