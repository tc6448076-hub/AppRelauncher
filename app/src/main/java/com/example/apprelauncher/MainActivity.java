package com.example.apprelauncher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Settings screen. Fully usable with a TV remote (D-pad). */
public class MainActivity extends Activity {
    private Prefs prefs;
    private TextView tvStatus, tvApp, tvPerm;
    private EditText etInterval;
    private CheckBox cbFresh, cbLaunchOnStart, cbBoot, cbWake;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(48), dp(32), dp(48), dp(32));

        root.addView(text("App Relauncher", 28));

        tvStatus = text("", 20);
        root.addView(tvStatus);

        root.addView(space());
        root.addView(text("1. App to launch", 18));
        tvApp = text("", 16);
        root.addView(tvApp);
        root.addView(button("Choose app", new View.OnClickListener() {
            @Override public void onClick(View v) { chooseApp(); }
        }));

        root.addView(space());
        root.addView(text("2. Launch every (minutes)", 18));
        etInterval = new EditText(this);
        etInterval.setInputType(InputType.TYPE_CLASS_NUMBER);
        etInterval.setSingleLine(true);
        etInterval.setText(String.valueOf(prefs.intervalMin()));
        root.addView(etInterval, new LinearLayout.LayoutParams(dp(200), LinearLayout.LayoutParams.WRAP_CONTENT));

        root.addView(space());
        root.addView(text("3. Options", 18));
        cbLaunchOnStart = check("Launch the app straight away when started", prefs.launchOnStart());
        cbFresh = check("Restart the app fresh each time (closes its current screen)", prefs.fresh());
        cbBoot = check("Start this launcher automatically when the TV starts up", prefs.autoStart());
        cbWake = check("Launch the app when the TV is switched on / wakes from standby", prefs.wakeLaunch());
        root.addView(cbLaunchOnStart);
        root.addView(cbFresh);
        root.addView(cbBoot);
        root.addView(cbWake);

        root.addView(space());
        root.addView(text("4. Permission", 18));
        tvPerm = text("", 16);
        root.addView(tvPerm);
        root.addView(button("Allow display over other apps", new View.OnClickListener() {
            @Override public void onClick(View v) { openOverlaySettings(); }
        }));

        root.addView(space());
        root.addView(button("Save & Start", new View.OnClickListener() {
            @Override public void onClick(View v) { startWatchdog(); }
        }));
        root.addView(button("Stop", new View.OnClickListener() {
            @Override public void onClick(View v) { stopWatchdog(); }
        }));
        root.addView(button("Launch app now (test)", new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (prefs.pkg().isEmpty()) { toast("Choose an app first"); return; }
                if (!AppLauncher.launch(MainActivity.this)) toast("Could not launch the app");
            }
        }));

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        setContentView(sv);
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        if (prefs.enabled()) {
            tvStatus.setText("RUNNING: " + prefs.label() + " every " + prefs.intervalMin() + " min");
            tvStatus.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            tvStatus.setText("STOPPED");
            tvStatus.setTextColor(Color.parseColor("#C62828"));
        }
        tvApp.setText(prefs.pkg().isEmpty() ? "No app selected"
                : prefs.label() + "\n(" + prefs.pkg() + ")");
        tvPerm.setText(canOverlay() ? "Display over other apps: ALLOWED"
                : "Display over other apps: NOT ALLOWED (needed so the app can open from the background)");
    }

    private boolean canOverlay() {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this);
    }

    private void chooseApp() {
        final List<AppLauncher.AppInfo> apps = AppLauncher.listApps(this);
        if (apps.isEmpty()) { toast("No launchable apps found"); return; }
        CharSequence[] names = new CharSequence[apps.size()];
        for (int i = 0; i < names.length; i++) names[i] = apps.get(i).label;
        new AlertDialog.Builder(this)
                .setTitle("Choose app to launch")
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        AppLauncher.AppInfo a = apps.get(which);
                        prefs.setApp(a.pkg, a.label);
                        refresh();
                    }
                })
                .show();
    }

    private void openOverlaySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            new AlertDialog.Builder(this)
                    .setTitle("Not available on this TV")
                    .setMessage("This TV has no settings screen for this permission. "
                            + "Allow it from a computer with this command:\n\n"
                            + "adb shell appops set " + getPackageName() + " SYSTEM_ALERT_WINDOW allow")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void startWatchdog() {
        int minutes;
        try { minutes = Integer.parseInt(etInterval.getText().toString().trim()); }
        catch (Exception e) { minutes = 0; }
        if (minutes < 1) { toast("Enter 1 minute or more"); return; }
        if (prefs.pkg().isEmpty()) { toast("Choose an app first"); return; }
        prefs.setInterval(minutes);
        prefs.setFresh(cbFresh.isChecked());
        prefs.setLaunchOnStart(cbLaunchOnStart.isChecked());
        prefs.setAutoStart(cbBoot.isChecked());
        prefs.setWakeLaunch(cbWake.isChecked());
        prefs.setEnabled(true);
        WatchdogService.start(this, true);
        refresh();
        toast("Started");
        if (!canOverlay()) openOverlaySettings();
    }

    private void stopWatchdog() {
        prefs.setEnabled(false);
        WatchdogService.stop(this);
        refresh();
        toast("Stopped");
    }

    // ---- small UI helpers ----
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    private TextView text(String s, int sp) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        return t;
    }

    private View space() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(16)));
        return v;
    }

    private Button button(String s, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        return b;
    }

    private CheckBox check(String s, boolean on) {
        CheckBox c = new CheckBox(this);
        c.setText(s);
        c.setChecked(on);
        return c;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
