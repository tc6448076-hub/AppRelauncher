package com.example.apprelauncher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

/**
 * Keeps running in the background. Launches the chosen app every N minutes,
 * and again whenever the TV is switched on / wakes from standby.
 */
public class WatchdogService extends Service {
    private static final String CHANNEL_ID = "relauncher";
    private static final int NOTIF_ID = 1;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Prefs prefs;
    private PowerManager.WakeLock wakeLock;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            AppLauncher.launch(WatchdogService.this);
            handler.postDelayed(this, prefs.intervalMs());
        }
    };

    // Fires when the TV screen turns on / wakes from standby / screensaver ends.
    private final BroadcastReceiver wakeReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!prefs.enabled() || !prefs.wakeLaunch()) return;
            handler.removeCallbacks(tick);
            handler.postDelayed(tick, 5000); // short delay so the TV is ready; timer restarts after
        }
    };

    static void start(Context c, boolean initial) {
        start(c, initial, false);
    }

    static void start(Context c, boolean initial, boolean fromBoot) {
        Intent i = new Intent(c, WatchdogService.class)
                .putExtra("initial", initial)
                .putExtra("boot", fromBoot);
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
    }

    static void stop(Context c) {
        c.stopService(new Intent(c, WatchdogService.class));
    }

    @Override public void onCreate() {
        super.onCreate();
        prefs = new Prefs(this);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AppRelauncher:timer");
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire();
        }
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_USER_PRESENT);
        f.addAction("android.intent.action.DREAMING_STOPPED");
        registerReceiver(wakeReceiver, f);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_ID, buildNotification());
        if (!prefs.enabled() || prefs.pkg().isEmpty()) {
            stopSelf();
            return START_NOT_STICKY;
        }
        handler.removeCallbacks(tick);
        boolean initial = intent != null && intent.getBooleanExtra("initial", false);
        boolean boot = intent != null && intent.getBooleanExtra("boot", false);
        if (initial && boot) handler.postDelayed(tick, 15000);   // let the TV finish starting up
        else if (initial && prefs.launchOnStart()) handler.post(tick);
        else handler.postDelayed(tick, prefs.intervalMs());
        return START_STICKY;
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(tick);
        try { unregisterReceiver(wakeReceiver); } catch (Exception ignored) { }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private Notification buildNotification() {
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, "App Relauncher", NotificationManager.IMPORTANCE_LOW));
            b = new Notification.Builder(this, CHANNEL_ID);
        } else {
            b = new Notification.Builder(this);
        }
        return b.setContentTitle("App Relauncher is running")
                .setContentText(prefs.label() + " every " + prefs.intervalMin() + " min")
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .setOngoing(true)
                .build();
    }
}
