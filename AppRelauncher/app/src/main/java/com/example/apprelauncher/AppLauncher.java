package com.example.apprelauncher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Finds installed apps and launches the chosen one. */
class AppLauncher {

    static class AppInfo {
        final String pkg;
        final String label;
        AppInfo(String pkg, String label) { this.pkg = pkg; this.label = label; }
    }

    /** Lists every app that has a normal or Android TV (leanback) launcher entry. */
    static List<AppInfo> listApps(Context c) {
        PackageManager pm = c.getPackageManager();
        List<AppInfo> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String[] categories = { Intent.CATEGORY_LAUNCHER, Intent.CATEGORY_LEANBACK_LAUNCHER };
        for (String cat : categories) {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for (ResolveInfo ri : pm.queryIntentActivities(i, 0)) {
                String p = ri.activityInfo.packageName;
                if (p.equals(c.getPackageName()) || !seen.add(p)) continue;
                out.add(new AppInfo(p, String.valueOf(ri.loadLabel(pm))));
            }
        }
        Collections.sort(out, new Comparator<AppInfo>() {
            @Override public int compare(AppInfo a, AppInfo b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        return out;
    }

    /** Launches the app saved in settings. Returns true if the launch request was sent. */
    static boolean launch(Context c) {
        Prefs prefs = new Prefs(c);
        String pkg = prefs.pkg();
        if (pkg.isEmpty()) return false;
        try {
            PackageManager pm = c.getPackageManager();
            Intent i = pm.getLaunchIntentForPackage(pkg);
            if (i == null) {
                i = pm.getLeanbackLaunchIntentForPackage(pkg);
            }
            if (i == null) {
                Log.w("AppRelauncher", "No launch intent for " + pkg);
                return false;
            }
            int flags = Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED;
            if (prefs.fresh()) flags |= Intent.FLAG_ACTIVITY_CLEAR_TASK;
            i.addFlags(flags);
            c.startActivity(i);
            return true;
        } catch (Exception e) {
            Log.e("AppRelauncher", "Launch failed", e);
            return false;
        }
    }
}
