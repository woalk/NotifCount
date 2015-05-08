
package com.woalk.apps.xposed.notifcount;

import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.IBinder;
import android.service.notification.NotificationListenerService.RankingMap;
import android.service.notification.StatusBarNotification;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.HashMap;
import java.util.List;

public class XposedMod implements IXposedHookLoadPackage,
    IXposedHookZygoteInit, IXposedHookInitPackageResources {

  private static final String PKG_SYSTEMUI = "com.android.systemui";
  private static final String CLASS_STATUSBARICONVIEW = PKG_SYSTEMUI
      + ".statusbar.StatusBarIconView";
  private static final String CLASS_STATUSBARICON = "com.android.internal.statusbar.StatusBarIcon";
  private static final String CLASS_STATUSBARMANAGERSERVICE = "com.android.server.StatusBarManagerService";
  private static final String CLASS_BASESTATUSBAR = PKG_SYSTEMUI + ".statusbar.BaseStatusBar";
  private static final String CLASS_STATUSBARNOTIFICATION_API15 = "com.android.internal.statusbar.StatusBarNotification";

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
        hookAutoIncrementMethodsApi18();
      else
        hookAutoIncrementMethodsApi15();
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
  }

  @Override
  public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam)
      throws Throwable {

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
            int numberSize = mSettingsHelper.getNumberSize();
            final float densityMultiplier = res.getDisplayMetrics().density;
            final float scaledPx = ((numberSize == 2) ? 7 : 9) * densityMultiplier;

            Paint mNumberPain = (Paint) XposedHelpers
                .getObjectField(param.thisObject, "mNumberPain");
            mNumberPain.setTypeface(Typeface.DEFAULT_BOLD);
            mNumberPain.setTextSize(scaledPx);

            int overlayId;
            switch (numberSize) {
              case 1:
                overlayId = R.drawable.ic_notification_overlay_transparent;
                break;
              case 2:
                overlayId = R.drawable.ic_notification_overlay_small;
                break;
              case 0:
                overlayId = R.drawable.ic_notification_overlay;
                break;
              default:
                overlayId = -1;
            }
            if (overlayId > -1)
              mRes.setReplacement(PKG_SYSTEMUI, "drawable", "ic_notification_overlay",
                  mModRes.fwd(overlayId));
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

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      hookAutoIncrementMethodsApi21(lpparam.classLoader);
    }
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
  private void hookAutoIncrementMethodsApi21(ClassLoader loader) {
    Class<?> clazz = XposedHelpers.findClass(CLASS_BASESTATUSBAR, loader);
    XposedHelpers.findAndHookMethod(clazz, "updateNotification", StatusBarNotification.class,
        RankingMap.class, new XC_MethodHook() {

          @Override
          protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            StatusBarNotification sbn = (StatusBarNotification) param.args[0];
            if (sbn.getNotification().number == 0) {
              mSettingsHelper.reload();
              boolean isListed = mSettingsHelper.isListed(sbn.getPackageName());
              boolean isExtract = mSettingsHelper.isListedExtract(sbn.getPackageName());
              if (isListed && !isExtract) {
                Object mNotificationData = XposedHelpers.getObjectField(param.thisObject,
                    "mNotificationData");
                Object mHeadsUpNotificationView = XposedHelpers.getObjectField(param.thisObject,
                    "mHeadsUpNotificationView");

                final String key = sbn.getKey();
                boolean wasHeadsUp = (boolean) XposedHelpers.callMethod(param.thisObject,
                    "isHeadsUp", key);
                Object oldEntry;
                if (wasHeadsUp) {
                  oldEntry = XposedHelpers.callMethod(mHeadsUpNotificationView, "getEntry");
                } else {
                  oldEntry = XposedHelpers.callMethod(mNotificationData, "get", key);
                }
                if (oldEntry == null) {
                  return;
                }

                final StatusBarNotification oldSbn = (StatusBarNotification) XposedHelpers
                    .getObjectField(oldEntry, "notification");
                if (oldSbn.getNotification().number == 0) {
                  sbn.getNotification().number = 2;
                } else {
                  sbn.getNotification().number = oldSbn.getNotification().number + 1;
                }
              } else if (isListed && isExtract) {
                try {
                  extractNumber(sbn.getNotification());
                } catch (Exception e) {
                  XposedBridge.log("Notification did not provide extractable number. Info: "
                      + sbn.toString());
                }
              }
            }
          }
        });
  }

  @TargetApi(18)
  private void hookAutoIncrementMethodsApi18() {
    Class<?> clazz = XposedHelpers.findClass(CLASS_STATUSBARMANAGERSERVICE, null);
    XposedHelpers.findAndHookMethod(clazz, "updateNotification", IBinder.class,
        StatusBarNotification.class, new XC_MethodHook() {

          @SuppressWarnings("unchecked")
          @Override
          protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            IBinder key = (IBinder) param.args[0];
            StatusBarNotification sbn = (StatusBarNotification) param.args[1];

            if (sbn.getNotification().number == 0) {
              mSettingsHelper.reload();
              boolean isListed = mSettingsHelper.isListed(sbn.getPackageName());
              boolean isExtract = mSettingsHelper.isListedExtract(sbn.getPackageName());
              if (isListed && !isExtract) {
                HashMap<IBinder, StatusBarNotification> mNotifications = (HashMap<IBinder, StatusBarNotification>) XposedHelpers
                    .getObjectField(param.thisObject, "mNotifications");

                if (mNotifications.containsKey(key)) {
                  StatusBarNotification oldSbn = mNotifications.get(key);
                  if (oldSbn.getNotification().number == 0) {
                    sbn.getNotification().number = 2;
                  } else {
                    sbn.getNotification().number = oldSbn.getNotification().number + 1;
                  }
                }
              } else if (isListed && isExtract) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                  try {
                    extractNumber(sbn.getNotification());
                  } catch (Exception e) {
                    XposedBridge.log("Notification did not provide extractable number. Info: "
                        + sbn.toString());
                  }
                } else {
                  XposedBridge
                      .log("Sorry, notification number extracting is not supported on versions lower than KITKAT.");
                }
              }
            }
          }
        });
  }

  private void hookAutoIncrementMethodsApi15() {
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
            if (notification.number == 0) {
              String pkg = (String) XposedHelpers.getObjectField(
                  sbn, "pkg");
              mSettingsHelper.reload();
              if (mSettingsHelper.isListed(pkg)) {
                HashMap<IBinder, ?> mNotifications = (HashMap<IBinder, ?>) XposedHelpers
                    .getObjectField(param.thisObject, "mNotifications");

                if (mNotifications.containsKey(key)) {
                  Object oldSbn = mNotifications.get(key);
                  Notification oldNotification = (Notification) XposedHelpers
                      .getObjectField(oldSbn, "notification");
                  if (oldNotification.number == 0) {
                    notification.number = 2;
                  } else {
                    notification.number = oldNotification.number + 1;
                  }
                }
              }
            }
          }
        });
  }

  @TargetApi(19)
  private static void extractNumber(Notification notification) throws NumberFormatException {
    String notification_text = notification.extras
        .getString(Notification.EXTRA_SUMMARY_TEXT);
    if (notification_text != null) {
      int i = findFirstIntegerInString(notification_text);
      notification.number = i;
    }
  }

  private static int findFirstIntegerInString(String str) throws NumberFormatException {
    int i = 0;
    while (!Character.isDigit(str.charAt(i)))
      i++;
    int j = i;
    while (Character.isDigit(str.charAt(j)))
      j++;
    return Integer.parseInt(str.substring(i, j));
  }
}
