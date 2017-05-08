package com.charles.hijack;

import android.app.Activity;
import android.app.Application;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.util.List;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;


public class ActivityProtection {

    private Stack<Activity> activities = new Stack<>();
    private H mH = new H();
    private Application application;
    private HijackListener mHijackListener;
    private static ActivityProtection mInstance = new ActivityProtection();

    public static ActivityProtection getInstance() {
        return mInstance;
    }

    private ActivityProtection() {
    }

    public void init(Application application, HijackListener mHijackListener) {
        this.application = application;
        this.mHijackListener = mHijackListener;
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                activities.push(activity);
            }

            @Override
            public void onActivityStopped(Activity activity) {
                if (activities.peek() == activity) {
                    Message message = Message.obtain();
                    message.what = 0;
                    message.obj = System.currentTimeMillis();
                    mH.sendMessageDelayed(message, 1000);
                }
            }

            @Override
            public void onActivityResumed(Activity activity) {
                mH.removeMessages(0);
            }


            @Override
            public void onActivityDestroyed(Activity activity) {
                activities.remove(activity);
                mH.removeMessages(0);
            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }
        });

    }

    private class H extends Handler {
        @Override
        public void handleMessage(Message msg) {
            getForegroundApp((long) msg.obj);
        }
    }

    private void getForegroundApp(long stopTime) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            android.app.ActivityManager mActivityManager = (android.app.ActivityManager) application.getSystemService(Context.ACTIVITY_SERVICE);
            checkAppInfo(mActivityManager.getRunningTasks(1).get(0).topActivity.getPackageName());
        } else {
            UsageStatsManager mUsageStatsManager = (UsageStatsManager) application.getSystemService(Context.USAGE_STATS_SERVICE);
            List<UsageStats> stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, stopTime - 1000, stopTime + 1000);
            if (stats != null) {
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
                for (UsageStats usageStats : stats) {
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                }
                if (!mySortedMap.isEmpty()) {
                    checkAppInfo(mySortedMap.get(mySortedMap.lastKey()).getPackageName());
                }
            }
        }
    }


    private void checkAppInfo(String packageName) {
        try {
            ApplicationInfo applicationInfo = application.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            if ((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) <= 0) {
                if (mHijackListener != null)
                    mHijackListener.onHijackListener(applicationInfo);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public interface HijackListener {
        void onHijackListener(ApplicationInfo applicationInfo);
    }

}
