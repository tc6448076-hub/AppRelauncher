package com.example.apprelauncher;

import android.content.Context;
import android.content.SharedPreferences;

/** All user settings, stored on the device. */
class Prefs {
    private final SharedPreferences sp;

    Prefs(Context c) {
        sp = c.getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    String pkg()             { return sp.getString("pkg", ""); }
    String label()           { return sp.getString("label", ""); }
    int intervalMin()        { return Math.max(1, sp.getInt("interval", 15)); }
    long intervalMs()        { return intervalMin() * 60L * 1000L; }
    boolean fresh()          { return sp.getBoolean("fresh", false); }
    boolean launchOnStart()  { return sp.getBoolean("launchOnStart", true); }
    boolean autoStart()      { return sp.getBoolean("autoStart", true); }
    boolean wakeLaunch()     { return sp.getBoolean("wakeLaunch", true); }
    boolean enabled()        { return sp.getBoolean("enabled", false); }

    void setApp(String pkg, String label) {
        sp.edit().putString("pkg", pkg).putString("label", label).apply();
    }
    void setInterval(int minutes)     { sp.edit().putInt("interval", minutes).apply(); }
    void setFresh(boolean v)          { sp.edit().putBoolean("fresh", v).apply(); }
    void setLaunchOnStart(boolean v)  { sp.edit().putBoolean("launchOnStart", v).apply(); }
    void setAutoStart(boolean v)      { sp.edit().putBoolean("autoStart", v).apply(); }
    void setWakeLaunch(boolean v)     { sp.edit().putBoolean("wakeLaunch", v).apply(); }
    void setEnabled(boolean v)        { sp.edit().putBoolean("enabled", v).apply(); }
}
