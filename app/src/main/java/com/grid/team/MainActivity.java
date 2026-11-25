package com.grid.team;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.graphics.PorterDuff;
import android.widget.FrameLayout;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {    private TextView weatherText;
    private LocationManager locationManager;
    private static final int LOCATION_PERMISSION = 501;
    private static final int PICK_PHOTOS = 201;
    private final ArrayList<JSONObject> jobs = new ArrayList<>();
    private final ArrayList<String> draftPhotos = new ArrayList<>();
    private SharedPreferences storage;
    private LinearLayout layout;
    private int editingIndex = -1;
    private int buttonCount = 0;
    private String selectedTurbine = null;
    private Runnable backAction;
    private Runnable jobsReturn;
    private String language = "ru";
    private final int white = Color.WHITE;
    private final int glassFill = Color.argb(100, 20, 40, 55);
    private final int glassBorder = Color.argb(165, 255, 255, 255);


    private final android.os.Handler weatherHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private String weatherValue = "-- °C\n-- m/s";
    private boolean weatherActive, weatherBusy, weatherPermissionAsked;
    private LocationListener weatherListener;
    private final Runnable weatherTick = new Runnable() {
        @Override public void run() {
            if (!weatherActive) return;
            refreshWeather(false);
            weatherHandler.postDelayed(this, 600000L);
        }
    };
    @Override protected void onResume() {
        super.onResume(); weatherActive = true;
        weatherHandler.removeCallbacks(weatherTick); weatherHandler.post(weatherTick);
    }
    @Override protected void onPause() {
        stopWeather(); super.onPause();
    }
    private void stopWeather() {
        weatherActive = false; weatherHandler.removeCallbacks(weatherTick);
        clearWeatherListener();
    }
    private void clearWeatherListener() {
        if (locationManager != null && weatherListener != null) {
            try { locationManager.removeUpdates(weatherListener); } catch (Exception ignored) {}
        }
        weatherListener = null;
    }
    private void weatherDisplay(String value) {
        weatherValue = value;
        if (weatherText != null) weatherText.setText(value);
    }
    private void refreshWeather(boolean manual) {
        if (!weatherActive || weatherBusy) return;
        boolean fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) {
            if (manual || !weatherPermissionAsked) {
                weatherPermissionAsked = true;
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION);
            }
            return;
        }
        clearWeatherListener();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null) return;
        try {
            Location best = null;
            java.util.List<String> providers = locationManager.getProviders(true);
            for (String provider : providers) {
                try {
                    Location l = locationManager.getLastKnownLocation(provider);
                    if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
                } catch (Exception ignored) {}
            }
            if (best != null && System.currentTimeMillis() - best.getTime() < 300000L) {
                fetchWeather(best); return;
            }
            weatherListener = new LocationListener() {
                @Override public void onLocationChanged(Location l) { clearWeatherListener(); if (weatherActive) fetchWeather(l); }
                @Override public void onProviderEnabled(String p) {}
                @Override public void onProviderDisabled(String p) {}
                @Override public void onStatusChanged(String p, int status, Bundle extras) {}
            };
            boolean requested = false;
            for (String provider : providers) {
                if (!LocationManager.GPS_PROVIDER.equals(provider) && !LocationManager.NETWORK_PROVIDER.equals(provider) && !"fused".equals(provider)) continue;
                try { locationManager.requestLocationUpdates(provider, 1000L, 0f, weatherListener); requested = true; } catch (Exception ignored) {}
            }
            if (!requested) { clearWeatherListener(); weatherDisplay("GPS --"); return; }
            final LocationListener pending = weatherListener;
            weatherHandler.postDelayed(() -> { if (weatherListener == pending) { clearWeatherListener(); weatherDisplay("GPS --"); } }, 30000L);
        } catch (Exception e) { clearWeatherListener(); weatherDisplay("GPS --"); }
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == LOCATION_PERMISSION && weatherActive) refreshWeather(false);
    }
    private void fetchWeather(Location location) {
        if (weatherBusy) return;
        weatherBusy = true;
        final double lat = location.getLatitude(), lon = location.getLongitude();
        new Thread(() -> {
            HttpURLConnection connection = null;
            String value;
            try {
                String address = String.format(Locale.US, "https://api.open-meteo.com/v1/forecast?latitude=%.5f&longitude=%.5f&current=temperature_2m,wind_speed_10m,wind_gusts_10m&wind_speed_unit=ms", lat, lon);
                connection = (HttpURLConnection) new URL(address).openConnection();
                connection.setConnectTimeout(10000); connection.setReadTimeout(10000);
                StringBuilder body = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line; while ((line = reader.readLine()) != null) body.append(line);
                }
                JSONObject current = new JSONObject(body.toString()).getJSONObject("current");
                value = String.format(Locale.US, "%.0f °C\n%.1f / %.1f m/s", current.getDouble("temperature_2m"), current.getDouble("wind_speed_10m"), current.getDouble("wind_gusts_10m"));
            } catch (Exception e) { value = "-- °C\n-- m/s"; }
            finally { if (connection != null) connection.disconnect(); }
            final String result = value;
            runOnUiThread(() -> { weatherBusy = false; if (weatherActive && !isFinishing() && !isDestroyed()) weatherDisplay(result); });
        }).start();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable glassShape(int radius, boolean field) {
        GradientDrawable shape = new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            field ? new int[]{Color.argb(150, 14, 31, 48), Color.argb(115, 30, 55, 72)}
                  : new int[]{Color.argb(155, 255, 255, 255), Color.argb(92, 45, 75, 95), Color.argb(125, 13, 30, 47)});
        shape.setCornerRadius(dp(radius));
        shape.setStroke(dp(1), glassBorder);
        return shape;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        storage = getSharedPreferences("grid_data", MODE_PRIVATE);
        language = storage.getString("language", "ru");
        loadJobs();
        if (profileName().isEmpty()) showProfile(true); else showHome();
    }

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.WHITE);
        t.setPadding(dp(8), dp(6), dp(8), dp(6));
        t.setShadowLayer(dp(2), 0, dp(1), Color.argb(190, 0, 0, 0));
        return t;
    }

    private void page(String heading, Runnable back) {
        backAction = back;
        buttonCount = 0;
        FrameLayout root = new FrameLayout(this);
        ImageView wallpaper = new ImageView(this);
        wallpaper.setImageResource(R.drawable.grid_background);
        wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        wallpaper.setColorFilter(Color.argb(72, 0, 12, 25), PorterDuff.Mode.SRC_ATOP);
        root.addView(wallpaper, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setPadding(dp(12), dp(36), dp(12), 0);
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setBackground(glassShape(16, true));
        Button arrow = new Button(this);
        arrow.setText("\u2190");
        arrow.setTextSize(25);
        arrow.setTextColor(white);
        arrow.setPadding(0, 0, 0, 0);
        arrow.setBackgroundColor(Color.TRANSPARENT);
        arrow.setContentDescription(local("\u041d\u0430\u0437\u0430\u0434", "Back", "Zur\u00fcck", "Atpaka\u013c"));
        arrow.setVisibility(back == null ? View.INVISIBLE : View.VISIBLE);
        arrow.setOnClickListener(v -> onBackPressed());
        toolbar.addView(arrow, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView title = text(heading, 21);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(dp(4), dp(8), dp(4), dp(8));
        if ("GRID".equalsIgnoreCase(heading.trim())) {
    LinearLayout.LayoutParams turbineSize =
        new LinearLayout.LayoutParams(dp(28), dp(28));

    TextView leftTurbine = text("✳", 27);
    leftTurbine.setGravity(Gravity.CENTER);
    leftTurbine.setPadding(0, 0, 0, 0);
    toolbar.addView(leftTurbine, turbineSize);

    android.view.animation.RotateAnimation spin =
    new android.view.animation.RotateAnimation(
        0f, 360f,
        android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
        android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
    );
spin.setDuration(3000);
spin.setRepeatCount(
    android.view.animation.Animation.INFINITE
);
spin.setInterpolator(
    new android.view.animation.LinearInterpolator()
);
leftTurbine.startAnimation(spin);
}
        toolbar.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
weatherText = new TextView(this);
weatherText.setText(weatherValue);
weatherText.setOnClickListener(v -> refreshWeather(true));
weatherText.setTextColor(0xFFFFFFFF);
weatherText.setTextSize(12);
weatherText.setGravity(17);
toolbar.addView(weatherText, new LinearLayout.LayoutParams(dp(90), dp(48)));
        shell.addView(toolbar, new LinearLayout.LayoutParams(-1, -2));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(4), dp(8), dp(4), dp(16));
        scroll.addView(layout);
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(shell, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        // Soft fade and upward slide whenever a GRID page opens.
        scroll.setAlpha(0f);
        scroll.setTranslationY(dp(18));
        scroll.animate().alpha(1f).translationY(0f)
            .setDuration(320).setInterpolator(new DecelerateInterpolator()).start();
    }

    @Override public void onBackPressed() {
        if (backAction != null) backAction.run(); else super.onBackPressed();
    }

    private void button(String label, Runnable action) {
        button(label, action, false);
    }

    private void button(String label, Runnable action, boolean completed) {
        button(label, action, completed, false);
    }

    private void button(String label, Runnable action, boolean completed, boolean blocked) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(white);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(null, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        if (blocked) {
            GradientDrawable red = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xff913b3b, 0xff602727});
            red.setCornerRadius(dp(22)); red.setStroke(dp(1), 0xfff87171); b.setBackground(red);
        } else if (completed) {
            GradientDrawable green = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xff287b49, 0xe61b6038, 0xf012432b});
            green.setCornerRadius(dp(22));
            green.setStroke(dp(1), 0xff85efaa);
            b.setBackground(green);
        } else {
            b.setBackground(glassShape(22, false));
        }
        b.setElevation(dp(5));
        b.setPadding(dp(10), dp(6), dp(10), dp(6));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(3), 0, dp(4));
        layout.addView(b, p);
        b.setMinimumHeight(dp(48));
        b.setMinHeight(dp(48));
        // Glass buttons compress gently under the finger and spring back.
        b.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().cancel();
                    view.animate().scaleX(0.965f).scaleY(0.965f)
                        .setDuration(110).setInterpolator(new DecelerateInterpolator()).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().cancel();
                    view.animate().scaleX(1f).scaleY(1f)
                        .setDuration(260).setInterpolator(new OvershootInterpolator(1.8f)).start();
                    break;
            }
            return false; // Preserve Android's normal click and accessibility behavior.
        });
        b.setOnClickListener(v -> action.run());
        // Staggered entrance on each screen.
        b.setAlpha(0f);
        b.setTranslationY(dp(10));
        b.animate().alpha(1f).translationY(0f).setStartDelay(Math.min(buttonCount++, 8) * 35L)
            .setDuration(280).setInterpolator(new DecelerateInterpolator()).start();
    }

    private void showHome() {
        jobsReturn = this::showJobs;
        selectedTurbine = null;
        page("GRID", null);
        TextView sub = text("TEAM WORK MANAGEMENT", 15);
        sub.setGravity(Gravity.CENTER);
        layout.addView(sub);
        button(tr("\u0422\u0423\u0420\u0411\u0418\u041d\u042b"), this::showTurbines);
        button(tr("\u0420\u0410\u0411\u041e\u0422\u042b"), this::showJobs);
        button(local("\u041f\u041e\u0418\u0421\u041a", "SEARCH", "SUCHE", "MEKL\u0112\u0160ANA"), () -> showSearch("", 0));button(tr("\u0414\u041e\u0411\u0410\u0412\u0418\u0422\u042c \u0420\u0410\u0411\u041e\u0422\u0423"), () -> editJob(-1));
        int nc = 0, done = 0;
        for (JSONObject j : jobs) { if (hasNc(j)) nc++; else if (j.optBoolean("done")) done++; }
        layout.addView(text(local("\u0412\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u043e", "Completed", "Erledigt", "Pabeigts") + ": " + done + " / " + jobs.size() + "   NC: " + nc + "   " + local("\u041e\u0441\u0442\u0430\u043b\u043e\u0441\u044c \u0431\u0435\u0437 NC", "Pending without NC", "Offen ohne NC", "Atliku\u0161i bez NC") + ": " + (jobs.size()-done-nc), 16));
        button(local("\u0412\u0421\u0415 \u041f\u0420\u041e\u0411\u041b\u0415\u041c\u042b NC", "ALL NC ISSUES", "ALLE NC-PROBLEME", "VISAS NC PROBL\u0112MAS") + " (" + nc + ")", () -> showNcList(null), false, nc > 0);
        button(tr("\u041a\u0410\u041b\u0415\u041d\u0414\u0410\u0420\u042c"), this::showJobs);
        button(tr("\u0424\u041e\u0422\u041e\u0413\u0420\u0410\u0424\u0418\u0418"), this::showGallery);
        button(tr("\u041a\u041e\u041c\u0410\u041d\u0414\u0410"), this::showTeam);
        button(tr("\u0421\u0418\u041d\u0425\u0420\u041e\u041d\u0418\u0417\u0410\u0426\u0418\u042f"), this::showSync);
        button(tr("\u0428\u0410\u0411\u041b\u041e\u041d \u0420\u0410\u0411\u041e\u0422"), this::importTemplate);
        button(tr("\u042f\u0417\u042b\u041a / LANGUAGE"), this::chooseLanguage);
    }

    private String local(String ru, String en, String de, String lv) {
        return "en".equals(language) ? en : "de".equals(language) ? de : "lv".equals(language) ? lv : ru;
    }

    private void progress(int done, int total) {
        int percent = total == 0 ? 0 : (int)(100L * done / total);
        TextView label = text(local("\u0412\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u043e", "Completed", "Erledigt", "Pabeigts") + ": " + done + " / " + total + "  \u2022  " + percent + "%", 17);
        label.setTextColor(0xff75ef98);
        layout.addView(label);
        android.widget.ProgressBar bar = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(Math.max(1, total));
        bar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xff22c55e));
        bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff344550));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(10));
        p.setMargins(dp(8), 0, dp(8), dp(8));
        layout.addView(bar, p);
        android.animation.ObjectAnimator.ofInt(bar, "progress", 0, done).setDuration(500).start();
    }

    private void showTurbines() {
        jobsReturn = this::showJobs;
        selectedTurbine = null;
        page(local("\u0422\u0423\u0420\u0411\u0418\u041d\u042b", "TURBINES", "TURBINEN", "TURB\u012aNAS"), this::showHome);
        java.util.Map<String, int[]> counts = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (JSONObject job : jobs) {
            String name = job.optString("turbine").trim();
            int[] c = counts.get(name);
            if (c == null) { c = new int[2]; counts.put(name, c); }
            c[0]++;
            if (job.optBoolean("done")) c[1]++;
        }
        if (counts.isEmpty()) layout.addView(text(local("\u041f\u043e\u043a\u0430 \u043d\u0435\u0442 \u0442\u0443\u0440\u0431\u0438\u043d", "No turbines yet", "Noch keine Turbinen", "V\u0113l nav turb\u012bnu"), 17));
        for (java.util.Map.Entry<String, int[]> entry : counts.entrySet()) {
            final String name = entry.getKey();
            int[] c = entry.getValue();
            button(name, () -> showTurbineJobs(name));
            progress(c[1], c[0]);
        }
        button(local("+ \u0428\u0430\u0431\u043b\u043e\u043d \u0440\u0430\u0431\u043e\u0442", "+ Job template", "+ Arbeitsvorlage", "+ Darbu veidne"), this::importTemplate);
    }

    private void showTurbineJobs(String name) {
        jobsReturn = () -> showTurbineJobs(name);
        selectedTurbine = name;
        page(name, this::showTurbines);
        java.util.Map<String, ArrayList<Integer>> groups = new java.util.LinkedHashMap<>();
        int total = 0, done = 0;
        for (int i = 0; i < jobs.size(); i++) {
            JSONObject j = jobs.get(i);
            if (!name.equalsIgnoreCase(j.optString("turbine").trim())) continue;
            total++;
            if (j.optBoolean("done")) done++;
            String title = j.optString("name");
            int split = title.indexOf(" \u2014 ");
            String group = split < 0 ? local("\u0414\u0440\u0443\u0433\u0438\u0435 \u0440\u0430\u0431\u043e\u0442\u044b", "Other jobs", "Andere Arbeiten", "Citi darbi") : title.substring(0, split);
            ArrayList<Integer> indices = groups.get(group);
            if (indices == null) { indices = new ArrayList<>(); groups.put(group, indices); }
            indices.add(i);
        }
        progress(done, total);
        button(local("\u0421\u041e\u0417\u0414\u0410\u0422\u042c PDF", "CREATE PDF REPORT", "PDF-BERICHT ERSTELLEN", "IZVEIDOT PDF"), () -> exportReport(name));
        int ncCount = 0;
        for (JSONObject j : jobs) if (name.equalsIgnoreCase(j.optString("turbine").trim()) && hasNc(j)) ncCount++;
        layout.addView(text("NC: " + ncCount, 16));
        button(tr("+ \u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0440\u0430\u0431\u043e\u0442\u0443"), () -> editJob(-1));
        button(tr("\u0424\u041e\u0422\u041e\u0413\u0420\u0410\u0424\u0418\u0418"), () -> showGallery());
        for (java.util.Map.Entry<String, ArrayList<Integer>> entry : groups.entrySet()) {
            int completed = 0;
            for (int i : entry.getValue()) if (jobs.get(i).optBoolean("done")) completed++;
            final String group = entry.getKey();
            final ArrayList<Integer> indices = entry.getValue();
            button(group + " (" + completed + "/" + indices.size() + ")", () -> showCategory(name, group));
            progress(completed, indices.size());
        }
    }

    private void exportReport(String turbine) {
        final ArrayList<JSONObject> snapshot = new ArrayList<>();
        try { for (JSONObject j : jobs) if (turbine.equalsIgnoreCase(j.optString("turbine").trim())) snapshot.add(new JSONObject(j.toString())); }
        catch (Exception e) { message(e.toString()); return; }
        if (android.os.Build.VERSION.SDK_INT < 29) { message("Android 10+ required"); return; }
        message(local("\u0421\u043e\u0437\u0434\u0430\u043d\u0438\u0435 PDF...", "Creating PDF...", "PDF wird erstellt...", "Veido PDF..."));
        new Thread(() -> {
            Uri target = null;
            android.graphics.pdf.PdfDocument pdf = null;
            try {
                pdf = new android.graphics.pdf.PdfDocument();
                ReportWriter w = new ReportWriter(pdf, turbine);
                int done=0, nc=0;
                for (JSONObject j : snapshot) { if (hasNc(j)) nc++; else if (j.optBoolean("done")) done++; }
                w.line(local("\u041e\u0442\u0447\u0451\u0442 \u043f\u043e \u0442\u0443\u0440\u0431\u0438\u043d\u0435", "Turbine report", "Turbinenbericht", "Turb\u012bnas p\u0101rskats"), 21, 0xff17354a);
                w.line(new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm z", Locale.ROOT).format(new java.util.Date()), 11, 0xff555555);
                w.line(local("\u0412\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u043e", "Completed", "Erledigt", "Pabeigts") + ": " + done + "/" + snapshot.size() + " (" + (snapshot.isEmpty()?0:100L*done/snapshot.size()) + "%)   NC: " + nc + "   " + local("\u041e\u0441\u0442\u0430\u043b\u043e\u0441\u044c \u0431\u0435\u0437 NC", "Pending without NC", "Offen ohne NC", "Atliku\u0161i bez NC") + ": " + (snapshot.size()-done-nc), 13, 0xff17354a);
                for (int state : new int[]{0,1,2}) {
                    w.line(state==0?local("\u041e\u0442\u043a\u0440\u044b\u0442\u044b\u0435 NC", "Open NC", "Offene NC", "Atv\u0113rtie NC"):state==1?local("\u0412\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u043d\u044b\u0435 \u0440\u0430\u0431\u043e\u0442\u044b", "Completed jobs", "Erledigte Arbeiten", "Pabeigtie darbi"):local("\u041d\u0435\u0437\u0430\u0432\u0435\u0440\u0448\u0451\u043d\u043d\u044b\u0435 \u0440\u0430\u0431\u043e\u0442\u044b", "Pending jobs", "Offene Arbeiten", "Nepabeigtie darbi"), 17, 0xff17354a);
                    for (JSONObject j : snapshot) {
                        int s=hasNc(j)?0:j.optBoolean("done")?1:2;
                        if(s!=state) continue;
                        w.line(j.optString("name"), 12, s==0?0xffa12626:s==1?0xff17663a:0xff333333);
                        if(s==0) {
                            w.line(ncInfo(j).trim(), 11, 0xff333333);
                            JSONArray a=ncPhotos(j);
                            for(int i=0;i<a.length();i++) w.photo(a.optString(i));
                        } else if(s==1) w.line(completionInfo(j).trim(), 11, 0xff555555);
                        w.y+=7;
                    }
                }
                w.finish();
                String name="GRID_"+turbine.replaceAll("[^a-zA-Z0-9_-]","_")+"_"+System.currentTimeMillis()+".pdf";
                android.content.ContentValues v=new android.content.ContentValues();
                v.put("_display_name",name); v.put("mime_type","application/pdf"); v.put("relative_path","Download/GRID"); v.put("is_pending",1);
                target=getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);
                if(target==null) throw new java.io.IOException("PDF save failed");
                try(java.io.OutputStream out=getContentResolver().openOutputStream(target)){if(out==null) throw new java.io.IOException("PDF output unavailable"); pdf.writeTo(out);}
                v.clear(); v.put("is_pending",0); getContentResolver().update(target,v,null,null);
                final Uri result=target;
                runOnUiThread(()->{
                    Intent share=new Intent(Intent.ACTION_SEND); share.setType("application/pdf"); share.putExtra(Intent.EXTRA_STREAM,result); share.setClipData(android.content.ClipData.newRawUri("GRID PDF",result)); share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    message(local("PDF \u0441\u043e\u0445\u0440\u0430\u043d\u0451\u043d \u0432 Download/GRID", "PDF saved in Download/GRID", "PDF in Download/GRID gespeichert", "PDF saglab\u0101ts Download/GRID"));
                    try { startActivity(Intent.createChooser(share,"GRID PDF")); } catch(Exception e){message(e.toString());}
                });
            } catch(Exception e){if(target!=null) try{getContentResolver().delete(target,null,null);}catch(Exception ignored){} runOnUiThread(()->message("PDF: "+e.getMessage()));}
            finally { if (pdf != null) try { pdf.close(); } catch (Exception ignored) {} }
        }).start();
    }

    private class ReportWriter {
        final android.graphics.pdf.PdfDocument pdf;
        final String turbine;
        android.graphics.pdf.PdfDocument.Page p;
        android.graphics.Canvas c;
        int y, number;
        ReportWriter(android.graphics.pdf.PdfDocument pdf,String turbine){this.pdf=pdf;this.turbine=turbine;next();}
        void finish(){if(p!=null){pdf.finishPage(p);p=null;}}
        void next(){
            finish(); p=pdf.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(595,842,++number).create()); c=p.getCanvas();
            android.graphics.Paint paint=new android.graphics.Paint(3); paint.setColor(0xff17354a); c.drawRect(0,0,595,65,paint);
            paint.setColor(Color.WHITE); paint.setTextSize(23); paint.setTypeface(Typeface.DEFAULT_BOLD); c.drawText("GRID TEAM",36,39,paint);
            paint.setColor(0xff555555); paint.setTextSize(10); c.drawText("GRID  |  "+number,36,817,paint); y=80;
            line(turbine.length()>150?turbine.substring(0,150):turbine,14,0xff17354a);
        }
        void line(String value,int size,int color){
            android.text.TextPaint paint=new android.text.TextPaint(3); paint.setColor(color); paint.setTextSize(size); paint.setTypeface(size>=14?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);
            android.text.StaticLayout sl=android.text.StaticLayout.Builder.obtain(value,0,value.length(),paint,523).setIncludePad(false).build();
            for(int i=0;i<sl.getLineCount();i++){
                int h=sl.getLineBottom(i)-sl.getLineTop(i); if(y+h>790) next();
                c.save(); c.clipRect(36,y,559,y+h); c.translate(36,y-sl.getLineTop(i)); sl.draw(c); c.restore(); y+=h;
            } y+=5;
        }
        void photo(String uri){
            android.graphics.Bitmap bitmap=null;
            try {
                android.graphics.BitmapFactory.Options o=new android.graphics.BitmapFactory.Options(); o.inJustDecodeBounds=true;
                try(java.io.InputStream in=getContentResolver().openInputStream(Uri.parse(uri))){android.graphics.BitmapFactory.decodeStream(in,null,o);}
                if(o.outWidth<=0||o.outHeight<=0) throw new java.io.IOException("Invalid photo");
                o.inSampleSize=1; while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>1400) o.inSampleSize*=2; o.inJustDecodeBounds=false;
                try(java.io.InputStream in=getContentResolver().openInputStream(Uri.parse(uri))){bitmap=android.graphics.BitmapFactory.decodeStream(in,null,o);}
                if(bitmap==null) throw new java.io.IOException("Photo unavailable");
                float scale=Math.min(523f/bitmap.getWidth(),260f/bitmap.getHeight()); int h=Math.max(1,(int)(bitmap.getHeight()*scale));
                if(y+h>790) next(); c.drawBitmap(bitmap,null,new android.graphics.RectF(36,y,36+bitmap.getWidth()*scale,y+h),new android.graphics.Paint(3)); y+=h+10;
            } catch(Exception e){line(local("\u0424\u043e\u0442\u043e \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u043d\u043e", "Photo unavailable", "Foto nicht verf\u00fcgbar", "Foto nav pieejams"),10,0xffa12626);}
            finally{if(bitmap!=null) bitmap.recycle();}
        }
    }

    private String profileName() { return storage.getString("profile_name", "").trim(); }
    private String profileId() { return storage.getString("profile_id", ""); }
    private void showProfile(boolean required) {
        page(local("\u041c\u041e\u0419 \u041f\u0420\u041e\u0424\u0418\u041b\u042c", "MY PROFILE", "MEIN PROFIL", "MANS PROFILS"), required ? null : this::showTeam);
        EditText name = field(local("\u0418\u043c\u044f \u0441\u043e\u0442\u0440\u0443\u0434\u043d\u0438\u043a\u0430", "Employee name", "Name", "Darbinieka v\u0101rds"), profileName());
        name.setSingleLine(true);
        name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});
        button(local("\u0421\u041e\u0425\u0420\u0410\u041d\u0418\u0422\u042c", "SAVE", "SPEICHERN", "SAGLAB\u0100T"), () -> {
            String value = name.getText().toString().trim();
            if (value.isEmpty()) { message(local("\u0412\u0432\u0435\u0434\u0438 \u0438\u043c\u044f", "Enter your name", "Name eingeben", "Ievadi v\u0101rdu")); return; }
            String id = profileId().isEmpty() ? java.util.UUID.randomUUID().toString() : profileId();
            storage.edit().putString("profile_name", value).putString("profile_id", id).apply();
            if (required) showHome(); else showTeam();
        });
        if (!required) button(local("\u0424\u041e\u0422\u041e \u041f\u0420\u041e\u0424\u0418\u041b\u042f", "PROFILE PHOTO", "PROFILFOTO", "PROFILA FOTO"), () -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*"); intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, 202);
        });
        button(local("\u042f\u0417\u042b\u041a", "LANGUAGE", "SPRACHE", "VALODA"), this::chooseLanguage);
    }
    private void showTeam() {
        page(local("\u041a\u041e\u041c\u0410\u041d\u0414\u0410", "TEAM", "TEAM", "KOMANDA"), this::showHome);
        String uri = storage.getString("profile_photo", "");
        if (!uri.isEmpty()) {
            ImageView avatar = new ImageView(this); avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            try { avatar.setImageURI(Uri.parse(uri)); layout.addView(avatar, new LinearLayout.LayoutParams(dp(96), dp(96))); } catch (Exception ignored) {}
        }
        button(local("\u041c\u043e\u0439 \u043f\u0440\u043e\u0444\u0438\u043b\u044c: ", "My profile: ", "Mein Profil: ", "Mans profils: ") + profileName(), () -> showProfile(false));
        java.util.Map<String, java.util.ArrayList<JSONObject>> groups = new java.util.LinkedHashMap<>();
        groups.put(profileId(), new java.util.ArrayList<>());
        for (JSONObject j : jobs) {
            if (!j.optBoolean("done") || j.optString("completedBy").isEmpty()) continue;
            String key = j.optString("completedById", j.optString("completedBy"));
            groups.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(j);
        }
        for (java.util.Map.Entry<String, java.util.ArrayList<JSONObject>> entry : groups.entrySet()) {
            java.util.ArrayList<JSONObject> items = entry.getValue();
            String name = entry.getKey().equals(profileId()) ? profileName() : items.get(0).optString("completedBy");
            button(name + " \u2022 " + items.size(), () -> {
                page(name, this::showTeam);
                for (JSONObject j : items) layout.addView(text(j.optString("turbine") + " / " + j.optString("name") + completionInfo(j), 16));
            });
        }
    }

    private String completionInfo(JSONObject j) {
        if (!j.optBoolean("done")) return "";
        long time = j.optLong("completedAt", 0);
        if (time <= 0) return "\n" + local("\u0414\u0430\u0442\u0430 \u0437\u0430\u0432\u0435\u0440\u0448\u0435\u043d\u0438\u044f \u043d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043d\u0430", "Completion date unknown", "Abschlussdatum unbekannt", "Pabeig\u0161anas datums nav zin\u0101ms");
        String stamp = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT).format(new java.util.Date(time));
        return "\n" + local("\u0417\u0430\u0432\u0435\u0440\u0448\u0435\u043d\u043e: ", "Completed: ", "Abgeschlossen: ", "Pabeigts: ") + stamp + "\n" + local("\u0412\u044b\u043f\u043e\u043b\u043d\u0438\u043b: ", "Completed by: ", "Erledigt von: ", "Izpild\u012bja: ") + (j.optString("completedBy").isEmpty() ? local("\u041d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043d\u043e", "Unknown", "Unbekannt", "Nav zin\u0101ms") : j.optString("completedBy"));
    }

    private boolean hasNc(JSONObject j) { return !j.optString("nc").trim().isEmpty(); }
    private String ncInfo(JSONObject j) {
        if (!hasNc(j)) return "";
        long t = j.optLong("ncAt", 0);
        return "\nNC: " + j.optString("nc") + "\n" + j.optString("ncBy") + (t > 0 ? " / " + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT).format(new java.util.Date(t)) : "");
    }
    private JSONObject ncPhotoJob;
    private Runnable ncPhotoBack;
    private JSONArray ncPhotos(JSONObject j) {
        JSONArray a = j.optJSONArray("ncPhotos");
        return a == null ? new JSONArray() : a;
    }
    private void showNcPhotos(JSONObject j, Runnable back) {
        page(local("\u0424\u041e\u0422\u041e NC", "NC PHOTOS", "NC-FOTOS", "NC FOTO"), back);
        layout.addView(text(j.optString("name") + ncInfo(j), 16));
        button(local("\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0444\u043e\u0442\u043e NC", "Add NC photos", "NC-Fotos hinzuf\u00fcgen", "Pievienot NC foto"), () -> {
            ncPhotoJob = j; ncPhotoBack = back;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*"); intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(intent, 203);
        });
        JSONArray a = ncPhotos(j);
        for (int i = 0; i < a.length(); i++) {
            final int pos = i;
            addThumbnail(a.optString(i));
            button(local("\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u0444\u043e\u0442\u043e", "Remove photo", "Foto entfernen", "No\u0146emt foto") + " " + (i+1), () -> {
                new AlertDialog.Builder(this).setMessage(local("\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u0444\u043e\u0442\u043e NC?", "Remove NC photo?", "NC-Foto entfernen?", "No\u0146emt NC foto?"))
                    .setNegativeButton(local("\u041e\u0442\u043c\u0435\u043d\u0430", "Cancel", "Abbrechen", "Atcelt"), null)
                    .setPositiveButton("OK", (d,w) -> {
                        try { JSONArray b = new JSONArray(); for (int k=0;k<a.length();k++) if(k!=pos) b.put(a.optString(k)); j.put("ncPhotos",b); saveJobs(); showNcPhotos(j,back); }
                        catch(Exception e) { message(e.getMessage()); }
                    }).show();
            });
        }
    }
    private void receiveNcPhotos(Intent data) {
        JSONObject j = ncPhotoJob; Runnable back = ncPhotoBack;
        ncPhotoJob = null; ncPhotoBack = null;
        if(j == null || back == null || data == null || !hasNc(j)) return;
        try {
            JSONArray a = ncPhotos(j);
            java.util.HashSet<String> seen = new java.util.HashSet<>();
            for(int i=0;i<a.length();i++) seen.add(a.optString(i));
            int count = data.getClipData() == null ? 1 : data.getClipData().getItemCount();
            for(int i=0;i<count;i++) {
                Uri u = data.getClipData() == null ? data.getData() : data.getClipData().getItemAt(i).getUri();
                if(u == null || seen.contains(u.toString())) continue;
                try { getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION); a.put(u.toString()); seen.add(u.toString()); }
                catch(SecurityException e) { message(local("\u041d\u0435\u0442 \u0434\u043e\u0441\u0442\u0443\u043f\u0430 \u043a \u0444\u043e\u0442\u043e", "Photo access denied", "Kein Fotozugriff", "Nav piek\u013cuves foto")); }
            }
            j.put("ncPhotos",a); saveJobs(); showNcPhotos(j,back);
        } catch(Exception e) { message(e.getMessage()); }
    }
    private void editNc(JSONObject j, Runnable back) {
        EditText input = new EditText(this);
        input.setHint(local("\u041f\u0440\u0438\u0447\u0438\u043d\u0430: \u043c\u0430\u0442\u0435\u0440\u0438\u0430\u043b\u044b, \u0438\u043d\u0441\u0442\u0440\u0443\u043c\u0435\u043d\u0442\u044b...", "Reason: materials, tools...", "Grund: Material, Werkzeug...", "Iemesls: materi\u0101li, instrumenti..."));
        input.setText(j.optString("nc")); input.setMinLines(2);
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(500)});
        new AlertDialog.Builder(this).setTitle("NC").setView(input)
            .setNegativeButton(local("\u041e\u0442\u043c\u0435\u043d\u0430", "Cancel", "Abbrechen", "Atcelt"), null)
            .setNeutralButton(local("\u0423\u0434\u0430\u043b\u0438\u0442\u044c NC", "Clear NC", "NC l\u00f6schen", "Dz\u0113st NC"), (d,w) -> updateNc(j, "", back))
            .setPositiveButton(local("\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c", "Save", "Speichern", "Saglab\u0101t"), (d,w) -> {
                String value = input.getText().toString().trim();
                if (!value.isEmpty()) updateNc(j, value, () -> showNcPhotos(j, back));
            }).show();
    }
    private void updateNc(JSONObject j, String value, Runnable back) {
        try {
            if (value.isEmpty()) j.remove("ncPhotos");
            j.put("nc", value); j.put("ncAt", System.currentTimeMillis()); j.put("ncBy", profileName());
            if (!value.isEmpty()) { j.put("done", false); j.remove("completedAt"); j.remove("completedBy"); j.remove("completedById"); }
            saveJobs(); back.run();
        } catch (Exception e) { message("NC: " + e.getMessage()); }
    }
    private void showNcList(String filter) {
        selectedTurbine = null;
        jobsReturn = () -> showNcList(filter);
        page(local("\u041f\u0420\u041e\u0411\u041b\u0415\u041c\u042b NC", "NC ISSUES", "NC-PROBLEME", "NC PROBL\u0112MAS"), this::showHome);
        java.util.TreeSet<String> turbines = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (JSONObject j : jobs) if (hasNc(j)) turbines.add(j.optString("turbine").trim());
        String all = local("\u0412\u0441\u0435 \u0442\u0443\u0440\u0431\u0438\u043d\u044b", "All turbines", "Alle Turbinen", "Visas turb\u012bnas");
        button(local("\u0424\u0438\u043b\u044c\u0442\u0440: ", "Filter: ", "Filter: ", "Filtrs: ") + (filter == null ? all : filter), () -> {
            java.util.ArrayList<String> names = new java.util.ArrayList<>(turbines);
            names.add(0, all);
            new AlertDialog.Builder(this).setTitle(all).setItems(names.toArray(new String[0]), (d,w) -> showNcList(w == 0 ? null : names.get(w))).show();
        });
        java.util.ArrayList<Integer> indices = new java.util.ArrayList<>();
        for (int i=0; i<jobs.size(); i++) {
            JSONObject j = jobs.get(i);
            if (hasNc(j) && (filter == null || filter.equalsIgnoreCase(j.optString("turbine").trim()))) indices.add(i);
        }
        java.util.Collections.sort(indices, (a,b) -> Long.compare(jobs.get(b).optLong("ncAt"), jobs.get(a).optLong("ncAt")));
        layout.addView(text("NC: " + indices.size(), 17));
        if (indices.isEmpty()) layout.addView(text(local("\u041d\u0435\u0442 \u043e\u0442\u043a\u0440\u044b\u0442\u044b\u0445 \u043f\u0440\u043e\u0431\u043b\u0435\u043c", "No open issues", "Keine offenen Probleme", "Nav atv\u0113rtu probl\u0113mu"), 17));
        for (int index : indices) {
            JSONObject j = jobs.get(index);
            button(j.optString("turbine") + "\n" + j.optString("name") + ncInfo(j) + "\n" + local("\u0424\u043e\u0442\u043e \u0440\u0430\u0431\u043e\u0442\u044b: ", "Job photos: ", "Arbeitsfotos: ", "Darba foto: ") + photos(j).length() + " / NC: " + ncPhotos(j).length(), () -> jobOptions(index), false, true);
        }
    }

    private void showCategory(String name, String group) {
        selectedTurbine = name;
        jobsReturn = () -> showCategory(name, group);
        page(group, () -> showTurbineJobs(name));
        for (int i = 0; i < jobs.size(); i++) {
            JSONObject j = jobs.get(i);
            if (!name.equalsIgnoreCase(j.optString("turbine").trim())) continue;
            String title = j.optString("name");
            int split = title.indexOf(" \u2014 ");
            String category = split < 0 ? local("\u0414\u0440\u0443\u0433\u0438\u0435 \u0440\u0430\u0431\u043e\u0442\u044b", "Other jobs", "Andere Arbeiten", "Citi darbi") : title.substring(0, split);
            if (!group.equals(category)) continue;
            final int index = i;
            button((j.optBoolean("done") ? "\u2713 " : "\u25cb ") + (split < 0 ? title : title.substring(split + 3)) + completionInfo(j) + ncInfo(j), () -> jobOptions(index), j.optBoolean("done"), hasNc(j));
        }
    }

    private void showJobs() {
        jobsReturn = this::showJobs;
        if (selectedTurbine != null) { showTurbineJobs(selectedTurbine); return; }
        page(tr("\u0420\u0410\u0411\u041e\u0422\u042b GRID"), this::showHome);
        button(tr("+ \u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0440\u0430\u0431\u043e\u0442\u0443"), () -> editJob(-1));
        if (jobs.isEmpty()) layout.addView(text(tr("\u041f\u043e\u043a\u0430 \u043d\u0435\u0442 \u0440\u0430\u0431\u043e\u0442. \u0414\u043e\u0431\u0430\u0432\u044c \u043f\u0435\u0440\u0432\u0443\u044e!"), 17));
        for (int i = 0; i < jobs.size(); i++) {
            final int index = i;
            JSONObject j = jobs.get(i);
            String status = j.optBoolean("done") ? tr("\u2713 \u0412\u042b\u041f\u041e\u041b\u041d\u0415\u041d\u041e") : tr("\u25cb \u0412 \u0420\u0410\u0411\u041e\u0422\u0415");
            String info = j.optString("turbine") + "\n" + j.optString("name") + "\n"
                    + j.optString("date") + "\n" + status + tr(" \u2022 \u0424\u043e\u0442\u043e: ") + photos(j).length() + completionInfo(j);
            button(info + ncInfo(j), () -> jobOptions(index), j.optBoolean("done"), hasNc(j));
        }
    }

    private EditText field(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Color.LTGRAY);
        e.setTextColor(Color.WHITE);
        e.setText(value);
        e.setTextSize(17);
        e.setPadding(dp(16), dp(15), dp(16), dp(15));
        e.setBackground(glassShape(18, true));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(8), 0, dp(10));
        layout.addView(e, params);
        return e;
    }

    private void editJob(int index) {
        Runnable returnToJobs = jobsReturn != null ? jobsReturn : this::showJobs;
        editingIndex = index;
        JSONObject old = index >= 0 ? jobs.get(index) : new JSONObject();
        draftPhotos.clear();
        JSONArray saved = photos(old);
        for (int i = 0; i < saved.length(); i++) draftPhotos.add(saved.optString(i));
        page(index >= 0 ? tr("\u0420\u0415\u0414\u0410\u041a\u0422\u0418\u0420\u041e\u0412\u0410\u0422\u042c") : tr("\u041d\u041e\u0412\u0410\u042f \u0420\u0410\u0411\u041e\u0422\u0410"), returnToJobs);
        EditText turbine = field(tr("\u041d\u043e\u043c\u0435\u0440 \u0438\u043b\u0438 \u043d\u0430\u0437\u0432\u0430\u043d\u0438\u0435 \u0442\u0443\u0440\u0431\u0438\u043d\u044b"), old.optString("turbine", selectedTurbine == null ? "" : selectedTurbine));
        EditText name = field(tr("\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435 \u0440\u0430\u0431\u043e\u0442\u044b"), old.optString("name"));
        EditText date = field(tr("\u0414\u0430\u0442\u0430 \u0440\u0430\u0431\u043e\u0442\u044b"), old.optString("date"));
        date.setFocusable(false);
        date.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) ->
                date.setText(String.format(Locale.US, "%02d.%02d.%04d", day, month + 1, year)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
        EditText description = field(tr("\u041e\u043f\u0438\u0441\u0430\u043d\u0438\u0435 \u0440\u0430\u0431\u043e\u0442\u044b"), old.optString("description"));
        description.setMinLines(3);
        description.setGravity(Gravity.TOP);
        button(tr("\ud83d\udcf7 \u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0444\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0438"), this::pickPhotos);
        TextView photoCount = text(tr("\u041f\u0440\u0438\u043a\u0440\u0435\u043f\u043b\u0435\u043d\u043e \u0444\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0439: ") + draftPhotos.size(), 16);
        layout.addView(photoCount);
        button(tr("\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440\u0435\u0442\u044c / \u0443\u0431\u0440\u0430\u0442\u044c \u0444\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0438"), () -> {
            if (draftPhotos.isEmpty()) { message(tr("\u0424\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0439 \u043f\u043e\u043a\u0430 \u043d\u0435\u0442")); return; }
            String[] items = new String[draftPhotos.size()];
            for (int i = 0; i < items.length; i++) items[i] = tr("\u0424\u043e\u0442\u043e ") + (i + 1);
            new AlertDialog.Builder(this).setTitle(tr("\u0424\u041e\u0422\u041e\u0413\u0420\u0410\u0424\u0418\u0418"))
                .setItems(items, (d, which) -> new AlertDialog.Builder(this)
                    .setTitle(tr("\u0424\u043e\u0442\u043e ") + (which + 1))
                    .setItems(new String[]{tr("\u041e\u0442\u043a\u0440\u044b\u0442\u044c"), tr("\u0423\u0431\u0440\u0430\u0442\u044c \u0438\u0437 \u0440\u0430\u0431\u043e\u0442\u044b")}, (dialog, action) -> {
                        if (action == 0) openPhoto(draftPhotos.get(which));
                        else {
                            draftPhotos.remove(which);
                            photoCount.setText(tr("\u041f\u0440\u0438\u043a\u0440\u0435\u043f\u043b\u0435\u043d\u043e \u0444\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0439: ") + draftPhotos.size());
                        }
                    }).show()).show();
        });
        button(tr("\u0421\u041e\u0425\u0420\u0410\u041d\u0418\u0422\u042c"), () -> {
            if (turbine.getText().toString().trim().isEmpty()
                || name.getText().toString().trim().isEmpty()) {
                message(tr("\u0417\u0430\u043f\u043e\u043b\u043d\u0438 \u0442\u0443\u0440\u0431\u0438\u043d\u0443 \u0438 \u043d\u0430\u0437\u0432\u0430\u043d\u0438\u0435 \u0440\u0430\u0431\u043e\u0442\u044b")); return;
            }
            try {
                JSONObject j = new JSONObject();
                j.put("turbine", turbine.getText().toString());
                j.put("name", name.getText().toString());
                j.put("date", date.getText().toString());
                j.put("description", description.getText().toString());
                j.put("done", old.optBoolean("done", false));
                if (old.optBoolean("done") && old.optLong("completedAt", 0) > 0) j.put("completedAt", old.optLong("completedAt"));
                if (old.optBoolean("done")) for (String k : new String[]{"completedBy", "completedById"}) if (old.has(k)) j.put(k, old.optString(k));
                for (String k : new String[]{"nc", "ncAt", "ncBy"}) if (old.has(k)) j.put(k, old.get(k));
                JSONArray array = new JSONArray();
                for (String uri : draftPhotos) array.put(uri);
                j.put("photos", array);
                if (index >= 0) jobs.set(index, j); else jobs.add(j);
                saveJobs();
                returnToJobs.run();
            } catch (Exception e) { message(tr("\u041e\u0448\u0438\u0431\u043a\u0430 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u044f")); }
        });
        button(tr("\u041e\u0422\u041c\u0415\u041d\u0410"), returnToJobs);
    }

    private void pickPhotos() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, PICK_PHOTOS);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == QR_SCAN_REQUEST) {
            com.google.zxing.integration.android.IntentResult result =
                com.google.zxing.integration.android.IntentIntegrator.parseActivityResult(resultCode, data);
            if (result != null && result.getContents() != null) handleSyncQr(result.getContents());
            return;
        }
        if (requestCode == 203) { if(resultCode == RESULT_OK) receiveNcPhotos(data); else { ncPhotoJob = null; ncPhotoBack = null; } return; }
        if (requestCode == 202 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try { getContentResolver().takePersistableUriPermission(data.getData(), Intent.FLAG_GRANT_READ_URI_PERMISSION); storage.edit().putString("profile_photo", data.getData().toString()).apply(); showTeam(); } catch (Exception e) { message("Photo: " + e.getMessage()); }
            return;
        }
        if (requestCode != PICK_PHOTOS || resultCode != RESULT_OK || data == null) return;
        int added = 0;
        if (data.getClipData() != null) {
            for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                if (rememberPhoto(data.getClipData().getItemAt(i).getUri(), data)) added++;
            }
        } else if (data.getData() != null) {
            if (rememberPhoto(data.getData(), data)) added++;
        }
        message(tr("\u0414\u043e\u0431\u0430\u0432\u043b\u0435\u043d\u043e \u0444\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0439: ") + added + tr(". \u041d\u0430\u0436\u043c\u0438 \u0421\u041e\u0425\u0420\u0410\u041d\u0418\u0422\u042c \u0434\u043b\u044f \u0437\u0430\u043f\u0438\u0441\u0438 \u0440\u0430\u0431\u043e\u0442\u044b."));
        // Rebuild the form only after save/cancel; selected photos remain in draftPhotos.
        // The counter on the current form updates when the user opens the photo list.
    }

    private boolean rememberPhoto(Uri uri, Intent result) {
        try {
            getContentResolver().takePersistableUriPermission(uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String value = uri.toString();
            if (!draftPhotos.contains(value)) { draftPhotos.add(value); return true; }
        } catch (SecurityException e) {
            message(tr("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0434\u043e\u0441\u0442\u0443\u043f \u043a \u0444\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0438"));
        }
        return false;
    }

    private JSONArray photos(JSONObject j) {
        JSONArray a = j.optJSONArray("photos");
        return a == null ? new JSONArray() : a;
    }

    private void showGallery() {
        page(tr("\u0424\u041e\u0422\u041e\u0413\u0420\u0410\u0424\u0418\u0418 GRID"), () -> { if (selectedTurbine != null) showTurbineJobs(selectedTurbine); else showHome(); });
        int count = 0;
        for (int i = 0; i < jobs.size(); i++) {
            JSONObject j = jobs.get(i);
            if (selectedTurbine != null && !selectedTurbine.equalsIgnoreCase(j.optString("turbine").trim())) continue;
            JSONArray arr = photos(j);
            if (arr.length() == 0) continue;
            layout.addView(text(j.optString("turbine") + tr(" \u2014 ") + j.optString("name"), 19));
            for (int k = 0; k < arr.length(); k++) {
                String uri = arr.optString(k);
                if (uri.isEmpty()) continue;
                count++;
                addThumbnail(uri);
            }
        }
        if (count == 0) layout.addView(text(tr("\u0424\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0439 \u043f\u043e\u043a\u0430 \u043d\u0435\u0442. \u0414\u043e\u0431\u0430\u0432\u044c \u0438\u0445 \u043a \u0440\u0430\u0431\u043e\u0442\u0435."), 17));
    }

    private void addThumbnail(String uri) {
        ImageView image = new ImageView(this);
        image.setAdjustViewBounds(true);
        image.setMaxHeight(480);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        try { image.setImageURI(Uri.parse(uri)); }
        catch (Exception e) { layout.addView(text(tr("\u0424\u043e\u0442\u043e \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u043d\u043e"), 14)); return; }
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, 420);
        p.setMargins(0, 8, 0, 12);
        layout.addView(image, p);
        image.setOnClickListener(v -> openPhoto(uri));
    }

    private void openPhoto(String uri) {
        ImageView image=new ImageView(this); image.setAdjustViewBounds(true);
        try { image.setImageURI(Uri.parse(uri)); } catch(Exception e) { message("Photo unavailable"); return; }
        new AlertDialog.Builder(this).setView(image).setPositiveButton("OK",null)
            .setNeutralButton(local("\u0421\u043a\u0430\u0447\u0430\u0442\u044c", "Save", "Speichern", "Saglab\u0101t"),(d,w)->savePhotoToGallery(uri)).show();
    }
    private void savePhotoToGallery(String uri) {
        new Thread(()-> {
            android.net.Uri target=null;
            try {
                if(android.os.Build.VERSION.SDK_INT<29) throw new java.io.IOException("Android 10+ required");
                byte[] bytes=photoBytes(uri);
                android.graphics.BitmapFactory.Options opts=new android.graphics.BitmapFactory.Options(); opts.inJustDecodeBounds=true;
                android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);
                String mime=opts.outMimeType; if(mime==null || !mime.startsWith("image/")) throw new java.io.IOException("Invalid image");
                String ext=android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime); if(ext==null) ext="img";
                android.content.ContentValues v=new android.content.ContentValues(); v.put("_display_name","GRID_"+System.currentTimeMillis()+"."+ext); v.put("mime_type",mime); v.put("relative_path","Pictures/GRID"); v.put("is_pending",1);
                target=getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v);
                if(target==null) throw new java.io.IOException("Save failed");
                try(java.io.OutputStream out=getContentResolver().openOutputStream(target)){if(out==null) throw new java.io.IOException("Save failed"); out.write(bytes);}
                v.clear(); v.put("is_pending",0); getContentResolver().update(target,v,null,null);
                runOnUiThread(()->message(local("\u0424\u043e\u0442\u043e \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u043e", "Photo saved", "Foto gespeichert", "Foto saglab\u0101ts")));
            } catch(Exception e){if(target!=null) try{getContentResolver().delete(target,null,null);}catch(Exception ignored){} runOnUiThread(()->message("Photo: "+e.getMessage()));}
        }).start();
    }

    private void jobOptions(int index) {
        Runnable returnToJobs = jobsReturn != null ? jobsReturn : this::showJobs;
        JSONObject j = jobs.get(index);
        String[] options = {tr("\u041f\u043e\u0441\u043c\u043e\u0442\u0440\u0435\u0442\u044c \u043e\u043f\u0438\u0441\u0430\u043d\u0438\u0435"), tr("\u0424\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0438 \u0440\u0430\u0431\u043e\u0442\u044b"), tr("\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u0442\u044c"),
            j.optBoolean("done") ? tr("\u0412\u0435\u0440\u043d\u0443\u0442\u044c \u0432 \u0440\u0430\u0431\u043e\u0442\u0443") : tr("\u041e\u0442\u043c\u0435\u0442\u0438\u0442\u044c \u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u043d\u043e\u0439"), tr("\u0423\u0434\u0430\u043b\u0438\u0442\u044c"), local("\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c / \u0438\u0437\u043c\u0435\u043d\u0438\u0442\u044c NC", "Add / edit NC", "NC hinzuf\u00fcgen / bearbeiten", "Pievienot / labot NC"), local("\u0424\u043e\u0442\u043e NC", "NC photos", "NC-Fotos", "NC foto")};
        new AlertDialog.Builder(this).setTitle(j.optString("name"))
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    new AlertDialog.Builder(this).setTitle(tr("\u041e\u043f\u0438\u0441\u0430\u043d\u0438\u0435"))
                        .setMessage(j.optString("description", "") + completionInfo(j) + ncInfo(j))
                        .setPositiveButton("OK", null).show();
                } else if (which == 1) {
                    page(tr("\u0424\u041e\u0422\u041e \u0420\u0410\u0411\u041e\u0422\u042b"), returnToJobs);
                    JSONArray a = photos(j);
                    if (a.length() == 0) layout.addView(text(tr("\u0424\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0439 \u043d\u0435\u0442"), 17));
                    for (int i = 0; i < a.length(); i++) addThumbnail(a.optString(i));
                } else if (which == 2) editJob(index);
                else if (which == 3) {
                    try {
                        if (hasNc(j)) { message(local("\u0421\u043d\u0430\u0447\u0430\u043b\u0430 \u0443\u0434\u0430\u043b\u0438\u0442\u0435 NC", "Clear NC first", "Zuerst NC l\u00f6schen", "Vispirms dz\u0113siet NC")); return; }
                        boolean completed = !j.optBoolean("done");
                        j.put("done", completed);
                        if (completed) { j.put("completedAt", System.currentTimeMillis()); j.put("completedBy", profileName()); j.put("completedById", profileId()); }
                        else { j.remove("completedAt"); j.remove("completedBy"); j.remove("completedById"); }
                        saveJobs(); returnToJobs.run();
                    } catch (Exception e) { message(tr("\u041e\u0448\u0438\u0431\u043a\u0430")); }
                } else if (which == 6) { if (hasNc(j)) showNcPhotos(j, returnToJobs); else editNc(j, returnToJobs); } else if (which == 5) { editNc(j, returnToJobs); } else {
                    new AlertDialog.Builder(this).setMessage(tr("\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u044d\u0442\u0443 \u0440\u0430\u0431\u043e\u0442\u0443?"))
                        .setNegativeButton(tr("\u041e\u0442\u043c\u0435\u043d\u0430"), null)
                        .setPositiveButton(tr("\u0423\u0434\u0430\u043b\u0438\u0442\u044c"), (d, w) -> {
                            jobs.remove(index); saveJobs(); returnToJobs.run();
                        }).show();
                }
            }).show();
    }

    private static final int QR_SCAN_REQUEST = 9041;
    private volatile java.net.ServerSocket syncServer;
    private boolean syncBusy;
    private static final int SYNC_PORT = 38766;
    private static final int SYNC_LIMIT = 32 * 1024 * 1024;
    private String syncText(String ru, String en, String de, String lv) { return local(ru,en,de,lv); }
    private void showSync() {
        page(local("\u0421\u0418\u041d\u0425\u0420\u041e\u041d\u0418\u0417\u0410\u0426\u0418\u042f", "SYNC", "SYNCHRONISIERUNG", "SINHRONIZ\u0100CIJA"), () -> { stopSync(); showHome(); });
        layout.addView(text(local("\u041e\u0434\u043d\u0430 \u0441\u0435\u0442\u044c Wi-Fi. \u0417\u0430\u0434\u0430\u043d\u0438\u044f \u0438 \u0444\u043e\u0442\u043e NC / \u0440\u0430\u0431\u043e\u0442. \u0422\u043e\u043b\u044c\u043a\u043e \u0434\u043e\u0432\u0435\u0440\u0435\u043d\u043d\u0430\u044f \u0441\u0435\u0442\u044c. \u041e\u0431\u043d\u043e\u0432\u0438\u0442\u0435 \u043e\u0431\u0430 \u0442\u0435\u043b\u0435\u0444\u043e\u043d\u0430.", "Same Wi-Fi. Jobs and NC / job photos. Trusted network only. Update both phones.", "Gleiches WLAN. Aufgaben und Fotos. Nur vertrauensw\u00fcrdiges Netz. Beide Ger\u00e4te aktualisieren.", "Viens Wi-Fi. Darbi un foto. Tikai uzticams t\u012bkls. Atjauniniet abus t\u0101lru\u0146us."), 15));
        button(local("\u0421\u043e\u0437\u0434\u0430\u0442\u044c \u0441\u0435\u0440\u0432\u0435\u0440", "Start server", "Server starten", "Izveidot serveri"), this::startSync);
        button(local("Сканировать QR-код", "Scan QR code", "QR-Code scannen", "Skenēt QR kodu"), this::scanSyncQr);
        button(local("\u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0438\u0442\u044c\u0441\u044f", "Connect", "Verbinden", "Piesl\u0113gties"), this::connectSync);
        button(local("\u041e\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c \u0441\u0435\u0440\u0432\u0435\u0440", "Stop server", "Server stoppen", "Aptur\u0113t serveri"), () -> { stopSync(); showSync(); });
    }
    private void stopSync() {
        java.net.ServerSocket s = syncServer; syncServer = null;
        if (s != null) try { s.close(); } catch (Exception ignored) {}
    }
    @Override protected void onDestroy() { stopWeather(); stopSync(); super.onDestroy(); }
    private String jobKey(JSONObject j) {
        return j.optString("turbine").trim().toLowerCase(Locale.ROOT) + "\n" + j.optString("name").trim().toLowerCase(Locale.ROOT);
    }
    private byte[] photoBytes(String uri) throws Exception {
        try (java.io.InputStream in = getContentResolver().openInputStream(Uri.parse(uri)); java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            if (in == null) throw new java.io.IOException("Photo unavailable");
            byte[] buf = new byte[8192]; int n;
            while ((n=in.read(buf)) != -1) { if(out.size()+n>12*1024*1024) throw new java.io.IOException("Photo exceeds 12 MB"); out.write(buf,0,n); }
            return out.toByteArray();
        }
    }
    private String photoHash(byte[] bytes) throws Exception {
        byte[] hash=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder b=new StringBuilder(); for(byte x:hash) b.append(String.format(Locale.ROOT,"%02x",x & 255)); return b.toString();
    }
    private JSONArray syncSnapshot() throws Exception {
        return new JSONArray(new JSONArray(jobs).toString());
    }
    private String photoPacket(JSONArray a) throws Exception {
        int total=0;
        for(int i=0;i<a.length();i++) {
            JSONObject j=a.getJSONObject(i);
            for(String key:new String[]{"photos","ncPhotos"}) {
                JSONArray src=j.optJSONArray(key), dst=new JSONArray();
                if(src!=null) for(int k=0;k<src.length();k++) {
                    byte[] bytes=photoBytes(src.getString(k)); total+=bytes.length;
                    if(total>20*1024*1024) throw new java.io.IOException("Photo exchange exceeds 20 MB");
                    dst.put(android.util.Base64.encodeToString(bytes,android.util.Base64.NO_WRAP));
                }
                j.put(key,dst);
            }
        }
        return a.toString();
    }
    private JSONArray importPhotos(JSONArray a) throws Exception {
        java.io.File dir=new java.io.File(getFilesDir(),"synced_photos");
        if(!dir.isDirectory() && !dir.mkdirs()) throw new java.io.IOException("Photo folder unavailable");
        for(int i=0;i<a.length();i++) for(String key:new String[]{"photos","ncPhotos"}) {
            JSONObject j=a.getJSONObject(i); JSONArray src=j.optJSONArray(key), dst=new JSONArray();
            if(src!=null) for(int k=0;k<src.length();k++) {
                byte[] bytes=android.util.Base64.decode(src.getString(k),android.util.Base64.DEFAULT);
                if(bytes.length>12*1024*1024) throw new java.io.IOException("Photo too large");
                android.graphics.BitmapFactory.Options opts=new android.graphics.BitmapFactory.Options(); opts.inJustDecodeBounds=true;
                android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.length,opts);
                if(opts.outWidth<=0 || opts.outHeight<=0) throw new java.io.IOException("Invalid image");
                java.io.File f=new java.io.File(dir,photoHash(bytes));
                if(!f.exists()) { java.io.File tmp=java.io.File.createTempFile("photo", ".tmp",dir); try { try(java.io.FileOutputStream out=new java.io.FileOutputStream(tmp)){out.write(bytes);} if(!tmp.renameTo(f)) throw new java.io.IOException("Photo save failed"); } finally {tmp.delete();} }
                dst.put(Uri.fromFile(f).toString());
            }
            j.put(key,dst);
        }
        return a;
    }
    private void mergePhotos(JSONObject l, JSONObject r, String key) throws Exception {
        JSONArray a=l.optJSONArray(key), b=r.optJSONArray(key), result=new JSONArray();
        java.util.HashSet<String> seen=new java.util.HashSet<>();
        for(JSONArray list:new JSONArray[]{a,b}) if(list!=null) for(int i=0;i<list.length();i++) {
            String uri=list.getString(i), id;
            try { id=photoHash(photoBytes(uri)); } catch(Exception e) { id=uri; }
            if(seen.add(id)) result.put(uri);
        }
        l.put(key,result);
    }
    private int mergeSync(JSONArray incoming) throws Exception {
        JSONArray backup = new JSONArray(); for (JSONObject j : jobs) backup.put(j);
        ArrayList<JSONObject> next = new ArrayList<>();
        for (JSONObject j : jobs) next.add(new JSONObject(j.toString()));
        java.util.HashMap<String, JSONObject> index = new java.util.HashMap<>();
        for (JSONObject j : next) index.put(jobKey(j), j);
        int conflicts = 0;
        for (int i=0; i<incoming.length(); i++) {
            JSONObject r = incoming.getJSONObject(i);
            if (r.optString("turbine").trim().isEmpty() || r.optString("name").trim().isEmpty()) throw new java.io.IOException("Invalid job");
            JSONObject l = index.get(jobKey(r));
            if (l == null) {
                JSONObject n = new JSONObject();
                for (String k : new String[]{"turbine","name","date","description"}) n.put(k,r.optString(k));
                n.put("done",r.optBoolean("done")); if (n.optBoolean("done") && r.optLong("completedAt",0)>0) n.put("completedAt",r.optLong("completedAt")); if (n.optBoolean("done")) for (String k : new String[]{"completedBy","completedById"}) if (r.has(k)) n.put(k,r.optString(k)); for (String k : new String[]{"nc","ncAt","ncBy"}) if (r.has(k)) n.put(k,r.get(k)); if (hasNc(n)) { n.put("done",false); n.remove("completedAt"); n.remove("completedBy"); n.remove("completedById"); } mergePhotos(n,r,"photos"); if(hasNc(n)) mergePhotos(n,r,"ncPhotos"); next.add(n); index.put(jobKey(n),n);
            } else {
                if (r.optLong("ncAt",0) > l.optLong("ncAt",0)) {
                    if (!r.optString("nc").equals(l.optString("nc"))) l.remove("ncPhotos");
                    for (String k : new String[]{"nc","ncAt","ncBy"}) if (r.has(k)) l.put(k,r.get(k));
                    if (hasNc(l)) { l.put("done",false); l.remove("completedAt"); l.remove("completedBy"); l.remove("completedById"); }
                } else if (r.optLong("ncAt",0) == l.optLong("ncAt",0) && !r.optString("nc").equals(l.optString("nc"))) conflicts++;
                mergePhotos(l,r,"photos");
                if(hasNc(l) && l.optString("nc").equals(r.optString("nc"))) mergePhotos(l,r,"ncPhotos");
                boolean different = l.optBoolean("done") != r.optBoolean("done") || !l.optString("date").equals(r.optString("date")) || !l.optString("description").equals(r.optString("description"));
                if (different || (l.optBoolean("done") && r.optBoolean("done") && l.optLong("completedAt",0)>0 && r.optLong("completedAt",0)>0 && l.optLong("completedAt",0)!=r.optLong("completedAt",0))) conflicts++;
                else if (l.optBoolean("done")) {
                    if (!l.optString("completedBy").isEmpty() && !r.optString("completedBy").isEmpty() && (!l.optString("completedById").equals(r.optString("completedById")) || !l.optString("completedBy").equals(r.optString("completedBy")))) conflicts++;
                    else {
                        if (l.optLong("completedAt",0)<=0 && r.optLong("completedAt",0)>0) l.put("completedAt",r.optLong("completedAt"));
                        if (l.optString("completedBy").isEmpty() && l.optLong("completedAt",0)==r.optLong("completedAt",0)) for (String k : new String[]{"completedBy","completedById"}) if (r.has(k)) l.put(k,r.optString(k));
                    }
                }
            }
        }
        if (!storage.edit().putString("sync_backup",backup.toString()).putString("sync_incoming",incoming.toString()).putString("jobs",new JSONArray(next).toString()).commit()) throw new java.io.IOException("Save failed");
        jobs.clear(); jobs.addAll(next); return conflicts;
    }
    private void syncResult(JSONArray incoming) {
        try { int n = mergeSync(incoming); message(local("\u041e\u0431\u043c\u0435\u043d \u0437\u0430\u0432\u0435\u0440\u0448\u0451\u043d. \u041a\u043e\u043d\u0444\u043b\u0438\u043a\u0442\u044b: ", "Exchange complete. Conflicts: ", "Austausch fertig. Konflikte: ", "Apmai\u0146a pabeigta. Konflikti: ") + n); }
        catch(Exception e) { message("Sync: " + e.getMessage()); }
    }
    private void writeSync(java.io.DataOutputStream out, String s) throws Exception {
        byte[] b=s.getBytes(java.nio.charset.StandardCharsets.UTF_8); if(b.length>SYNC_LIMIT) throw new java.io.IOException("Too large"); out.writeInt(b.length); out.write(b); out.flush();
    }
    private String readSync(java.io.DataInputStream in) throws Exception {
        int n=in.readInt(); if(n<0 || n>SYNC_LIMIT) throw new java.io.IOException("Invalid size"); byte[] b=new byte[n]; in.readFully(b); return new String(b,java.nio.charset.StandardCharsets.UTF_8);
    }
    private String syncAddresses() throws Exception {
        StringBuilder b=new StringBuilder(); java.util.Enumeration<java.net.NetworkInterface> ns=java.net.NetworkInterface.getNetworkInterfaces();
        while(ns.hasMoreElements()) { java.net.NetworkInterface ni=ns.nextElement(); if(!ni.isUp() || ni.isLoopback()) continue; java.util.Enumeration<java.net.InetAddress> as=ni.getInetAddresses(); while(as.hasMoreElements()) { java.net.InetAddress a=as.nextElement(); if(a instanceof java.net.Inet4Address && a.isSiteLocalAddress()) b.append(a.getHostAddress()).append("\n"); }} return b.toString();
    }
    private void startSync() {
        if(syncBusy || syncServer!=null) return;
        try {
            final JSONArray snapshot=syncSnapshot();
            final String pin=String.format(Locale.US,"%08d",new java.security.SecureRandom().nextInt(100000000));
            final java.net.ServerSocket server=new java.net.ServerSocket(); server.bind(new java.net.InetSocketAddress(SYNC_PORT)); server.setSoTimeout(120000); syncServer=server;
            String[] addresses = syncAddresses().trim().split("\\s+");
            if (addresses.length == 0 || addresses[0].isEmpty()) throw new java.io.IOException("No local Wi-Fi IP");
            String host = addresses[0];
            for (String address : addresses) if (address.startsWith("192.168.") || address.startsWith("10.")) { host = address; break; }
            String payload = "grid://sync?ip=" + host + "&pin=" + pin;
            showSyncQr(payload, host, pin);
            new Thread(() -> {
                try (java.net.ServerSocket ss=server; java.net.Socket socket=ss.accept()) {
                    socket.setSoTimeout(120000);
                    java.io.DataInputStream in=new java.io.DataInputStream(socket.getInputStream()); java.io.DataOutputStream out=new java.io.DataOutputStream(socket.getOutputStream());
                    if(!pin.equals(readSync(in))) throw new java.io.IOException("Wrong PIN");
                    JSONArray incoming=importPhotos(new JSONArray(readSync(in))); writeSync(out,photoPacket(snapshot));
                    runOnUiThread(() -> { if(syncServer==server) syncResult(incoming); });
                } catch(Exception e) { runOnUiThread(() -> { if(syncServer==server) message("Sync: "+e.getMessage()); }); }
                finally { runOnUiThread(() -> { if(syncServer==server) syncServer=null; }); }
            }).start();
        } catch(Exception e) { stopSync(); message("Sync: "+e.getMessage()); }
    }
    private void showSyncQr(String payload, String host, String pin) throws Exception {
        com.google.zxing.common.BitMatrix matrix = new com.google.zxing.qrcode.QRCodeWriter()
            .encode(payload, com.google.zxing.BarcodeFormat.QR_CODE, 640, 640);
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(640, 640, android.graphics.Bitmap.Config.ARGB_8888);
        for (int y = 0; y < 640; y++) for (int x = 0; x < 640; x++)
            bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
        ImageView image = new ImageView(this);
        image.setImageBitmap(bitmap);
        image.setAdjustViewBounds(true);
        image.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.addView(image, new LinearLayout.LayoutParams(-1, dp(320)));
        TextView detail = new TextView(this);
        detail.setText("IP: " + host + "\nPIN: " + pin + "\n120 sec");
        detail.setTextColor(Color.BLACK);
        detail.setGravity(Gravity.CENTER);
        detail.setTextSize(16);
        content.addView(detail);
        new AlertDialog.Builder(this).setTitle("Wi-Fi GRID — QR")
            .setView(content).setPositiveButton("OK", null).show();
    }
    private void scanSyncQr() {
        try {
            com.google.zxing.integration.android.IntentIntegrator scanner =
                new com.google.zxing.integration.android.IntentIntegrator(this);
            scanner.setDesiredBarcodeFormats(com.google.zxing.integration.android.IntentIntegrator.QR_CODE);
            scanner.setPrompt(local("Наведите камеру на QR GRID", "Scan GRID QR code", "GRID QR-Code scannen", "Skenē GRID QR kodu"));
            scanner.setBeepEnabled(false);
            scanner.setOrientationLocked(false);
            scanner.setRequestCode(QR_SCAN_REQUEST);
            scanner.initiateScan();
        } catch (Exception e) { message("QR: " + e.getMessage()); }
    }
    private void handleSyncQr(String contents) {
        try {
            Uri uri = Uri.parse(contents);
            if (!"grid".equals(uri.getScheme()) || !"sync".equals(uri.getHost()))
                throw new IllegalArgumentException("Not a GRID QR code");
            String host = uri.getQueryParameter("ip"), pin = uri.getQueryParameter("pin");
            if (host == null || !host.matches("[0-9.]{7,15}") || pin == null || !pin.matches("[0-9]{8}"))
                throw new IllegalArgumentException("Invalid GRID QR code");
            connectSyncTo(host, pin);
        } catch (Exception e) { message("QR: " + e.getMessage()); }
    }
    private void connectSync() {
        if(syncBusy || syncServer!=null) return;
        LinearLayout form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        EditText ip=new EditText(this); ip.setHint("IP (192.168.x.x)"); ip.setSingleLine(true); form.addView(ip);
        EditText pin=new EditText(this); pin.setHint("PIN"); pin.setInputType(2); form.addView(pin);
        new AlertDialog.Builder(this).setTitle("Wi-Fi GRID").setView(form).setNegativeButton(local("\u041e\u0442\u043c\u0435\u043d\u0430","Cancel","Abbrechen","Atcelt"),null).setPositiveButton("OK",(d,w)-> {
            final String host=ip.getText().toString().trim(), code=pin.getText().toString().trim();
            if(!host.matches("[0-9.]{7,15}") || !code.matches("[0-9]{8}")) { message("Invalid IP / PIN"); return; }
            connectSyncTo(host, code);
        }).show();
    }
    private void connectSyncTo(String host, String code) {
            if(syncBusy || syncServer!=null) { message("Sync busy"); return; }
            try {
                final JSONArray snapshot=syncSnapshot(); syncBusy=true;
                new Thread(()-> {
                    try(java.net.Socket s=new java.net.Socket()) {
                        java.net.InetAddress addr=java.net.InetAddress.getByName(host); if(!addr.isSiteLocalAddress()) throw new java.io.IOException("Local IP only");
                        s.connect(new java.net.InetSocketAddress(addr,SYNC_PORT),5000); s.setSoTimeout(120000);
                        java.io.DataOutputStream out=new java.io.DataOutputStream(s.getOutputStream()); java.io.DataInputStream in=new java.io.DataInputStream(s.getInputStream());
                        writeSync(out,code); writeSync(out,photoPacket(snapshot)); JSONArray incoming=importPhotos(new JSONArray(readSync(in))); runOnUiThread(()->syncResult(incoming));
                    } catch(Exception e) { runOnUiThread(()->message("Sync: "+e.getMessage())); }
                    finally { runOnUiThread(()->syncBusy=false); }
                }).start();
            } catch(Exception e) { syncBusy=false; message("Sync: "+e.getMessage()); }
     }
    private void saveJobs() {
        JSONArray a = new JSONArray();
        for (JSONObject j : jobs) a.put(j);
        storage.edit().putString("jobs", a.toString()).apply();
    }

    private void loadJobs() {
        try {
            JSONArray a = new JSONArray(storage.getString("jobs", "[]"));
            for (int i = 0; i < a.length(); i++) jobs.add(a.getJSONObject(i));
        } catch (Exception e) { message(tr("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c \u0440\u0430\u0431\u043e\u0442\u044b")); }
    }

    private void message(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private static final String[][] TEMPLATE = {
        {"Material Check", "Checking Lift Material Conteiner"},
        {"Material Check", "Checking Grid connection Cointainer"},
        {"Material Check", "Checkink Tower Base container"},
        {"Material Check", "Checking Tower Base material"},
        {"Preperation of S3", "Installation of Black lift cable"},
        {"Preperation of S3", "Install Lift electrical box"},
        {"Preperation of S3", "Install lift magnet (Top side)"},
        {"Preperation of S3", "Secure lift magnet (concrete)"},
        {"Preperation of S3", "Secure Lift emergency descent cable"},
        {"Preperation of S3", "Secure Lift power cable"},
        {"Preperation of S3", "Secure Lift plate + 2bolts washers"},
        {"Preperation of S3", "Secure guide wire schacles"},
        {"Preperation of S3", "Secure 2 rubber protection elements"},
        {"Preperation of S3", "Secure 3x Large, 2x small zip ties, garbage bag"},
        {"Preperation of S3", "Secure Interlock (Concrete)"},
        {"Preperation of S3", "Secure Interlock (Top side)"},
        {"Preperation of S3", "Organise cables, not to be in the way in tray"},
        {"Preperation of S2", "Organise cables, not to be in the way in tray"},
        {"Preperation of S2", "Sedure garbage bag, big zip ties"},
        {"Preperation of S2", "Install lift magnet (Top side)"},
        {"Preperation of S2", "Secure Interlock (Top side)"},
        {"Preperation of S2", "Secure Big banana (top side) +4x M12 Bolts washers"},
        {"Preperation of S2", "Secure slider (top side) + 2x M12 bolts washers"},
        {"Preperation of S1", "Organise cables, not to be in the way"},
        {"Preperation of S1", "Cut cable tray for small banana"},
        {"Preperation of S1", "Check bolt holes for small banana (drill if necessary)"},
        {"Preperation of S1", "Drill bolt holes for Slider"},
        {"Preperation of S1", "Check for Ring bolts, drill if necessary"},
        {"Preperation of S1", "Secure small banana"},
        {"Preperation of S1", "Secure two yellow cables for Nacelle"},
        {"Preperation of S1", "Secure power cable for Vincha"},
        {"Preperation of S1", "Secure vincha + 3m Sling"},
        {"Preperation of S1", "Secure rope for vincha + metal part+ swiwel schackle"},
        {"Preperation of S1", "Install Lift schacles"},
        {"Preperation of S1", "Secure lift Stopping plate"},
        {"Preperation of S1", "Secure PE cables for Lift zail + longer bolts"},
        {"Preperation of S1", "Check Nuts for I-beam"},
        {"Preperation of S1", "Secure Interlock"},
        {"Preperation of S1", "Secure tover oscilation sensor cable"},
        {"Preperation of S1", "Assambe Rings"},
        {"Preperation of S1", "Secure rings in S1"},
        {"Nacelle preperations", "Check inside of Nacelle, missing parts etc"},
        {"Nacelle preperations", "Put 6x Rollers for MV"},
        {"Nacelle preperations", "Put 3x Guide rollers for metal zail"},
        {"Nacelle preperations", "Put Frame for MV cable + swivel schacle"},
        {"Nacelle preperations", "Put 3x Orange rollers"},
        {"Nacelle preperations", "Put Big gofra 8m"},
        {"Nacelle preperations", "Put medium gofra 2m"},
        {"Nacelle preperations", "Put small gofra 8m"},
        {"Nacelle preperations", "Put 3x 1m slings"},
        {"Nacelle preperations", "Put 2x 2m slings"},
        {"Nacelle preperations", "Put kettencuk (large and small) + 3m sling"},
        {"Nacelle preperations", "Put large Roller for lovering cables"},
        {"Nacelle preperations", "Put Optical cable"},
        {"Nacelle preperations", "Test Nacelle (YAW)"},
        {"Nacelle preperations", "Put tool for Blade heating cable connections"},
        {"Nacelle preperations", "Install Weatherstation"},
        {"Nacelle preperations", "Check Weatherstation"},
        {"Nacelle preperations", "Put connection elememt for vincha"},
        {"Nacelle preperations", "Put 3x rubber pads"},
        {"Nacelle preperations", "Put toolbox GRID (Check for all tools before)"},
        {"Nacelle preperations", "Put Ventilation Gofra + Rings"},
        {"Nacelle preperations", "Put MV cable Nacelle Connections"},
        {"Powering PSP", "Connect Generator"},
        {"Powering PSP", "Connect PE cable"},
        {"Tower Base", "Put all material in Tower Base"},
        {"Tower Base", "Remove Plastic from E-Module"},
        {"Tower Base", "Fence Installation"},
        {"Tower Base", "Door lock installation"},
        {"Tower Base", "Connect E-module PE cables"},
        {"Tower Base", "Operation Panel installation +PE cable"},
        {"Tower Base", "Cable guide rope installation"},
        {"Tower Base", "Cable tray installation"},
        {"Tower Base", "Entrance sighn post installation +PE cable"},
        {"Tower Base", "Light installation (on fence)"},
        {"Tower Base", "Cable guiding, zip tie (tower lights, Tower power,)"},
        {"Tower Base", "E-module cable installation (green cable, signal cable)"},
        {"Tower Base", "Operation panel and main electrical cabinet cable installation"},
        {"Tower Base", "Smoke detector installation, cabling"},
        {"Tower Base", "Signal lamp installation, cabling"},
        {"Tower Base", "Medicine box installation"},
        {"Tower Base", "Exterrior light installation, cable management"},
        {"Tower Base", "Anchor bolt installation on E-Module"},
        {"Tower Base", "Installing Support beams for MV cable"},
        {"Tower Base", "Opening E-module for MV connection"},
        {"Tower Base", "Pre-install Lift support frame"},
        {"Tower Base", "Drill Lift support frame holes"},
        {"Tower Base", "Install Interlock"},
        {"Tower Base", "Remove smaller part of the gate (so we can push lift through)"},
        {"Tower Base", "Get lift on the sliding platform"},
        {"Tower Base", "Connect Lift bottom plate sensor"},
        {"Tower Base", "Connect lift Guide roller for Black cable"},
        {"Tower Base", "Install Top sensor on Lift"},
        {"Tower Base", "Install all labels inside lift"},
        {"Tower Base", "Remove 4 rollers for lift"},
        {"Tower Base", "Prepare lift zail for lifting up"},
        {"Tower Base", "Put lift zail documentation in lift book"},
        {"Tower Base", "Pull UP lift zails"},
        {"Tower Base", "Push Lift in Position"},
        {"Tower Base", "Connect Lift Power cable (after lowered from S3)"},
        {"Tower Base", "Install Lift zails"},
        {"Tower Base", "Connect Nacelle Power cable (after lowered from S3)"},
        {"Tower Base", "Connect Lift power cable and lift energency descend cable"},
        {"Tower Base", "Connect Lift safety cable"},
        {"Tower Base", "Connect Lift frame PE cable"},
        {"Tower Base", "Put gofra on cables going to E-module"},
        {"Tower Base", "Guide Cables coming from Nacelle in cable tray, put them in gofra"},
        {"Tower Base", "Do electrical measurements in Tower Base"},
        {"Tower Base", "Connect MV Cable to torpedo"},
        {"Tower Base", "Connect MV Cable to E-Module"},
        {"Tower Base", "Prepare MV cable clamps in Tower base, glue washers etc"},
        {"Tower Base", "Clean Tower Base"},
        {"Tower Base", "Organise all tools, put unnecessery tools in pallet"},
        {"Tower Base", "Pull up Interlock cable, connect interlock, put on small gofra"},
        {"ON S3 WHEN LIFTING", "Make power connections for S2"},
        {"ON S3 WHEN LIFTING", "Connect Tower lights"},
        {"ON S3 WHEN LIFTING", "Organise cables neatley"},
        {"ON S3 WHEN LIFTING", "Connect PE cables, mark them, RIVOLTA"},
        {"ON S3 WHEN LIFTING", "Install lift plate"},
        {"ON S3 WHEN LIFTING", "Lower down Lift power, black, nacelle power, emergency descent cables"},
        {"ON S3 WHEN LIFTING", "Install Interlock"},
        {"ON S2 WHEN LIFTING", "Make power connection for S1"},
        {"ON S2 WHEN LIFTING", "Connect Tower lights"},
        {"ON S2 WHEN LIFTING", "Organise cables neatley"},
        {"ON S2 WHEN LIFTING", "Connect interlock cables (on tray)"},
        {"ON S2 WHEN LIFTING", "Connect PE cables, mark them, RIVOLTA"},
        {"ON S2 WHEN LIFTING", "Install Interlock"},
        {"ON S1 WHEN LIFTING", "Make power connections for Nacelle"},
        {"ON S1 WHEN LIFTING", "Connect Tower light cables"},
        {"ON S1 WHEN LIFTING", "Connect Interlock cables (On tray)"},
        {"ON S1 WHEN LIFTING", "Connect PE cables, mark them, RIVOLTA"},
        {"ON S1 WHEN LIFTING", "Install Interlock"},
        {"ON TOP FLOOR WHEN LIFTING", "Install Interlock"},
        {"ON TOP FLOOR WHEN LIFTING", "Connect interlock"},
        {"ON TOP FLOOR WHEN LIFTING", "Connect Lift zails"},
        {"OTHER WORKS IN TOWER", "Connect interlock to cables on TOP floor"},
        {"OTHER WORKS IN TOWER", "Connect interlock to cables on S1"},
        {"OTHER WORKS IN TOWER", "Connect interlock to cables on S2"},
        {"OTHER WORKS IN TOWER", "Connect interlock to cables on S3"},
        {"OTHER WORKS IN TOWER", "Lower Tower oscilation cable from S1"},
        {"OTHER WORKS IN TOWER", "Put necessary labels on each floor"},
        {"OTHER WORKS IN TOWER", "Put cable protection on each floor where necessary"},
        {"OTHER WORKS IN TOWER", "Install Rubber On S2, S3 for MV cable"},
        {"OTHER WORKS IN TOWER", "Make electrical measurements"},
        {"OTHER WORKS IN TOWER", "Put clamps for MV cable"},
        {"OTHER WORKS IN TOWER", "Install big banana +rubber"},
        {"OTHER WORKS IN TOWER", "Organise cable lenghts in small banana"},
        {"OTHER WORKS IN TOWER", "Put Zip ties for all cables"},
        {"OTHER WORKS IN TOWER", "Cut all zip ties"},
        {"OTHER WORKS IN TOWER", "Install Small banana"},
        {"OTHER WORKS IN TOWER", "Install Slider"},
        {"WHEN NACELLE AND GENO IS UP", "Install ventilation gofra"},
        {"WHEN NACELLE AND GENO IS UP", "Make Geno cable connections"},
        {"WHEN NACELLE AND GENO IS UP", "Organise, comnnect PE and 12 pin cable near geno"},
        {"WHEN NACELLE AND GENO IS UP", "Make Stator cable connections"},
        {"WHEN NACELLE AND GENO IS UP", "Mark, close the floor for stator cables"},
        {"WHEN NACELLE AND GENO IS UP", "Guide yellow lNabe ight cable to Geno"},
        {"WHEN NACELLE AND GENO IS UP", "Install rollers for MV cable"},
        {"WHEN NACELLE AND GENO IS UP", "Remove floor, install all big rollers for MV cable"},
        {"WHEN NACELLE AND GENO IS UP", "Open transformator, prepare everything for MV pulling"},
        {"WHEN NABE IS UP", "Make Pitch cable connections"},
        {"WHEN NABE IS UP", "Make 45cm PE cable for nabe door, connect it"},
        {"WHEN NABE IS UP", "Make Blade heating cable connections"},
        {"WHEN NABE IS UP", "Connect Oil heating, temperature sensors and all the rest cables under the podest on the way to nabe"},
        {"WHEN NABE IS UP", "Test if pitch is working"},
        {"WHEN NABE IS UP", "Pull in cables from NABE"},
        {"WHEN NABE IS UP", "Connect 6pin cable inside geno"},
        {"WHEN NABE IS UP", "Connect 24 pin cable inside geno"},
        {"WHEN NABE IS UP", "Place silicon where necessary in geno"},
        {"WHEN NABE IS UP", "Connect magnetic belt sensor inside geno"},
        {"WHEN NABE IS UP", "Install Magnetic belt sensor cover in geno"},
        {"WHEN BLADES ARE UP", "Connect blade heating cables, PE, Metal rope and sensor cables for A Blade"},
        {"WHEN BLADES ARE UP", "Connect blade heating cables, PE, Metal rope and sensor cables for B Blade"},
        {"WHEN BLADES ARE UP", "Connect blade heating cables, PE, Metal rope and sensor cables for C Blade"},
        {"WHEN BLADES ARE UP", "Connect Blade heating for schleifring"},
        {"WHEN BLADES ARE UP", "Make electrical measurements in Blades"},
        {"MV CABLE PULLING", "Lift Vincha to Nacelle"},
        {"MV CABLE PULLING", "Lift Zail to Nacelle"},
        {"MV CABLE PULLING", "Open Podests and install rollers"},
        {"MV CABLE PULLING", "Place vincha and zail in position"},
        {"MV CABLE PULLING", "Position banana in position"},
        {"MV CABLE PULLING", "Install Kettencuk + 3m sling"},
        {"MV CABLE PULLING", "Put Nacelle in Correct position"},
        {"MV CABLE PULLING", "Check again if all preperations are done for vincha and zail"},
        {"MV CABLE PULLING", "Pulling MV Cable"},
        {"MV CABLE PULLING", "Remove rollers, cables, kettencuk etc"},
        {"MV CABLE PULLING", "SET ZERO POINT in HMI"},
        {"MV CABLE PULLING", "Finishing Clamps in Nacelle"},
        {"MV CABLE PULLING", "Putting in bag all tools necessary for MV cable pulling"},
        {"MV CABLE PULLING", "Organising zail in drum"},
        {"MV CABLE PULLING", "Sending down zail, vincha and tool bag"},
        {"MV CABLE PULLING", "closing podests, floors"},
        {"MV CABLE PULLING", "Positioning Banana back in place"},
        {"MV CABLE PULLING", "Install Rubber on Sock under Nacelle"},
        {"MV CABLE PULLING", "Making pictures for protocol"},
        {"OTHER WORKS IN NABE", "Install NABE Lights"},
        {"OTHER WORKS IN NABE", "Install Gofra for Schleifring cables, lights"},
        {"OTHER WORKS IN NABE", "Organise cables next to the doors, going to nabe"},
        {"OTHER WORKS IN NABE", "Check all works in NABE are done"},
        {"OTHER WORKS IN NABE", "Bolt Pitch remote connector to nabe door element"},
        {"OTHER WORKS IN NABE", "Bolt Nabe light connector to door element"},
        {"OTHER WORKS IN NACELLE", "Connect MV cable to transformer"},
        {"OTHER WORKS IN NACELLE", "Checking all clamps, loctite etc"},
        {"OTHER WORKS IN NACELLE", "Closing covers, puting on label, making photo"},
        {"OTHER WORKS IN NACELLE", "Install 3x light sensors in nacelle"},
        {"OTHER WORKS IN NACELLE", "Install 2x fire extinquishers"},
        {"LOWERING CABLES", "LOWERING CABLES"},
        {"LAST CHECKS", "Nacelle MV clamps loctite"},
        {"LAST CHECKS", "Electrical tape on MV connection in Nacelle"},
        {"LAST CHECKS", "Rivolta and markers on PE connections"},
        {"LAST CHECKS", "Cable management under Weatherstation"},
        {"LAST CHECKS", "Uncut zip ties in Nacelle"},
        {"LAST CHECKS", "Places where need to install cable protectors"},
        {"LAST CHECKS", "Light cables for tower lights in tower"},
        {"LAST CHECKS", "If tower base is clean"},
        {"LAST CHECKS", "Do this cheks at least two person"}
    };
    private void importTemplate() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(tr("\u041d\u043e\u043c\u0435\u0440 \u0442\u0443\u0440\u0431\u0438\u043d\u044b"));
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(tr("\u0428\u0410\u0411\u041b\u041e\u041d \u0420\u0410\u0411\u041e\u0422"))
            .setMessage(tr("\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u043d\u043e\u0432\u044b\u0439 \u0441\u043f\u0438\u0441\u043e\u043a \u0440\u0430\u0431\u043e\u0442 \u0431\u0435\u0437 \u043e\u0442\u043c\u0435\u0442\u043e\u043a \u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u0438\u044f?"))
            .setView(input).setNegativeButton(tr("\u041e\u0442\u043c\u0435\u043d\u0430"), null)
            .setPositiveButton(tr("\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0448\u0430\u0431\u043b\u043e\u043d"), null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String turbine = input.getText().toString().trim();
            if (turbine.isEmpty()) { input.setError(tr("\u041d\u043e\u043c\u0435\u0440 \u0442\u0443\u0440\u0431\u0438\u043d\u044b")); return; }
            for (JSONObject job : jobs) {
                if (turbine.equalsIgnoreCase(job.optString("turbine").trim())) {
                    input.setError(tr("\u0414\u043b\u044f \u044d\u0442\u043e\u0439 \u0442\u0443\u0440\u0431\u0438\u043d\u044b \u0443\u0436\u0435 \u0435\u0441\u0442\u044c \u0440\u0430\u0431\u043e\u0442\u044b")); return;
                }
            }
            ArrayList<JSONObject> added = new ArrayList<>();
            try {
                for (String[] row : TEMPLATE) {
                    JSONObject job = new JSONObject();
                    job.put("turbine", turbine);
                    job.put("name", row[0] + " \u2014 " + row[1]);
                    job.put("description", row[1]);
                    job.put("date", "");
                    job.put("done", false);
                    job.put("photos", new JSONArray());
                    added.add(job);
                }
            } catch (Exception e) { message(tr("\u041e\u0448\u0438\u0431\u043a\u0430")); return; }
            jobs.addAll(added);
            saveJobs();
            dialog.dismiss();
            showTurbines();
        }));
        dialog.show();
    }

    private void chooseLanguage() {
        String[] names = {"\ud83c\uddf7\ud83c\uddfa \u0420\u0443\u0441\u0441\u043a\u0438\u0439", "\ud83c\uddec\ud83c\udde7 English", "\ud83c\udde9\ud83c\uddea Deutsch", "\ud83c\uddf1\ud83c\uddfb Latvie\u0161u"};
        String[] codes = {"ru", "en", "de", "lv"};
        int selected = 0;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(language)) selected = i;
        }
        new AlertDialog.Builder(this)
            .setTitle(tr("\u042f\u0417\u042b\u041a / LANGUAGE"))
            .setSingleChoiceItems(names, selected, (dialog, which) -> {
                language = codes[which];
                storage.edit().putString("language", language).apply();
                dialog.dismiss();
                if (profileName().isEmpty()) showProfile(true); else showHome();
            })
            .setNegativeButton(tr("\u041e\u0442\u043c\u0435\u043d\u0430"), null)
            .show();
    }

    private String tr(String key) {
        String[] values = TRANSLATIONS.get(key);
        if (values == null) return key;
        int index = "en".equals(language) ? 0 : "de".equals(language) ? 1 : "lv".equals(language) ? 2 : -1;
        return index < 0 ? key : values[index];
    }

    private static final java.util.Map<String, String[]> TRANSLATIONS = makeTranslations();
    private static java.util.Map<String, String[]> makeTranslations() {
        java.util.Map<String, String[]> map = new java.util.HashMap<>();
        map.put("\u0422\u0423\u0420\u0411\u0418\u041d\u042b", new String[]{"TURBINES", "TURBINEN", "TURB\u012aNAS"});
        map.put("\u0420\u0410\u0411\u041e\u0422\u042b", new String[]{"JOBS", "ARBEITEN", "DARBI"});
        map.put("\u0414\u041e\u0411\u0410\u0412\u0418\u0422\u042c \u0420\u0410\u0411\u041e\u0422\u0423", new String[]{"ADD JOB", "ARBEIT HINZUF\u00dcGEN", "PIEVIENOT DARBU"});
        map.put("\u041a\u0410\u041b\u0415\u041d\u0414\u0410\u0420\u042c", new String[]{"CALENDAR", "KALENDER", "KALEND\u0100RS"});
        map.put("\u0424\u041e\u0422\u041e\u0413\u0420\u0410\u0424\u0418\u0418", new String[]{"PHOTOS", "FOTOS", "FOTOGR\u0100FIJAS"});
        map.put("\u041a\u041e\u041c\u0410\u041d\u0414\u0410", new String[]{"TEAM", "TEAM", "KOMANDA"});
        map.put("\u0421\u0418\u041d\u0425\u0420\u041e\u041d\u0418\u0417\u0410\u0426\u0418\u042f", new String[]{"SYNC", "SYNCHRONISIERUNG", "SINHRONIZ\u0100CIJA"});
        map.put("\u042f\u0417\u042b\u041a / LANGUAGE", new String[]{"LANGUAGE", "SPRACHE", "VALODA"});
        map.put("\u0410\u043a\u043a\u0430\u0443\u043d\u0442\u044b \u0434\u043e\u0431\u0430\u0432\u0438\u043c \u043f\u043e\u0437\u0436\u0435", new String[]{"Accounts will be added later", "Konten kommen sp\u00e4ter", "Konti tiks pievienoti v\u0113l\u0101k"});
        map.put("\u041f\u043e\u043a\u0430 \u0434\u0430\u043d\u043d\u044b\u0435 \u0442\u043e\u043b\u044c\u043a\u043e \u043d\u0430 \u044d\u0442\u043e\u043c \u0442\u0435\u043b\u0435\u0444\u043e\u043d\u0435", new String[]{"Data is stored on this phone only", "Daten sind nur auf diesem Handy gespeichert", "Dati pagaid\u0101m glab\u0101jas tikai \u0161aj\u0101 t\u0101lrun\u012b"});
        map.put("\u0420\u0410\u0411\u041e\u0422\u042b GRID", new String[]{"GRID JOBS", "GRID ARBEITEN", "GRID DARBI"});
        map.put("+ \u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0440\u0430\u0431\u043e\u0442\u0443", new String[]{"+ Add job", "+ Arbeit hinzuf\u00fcgen", "+ Pievienot darbu"});
        map.put("\u041f\u043e\u043a\u0430 \u043d\u0435\u0442 \u0440\u0430\u0431\u043e\u0442. \u0414\u043e\u0431\u0430\u0432\u044c \u043f\u0435\u0440\u0432\u0443\u044e!", new String[]{"No jobs yet. Add your first!", "Noch keine Arbeiten. Erste hinzuf\u00fcgen!", "Darbu v\u0113l nav. Pievieno pirmo!"});
        map.put("\u2713 \u0412\u042b\u041f\u041e\u041b\u041d\u0415\u041d\u041e", new String[]{"\u2713 COMPLETED", "\u2713 ERLEDIGT", "\u2713 PABEIGTS"});
        map.put("\u25cb \u0412 \u0420\u0410\u0411\u041e\u0422\u0415", new String[]{"\u25cb IN PROGRESS", "\u25cb IN BEARBEITUNG", "\u25cb PROCES\u0100"});
        map.put(" \u2022 \u0424\u043e\u0442\u043e: ", new String[]{" \u2022 Photos: ", " \u2022 Fotos: ", " \u2022 Foto: "});
        map.put("\u2190 \u0413\u043b\u0430\u0432\u043d\u043e\u0435 \u043c\u0435\u043d\u044e", new String[]{"\u2190 Home", "\u2190 Hauptmen\u00fc", "\u2190 Galven\u0101 izv\u0113lne"});
        map.put("\u0420\u0415\u0414\u0410\u041a\u0422\u0418\u0420\u041e\u0412\u0410\u0422\u042c", new String[]{"EDIT JOB", "ARBEIT BEARBEITEN", "REDI\u0122\u0112T DARBU"});
        map.put("\u041d\u041e\u0412\u0410\u042f \u0420\u0410\u0411\u041e\u0422\u0410", new String[]{"NEW JOB", "NEUE ARBEIT", "JAUNS DARBS"});
        map.put("\u041d\u043e\u043c\u0435\u0440 \u0438\u043b\u0438 \u043d\u0430\u0437\u0432\u0430\u043d\u0438\u0435 \u0442\u0443\u0440\u0431\u0438\u043d\u044b", new String[]{"Turbine number or name", "Turbinennummer oder Name", "Turb\u012bnas numurs vai nosaukums"});
        map.put("\u041d\u0430\u0437\u0432\u0430\u043d\u0438\u0435 \u0440\u0430\u0431\u043e\u0442\u044b", new String[]{"Job title", "Arbeitsbezeichnung", "Darba nosaukums"});
        map.put("\u0414\u0430\u0442\u0430 \u0440\u0430\u0431\u043e\u0442\u044b", new String[]{"Job date", "Arbeitsdatum", "Darba datums"});
        map.put("\u041e\u043f\u0438\u0441\u0430\u043d\u0438\u0435 \u0440\u0430\u0431\u043e\u0442\u044b", new String[]{"Job description", "Arbeitsbeschreibung", "Darba apraksts"});
        map.put("\ud83d\udcf7 \u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0444\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0438", new String[]{"\ud83d\udcf7 Add photos", "\ud83d\udcf7 Fotos hinzuf\u00fcgen", "\ud83d\udcf7 Pievienot foto"});
        map.put("\u041f\u0440\u0438\u043a\u0440\u0435\u043f\u043b\u0435\u043d\u043e \u0444\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0439: ", new String[]{"Attached photos: ", "Angeh\u00e4ngte Fotos: ", "Pievienotie foto: "});
        map.put("\u041f\u0440\u043e\u0441\u043c\u043e\u0442\u0440\u0435\u0442\u044c / \u0443\u0431\u0440\u0430\u0442\u044c \u0444\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0438", new String[]{"View / remove photos", "Fotos ansehen / entfernen", "Skat\u012bt / no\u0146emt foto"});
        map.put("\u0424\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0439 \u043f\u043e\u043a\u0430 \u043d\u0435\u0442", new String[]{"No photos yet", "Noch keine Fotos", "Foto v\u0113l nav"});
        map.put("\u0424\u043e\u0442\u043e ", new String[]{"Photo ", "Foto ", "Foto "});
        map.put("\u041e\u0442\u043a\u0440\u044b\u0442\u044c", new String[]{"Open", "\u00d6ffnen", "Atv\u0113rt"});
        map.put("\u0423\u0431\u0440\u0430\u0442\u044c \u0438\u0437 \u0440\u0430\u0431\u043e\u0442\u044b", new String[]{"Remove from job", "Aus Arbeit entfernen", "No\u0146emt no darba"});
        map.put("\u0421\u041e\u0425\u0420\u0410\u041d\u0418\u0422\u042c", new String[]{"SAVE", "SPEICHERN", "SAGLAB\u0100T"});
        map.put("\u0417\u0430\u043f\u043e\u043b\u043d\u0438 \u0442\u0443\u0440\u0431\u0438\u043d\u0443 \u0438 \u043d\u0430\u0437\u0432\u0430\u043d\u0438\u0435 \u0440\u0430\u0431\u043e\u0442\u044b", new String[]{"Enter turbine and job title", "Turbine und Arbeitsbezeichnung eingeben", "Ievadi turb\u012bnu un darba nosaukumu"});
        map.put("\u041e\u0448\u0438\u0431\u043a\u0430 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u044f", new String[]{"Could not save", "Speichern fehlgeschlagen", "Neizdev\u0101s saglab\u0101t"});
        map.put("\u041e\u0422\u041c\u0415\u041d\u0410", new String[]{"CANCEL", "ABBRECHEN", "ATCELT"});
        map.put("\u0414\u043e\u0431\u0430\u0432\u043b\u0435\u043d\u043e \u0444\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0439: ", new String[]{"Photos added: ", "Fotos hinzugef\u00fcgt: ", "Pievienoti foto: "});
        map.put(". \u041d\u0430\u0436\u043c\u0438 \u0421\u041e\u0425\u0420\u0410\u041d\u0418\u0422\u042c \u0434\u043b\u044f \u0437\u0430\u043f\u0438\u0441\u0438 \u0440\u0430\u0431\u043e\u0442\u044b.", new String[]{". Tap SAVE to save the job.", ". Tippe SPEICHERN, um die Arbeit zu speichern.", ". Nospied SAGLAB\u0100T, lai saglab\u0101tu darbu."});
        map.put("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c \u0434\u043e\u0441\u0442\u0443\u043f \u043a \u0444\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0438", new String[]{"Could not retain access to photo", "Fotozugriff konnte nicht gespeichert werden", "Neizdev\u0101s saglab\u0101t piek\u013cuvi foto"});
        map.put("\u0424\u041e\u0422\u041e\u0413\u0420\u0410\u0424\u0418\u0418 GRID", new String[]{"GRID PHOTOS", "GRID FOTOS", "GRID FOTOGR\u0100FIJAS"});
        map.put(" \u2014 ", new String[]{" \u2014 ", " \u2014 ", " \u2014 "});
        map.put("\u0424\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0439 \u043f\u043e\u043a\u0430 \u043d\u0435\u0442. \u0414\u043e\u0431\u0430\u0432\u044c \u0438\u0445 \u043a \u0440\u0430\u0431\u043e\u0442\u0435.", new String[]{"No photos yet. Attach photos to a job.", "Noch keine Fotos. F\u00fcge einer Arbeit Fotos hinzu.", "Foto v\u0113l nav. Pievieno tos darbam."});
        map.put("\u0424\u043e\u0442\u043e \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u043d\u043e", new String[]{"Photo unavailable", "Foto nicht verf\u00fcgbar", "Foto nav pieejams"});
        map.put("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0442\u043a\u0440\u044b\u0442\u044c \u0444\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u044e", new String[]{"Could not open photo", "Foto konnte nicht ge\u00f6ffnet werden", "Neizdev\u0101s atv\u0113rt foto"});
        map.put("\u041f\u043e\u0441\u043c\u043e\u0442\u0440\u0435\u0442\u044c \u043e\u043f\u0438\u0441\u0430\u043d\u0438\u0435", new String[]{"View description", "Beschreibung anzeigen", "Skat\u012bt aprakstu"});
        map.put("\u0424\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0438 \u0440\u0430\u0431\u043e\u0442\u044b", new String[]{"Job photos", "Arbeitsfotos", "Darba foto"});
        map.put("\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u0442\u044c", new String[]{"Edit", "Bearbeiten", "Redi\u0123\u0113t"});
        map.put("\u0412\u0435\u0440\u043d\u0443\u0442\u044c \u0432 \u0440\u0430\u0431\u043e\u0442\u0443", new String[]{"Mark in progress", "Als in Bearbeitung markieren", "Atz\u012bm\u0113t k\u0101 proces\u0101"});
        map.put("\u041e\u0442\u043c\u0435\u0442\u0438\u0442\u044c \u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u043d\u043e\u0439", new String[]{"Mark completed", "Als erledigt markieren", "Atz\u012bm\u0113t k\u0101 pabeigtu"});
        map.put("\u0423\u0434\u0430\u043b\u0438\u0442\u044c", new String[]{"Delete", "L\u00f6schen", "Dz\u0113st"});
        map.put("\u041e\u043f\u0438\u0441\u0430\u043d\u0438\u0435", new String[]{"Description", "Beschreibung", "Apraksts"});
        map.put("\u0424\u041e\u0422\u041e \u0420\u0410\u0411\u041e\u0422\u042b", new String[]{"JOB PHOTOS", "ARBEITSFOTOS", "DARBA FOTO"});
        map.put("\u0424\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0439 \u043d\u0435\u0442", new String[]{"No photos", "Keine Fotos", "Nav foto"});
        map.put("\u2190 \u0420\u0430\u0431\u043e\u0442\u044b", new String[]{"\u2190 Jobs", "\u2190 Arbeiten", "\u2190 Darbi"});
        map.put("\u041e\u0448\u0438\u0431\u043a\u0430", new String[]{"Error", "Fehler", "K\u013c\u016bda"});
        map.put("\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u044d\u0442\u0443 \u0440\u0430\u0431\u043e\u0442\u0443?", new String[]{"Delete this job?", "Diese Arbeit l\u00f6schen?", "Dz\u0113st \u0161o darbu?"});
        map.put("\u041e\u0442\u043c\u0435\u043d\u0430", new String[]{"Cancel", "Abbrechen", "Atcelt"});
        map.put("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c \u0440\u0430\u0431\u043e\u0442\u044b", new String[]{"Could not load jobs", "Arbeiten konnten nicht geladen werden", "Neizdev\u0101s iel\u0101d\u0113t darbus"});
        map.put("\u0428\u0410\u0411\u041b\u041e\u041d \u0420\u0410\u0411\u041e\u0422", new String[]{"JOB TEMPLATE", "ARBEITSVORLAGE", "DARBU VEIDNE"});
        map.put("\u041d\u043e\u043c\u0435\u0440 \u0442\u0443\u0440\u0431\u0438\u043d\u044b", new String[]{"Turbine number", "Turbinennummer", "Turb\u012bnas numurs"});
        map.put("\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u043d\u043e\u0432\u044b\u0439 \u0441\u043f\u0438\u0441\u043e\u043a \u0440\u0430\u0431\u043e\u0442 \u0431\u0435\u0437 \u043e\u0442\u043c\u0435\u0442\u043e\u043a \u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u0438\u044f?", new String[]{"Add a new unchecked job list?", "Neue Aufgabenliste ohne Erledigungsmarkierungen hinzuf\u00fcgen?", "Pievienot jaunu darbu sarakstu bez izpildes atz\u012bm\u0113m?"});
        map.put("\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0448\u0430\u0431\u043b\u043e\u043d", new String[]{"Add template", "Vorlage hinzuf\u00fcgen", "Pievienot veidni"});
        map.put("\u0414\u043b\u044f \u044d\u0442\u043e\u0439 \u0442\u0443\u0440\u0431\u0438\u043d\u044b \u0443\u0436\u0435 \u0435\u0441\u0442\u044c \u0440\u0430\u0431\u043e\u0442\u044b", new String[]{"Jobs already exist for this turbine", "F\u00fcr diese Turbine gibt es bereits Aufgaben", "\u0160ai turb\u012bnai jau ir darbi"});
        return map;
    }
    private void showSearch(String query, int unused) {
        final EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Поиск работы");

        new AlertDialog.Builder(this)
            .setTitle("Поиск")
            .setView(search)
            .setPositiveButton("Найти", (dialog, which) -> {
                String text = search.getText().toString().trim();
                if (text.isEmpty()) {
                    showHome();
                    return;
                }

                String needle = text.toLowerCase(java.util.Locale.ROOT);
                java.util.ArrayList<String> results =
                    new java.util.ArrayList<>();

                for (JSONObject job : jobs) {
                    if (job.toString().toLowerCase(
                            java.util.Locale.ROOT).contains(needle)) {
                        results.add(job.optString("name", "Работа"));
                    }
                }

                new AlertDialog.Builder(this)
                    .setTitle("Результаты поиска")
                    .setItems(results.toArray(new String[0]),
                        (d, selected) -> showJobs())
                    .setPositiveButton("OK", null)
                    .show();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
}
