
package de.bbukowski.notifcount;

import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.app.Notification;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.content.res.XResources;
import android.graphics.Paint;
import android.graphics.Typeface;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class NotifCount implements IXposedHookLoadPackage,
        IXposedHookZygoteInit, IXposedHookInitPackageResources {

    private static final String CLASS_STATUSBARICONVIEW = "com.android.systemui.statusbar.StatusBarIconView";
    private static final String CLASS_STATUSBARICON = "com.android.internal.statusbar.StatusBarIcon";

    private static String MODULE_PATH = null;
    private XResources mRes;
    private XModuleResources mModRes;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
    }

    @Override
    public void handleInitPackageResources(
            XC_InitPackageResources.InitPackageResourcesParam resparam)
            throws Throwable {
        if (!resparam.packageName.equals("com.android.systemui"))
            return;

        mModRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);
        mRes = resparam.res;
        mRes.setReplacement("com.android.systemui", "drawable",
                "notification_number_text_color",
                mModRes.fwd(R.drawable.notification_number_text_color));
        mRes.setReplacement("com.android.systemui", "drawable",
                "ic_notification_overlay",
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
                        if (number > 1) {
                            mRes.setReplacement("com.android.systemui", "bool",
                                    "config_statusBarShowNumber", Boolean.TRUE);
                        }
                    }
                });
    }
}
