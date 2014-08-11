
package de.bbukowski.notifcount;

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
import android.service.notification.StatusBarNotification;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.HashMap;
import java.util.List;

public class XposedMod implements IXposedHookLoadPackage,
    IXposedHookZygoteInit, IXposedHookInitPackageResources {

  private static final String CLASS_STATUSBARICONVIEW = "com.android.systemui.statusbar.StatusBarIconView";
  private static final String CLASS_STATUSBARICON = "com.android.internal.statusbar.StatusBarIcon";
  private static final String CLASS_STATUSBARMANAGERSERVICE = "com.android.server.StatusBarManagerService";
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

    if (Build.VERSION.SDK_INT >= 16) {
      hookNotificationInboxStyle();
    }

    if (Build.VERSION.SDK_INT >= 18) {
      hookAutoIncrementMethodsApi18();
    } else {
      hookAutoIncrementMethodsApi15();
    }
  }

  @Override
  public void handleInitPackageResources(
      XC_InitPackageResources.InitPackageResourcesParam resparam)
      throws Throwable {
    if (!resparam.packageName.equals("com.android.systemui"))
      return;

    mModRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);
    mRes = resparam.res;
    mRes.setReplacement("com.android.systemui", "drawable", "notification_number_text_color",
        mModRes.fwd(R.drawable.notification_number_text_color));
    mRes.setReplacement("com.android.systemui", "drawable", "ic_notification_overlay",
        mModRes.fwd(R.drawable.ic_notification_overlay));
  }

  @Override
  public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam)
      throws Throwable {

    if (!"com.android.systemui".equals(lpparam.packageName))
      return;

    findAndHookConstructor(CLASS_STATUSBARICONVIEW, lpparam.classLoader,
        Context.class, String.class, Notification.class,
        new XC_MethodHook() {

          @Override
          protected void afterHookedMethod(MethodHookParam param)
              throws Throwable {
            Context context = (Context) param.args[0];
            final Resources res = context.getResources();
            final float densityMultiplier = res.getDisplayMetrics().density;
            final float scaledPx = 8 * densityMultiplier;

            Paint mNumberPain = (Paint) XposedHelpers
                .getObjectField(param.thisObject, "mNumberPain");
            mNumberPain.setTypeface(Typeface.DEFAULT_BOLD);
            mNumberPain.setTextSize(scaledPx);
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

            mRes.setReplacement("com.android.systemui", "bool",
                "config_statusBarShowNumber",
                number > 1);
          }
        });
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
              if (mSettingsHelper.isListed(sbn.getPackageName())) {
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
}
