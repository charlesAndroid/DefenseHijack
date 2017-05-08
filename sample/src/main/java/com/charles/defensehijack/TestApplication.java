package com.charles.defensehijack;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.charles.hijack.ActivityProtection;


public class TestApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ActivityProtection.getInstance().init(this, new ActivityProtection.HijackListener() {
            @Override
            public void onHijackListener(ApplicationInfo applicationInfo) {
                Log.e("hijack", applicationInfo.packageName);
            }
        });
    }
}
