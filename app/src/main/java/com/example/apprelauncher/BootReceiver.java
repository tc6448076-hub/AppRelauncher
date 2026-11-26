package com.example.apprelauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Restarts the timer after the TV starts up (or after the app is updated). */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        Prefs prefs = new Prefs(context);
        if (prefs.enabled() && prefs.autoStart() && !prefs.pkg().isEmpty()) {
            WatchdogService.start(context, true, true);
        }
    }
}
