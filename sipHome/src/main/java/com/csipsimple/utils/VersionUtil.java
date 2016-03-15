package com.csipsimple.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.csipsimple.R;

import java.util.Date;

/**
 * Created by kadyrovs on 15.03.2016.
 */
public class VersionUtil {

    public static String getVersionName(Context ctx) {
        try {
            PackageInfo e = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            return String.valueOf(e.versionName);
        } catch (PackageManager.NameNotFoundException var2) {
            return ctx.getString(R.string.unknown_version);
        }
    }


    public static String getVersionCode(Context ctx) {
        try {
            PackageInfo e = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            return String.valueOf(e.versionCode);
        } catch (PackageManager.NameNotFoundException var2) {
            return "<unknown code>";
        }
    }

    public static Date getFirstInstallTime(Context ctx) {
        try {
            PackageInfo e = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            return new Date(e.firstInstallTime);
        } catch (PackageManager.NameNotFoundException var2) {
            return null;
        }
    }

    public static Date getLastUpdateTime(Context ctx) {
        try {
            PackageInfo e = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            return new Date(e.lastUpdateTime);
        } catch (PackageManager.NameNotFoundException var2) {
            return null;
        }
    }
}
