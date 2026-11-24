package com.grid.team;

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

public class MainActivity extends Activity {
    private static final int PICK_PHOTOS = 201;
    private final ArrayList<JSONObject> jobs = new ArrayList<>();
    private final ArrayList<String> draftPhotos = new ArrayList<>();
    private SharedPreferences storage;
    private LinearLayout layout;
    private int editingIndex = -1;
    private int buttonCount = 0;
    private String language = "ru";
    private final int white = Color.WHITE;
    private final int glassFill = Color.argb(100, 20, 40, 55);
    private final int glassBorder = Color.argb(165, 255, 255, 255);

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
        showHome();
    }

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.WHITE);
        t.setPadding(dp(12), dp(16), dp(12), dp(16));
        t.setShadowLayer(dp(2), 0, dp(1), Color.argb(190, 0, 0, 0));
        return t;
    }

    private void page(String heading) {
        buttonCount = 0;
        FrameLayout root = new FrameLayout(this);
        ImageView wallpaper = new ImageView(this);
        wallpaper.setImageResource(R.drawable.grid_background);
        wallpaper.setScaleType(ImageView.ScaleType.CENTER_CROP);
        wallpaper.setColorFilter(Color.argb(72, 0, 12, 25), PorterDuff.Mode.SRC_ATOP);
        root.addView(wallpaper, new FrameLayout.LayoutParams(-1, -1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(35), dp(20), dp(35));
        TextView title = text(heading, 30);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setShadowLayer(dp(4), 0, dp(2), Color.BLACK);
        layout.addView(title);
        scroll.addView(layout);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        // Soft fade and upward slide whenever a GRID page opens.
        scroll.setAlpha(0f);
        scroll.setTranslationY(dp(18));
        scroll.animate().alpha(1f).translationY(0f)
            .setDuration(320).setInterpolator(new DecelerateInterpolator()).start();
    }

    private void button(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(white);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTypeface(null, Typeface.BOLD);
        b.setGravity(Gravity.CENTER);
        b.setBackground(glassShape(22, false));
        b.setElevation(dp(5));
        b.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, dp(7), 0, dp(9));
        layout.addView(b, p);
        b.setMinHeight(dp(64));
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
        page("GRID");
        TextView sub = text("TEAM WORK MANAGEMENT", 15);
        sub.setGravity(Gravity.CENTER);
        layout.addView(sub);
        button(tr("\u0422\u0423\u0420\u0411\u0418\u041d\u042b"), this::showJobs);
        button(tr("\u0420\u0410\u0411\u041e\u0422\u042b"), this::showJobs);
        button(tr("\u0414\u041e\u0411\u0410\u0412\u0418\u0422\u042c \u0420\u0410\u0411\u041e\u0422\u0423"), () -> editJob(-1));
        button(tr("\u041a\u0410\u041b\u0415\u041d\u0414\u0410\u0420\u042c"), this::showJobs);
        button(tr("\u0424\u041e\u0422\u041e\u0413\u0420\u0410\u0424\u0418\u0418"), this::showGallery);
        button(tr("\u041a\u041e\u041c\u0410\u041d\u0414\u0410"), () -> message(tr("\u0410\u043a\u043a\u0430\u0443\u043d\u0442\u044b \u0434\u043e\u0431\u0430\u0432\u0438\u043c \u043f\u043e\u0437\u0436\u0435")));
        button(tr("\u0421\u0418\u041d\u0425\u0420\u041e\u041d\u0418\u0417\u0410\u0426\u0418\u042f"), () -> message(tr("\u041f\u043e\u043a\u0430 \u0434\u0430\u043d\u043d\u044b\u0435 \u0442\u043e\u043b\u044c\u043a\u043e \u043d\u0430 \u044d\u0442\u043e\u043c \u0442\u0435\u043b\u0435\u0444\u043e\u043d\u0435")));
        button(tr("\u0428\u0410\u0411\u041b\u041e\u041d \u0420\u0410\u0411\u041e\u0422"), this::importTemplate);
        button(tr("\u042f\u0417\u042b\u041a / LANGUAGE"), this::chooseLanguage);
    }

    private void showJobs() {
        page(tr("\u0420\u0410\u0411\u041e\u0422\u042b GRID"));
        button(tr("+ \u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0440\u0430\u0431\u043e\u0442\u0443"), () -> editJob(-1));
        if (jobs.isEmpty()) layout.addView(text(tr("\u041f\u043e\u043a\u0430 \u043d\u0435\u0442 \u0440\u0430\u0431\u043e\u0442. \u0414\u043e\u0431\u0430\u0432\u044c \u043f\u0435\u0440\u0432\u0443\u044e!"), 17));
        for (int i = 0; i < jobs.size(); i++) {
            final int index = i;
            JSONObject j = jobs.get(i);
            String status = j.optBoolean("done") ? tr("\u2713 \u0412\u042b\u041f\u041e\u041b\u041d\u0415\u041d\u041e") : tr("\u25cb \u0412 \u0420\u0410\u0411\u041e\u0422\u0415");
            String info = j.optString("turbine") + "\n" + j.optString("name") + "\n"
                    + j.optString("date") + "\n" + status + tr(" \u2022 \u0424\u043e\u0442\u043e: ") + photos(j).length();
            button(info, () -> jobOptions(index));
        }
        button(tr("\u2190 \u0413\u043b\u0430\u0432\u043d\u043e\u0435 \u043c\u0435\u043d\u044e"), this::showHome);
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
        editingIndex = index;
        JSONObject old = index >= 0 ? jobs.get(index) : new JSONObject();
        draftPhotos.clear();
        JSONArray saved = photos(old);
        for (int i = 0; i < saved.length(); i++) draftPhotos.add(saved.optString(i));
        page(index >= 0 ? tr("\u0420\u0415\u0414\u0410\u041a\u0422\u0418\u0420\u041e\u0412\u0410\u0422\u042c") : tr("\u041d\u041e\u0412\u0410\u042f \u0420\u0410\u0411\u041e\u0422\u0410"));
        EditText turbine = field(tr("\u041d\u043e\u043c\u0435\u0440 \u0438\u043b\u0438 \u043d\u0430\u0437\u0432\u0430\u043d\u0438\u0435 \u0442\u0443\u0440\u0431\u0438\u043d\u044b"), old.optString("turbine"));
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
                JSONArray array = new JSONArray();
                for (String uri : draftPhotos) array.put(uri);
                j.put("photos", array);
                if (index >= 0) jobs.set(index, j); else jobs.add(j);
                saveJobs();
                showJobs();
            } catch (Exception e) { message(tr("\u041e\u0448\u0438\u0431\u043a\u0430 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u044f")); }
        });
        button(tr("\u041e\u0422\u041c\u0415\u041d\u0410"), this::showJobs);
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
        page(tr("\u0424\u041e\u0422\u041e\u0413\u0420\u0410\u0424\u0418\u0418 GRID"));
        int count = 0;
        for (int i = 0; i < jobs.size(); i++) {
            JSONObject j = jobs.get(i);
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
        button(tr("\u2190 \u0413\u043b\u0430\u0432\u043d\u043e\u0435 \u043c\u0435\u043d\u044e"), this::showHome);
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
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(uri), "image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) { message(tr("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0442\u043a\u0440\u044b\u0442\u044c \u0444\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u044e")); }
    }

    private void jobOptions(int index) {
        JSONObject j = jobs.get(index);
        String[] options = {tr("\u041f\u043e\u0441\u043c\u043e\u0442\u0440\u0435\u0442\u044c \u043e\u043f\u0438\u0441\u0430\u043d\u0438\u0435"), tr("\u0424\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0438 \u0440\u0430\u0431\u043e\u0442\u044b"), tr("\u0420\u0435\u0434\u0430\u043a\u0442\u0438\u0440\u043e\u0432\u0430\u0442\u044c"),
            j.optBoolean("done") ? tr("\u0412\u0435\u0440\u043d\u0443\u0442\u044c \u0432 \u0440\u0430\u0431\u043e\u0442\u0443") : tr("\u041e\u0442\u043c\u0435\u0442\u0438\u0442\u044c \u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u043d\u043e\u0439"), tr("\u0423\u0434\u0430\u043b\u0438\u0442\u044c")};
        new AlertDialog.Builder(this).setTitle(j.optString("name"))
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    new AlertDialog.Builder(this).setTitle(tr("\u041e\u043f\u0438\u0441\u0430\u043d\u0438\u0435"))
                        .setMessage(j.optString("description", ""))
                        .setPositiveButton("OK", null).show();
                } else if (which == 1) {
                    page(tr("\u0424\u041e\u0422\u041e \u0420\u0410\u0411\u041e\u0422\u042b"));
                    JSONArray a = photos(j);
                    if (a.length() == 0) layout.addView(text(tr("\u0424\u043e\u0442\u043e\u0433\u0440\u0430\u0444\u0438\u0439 \u043d\u0435\u0442"), 17));
                    for (int i = 0; i < a.length(); i++) addThumbnail(a.optString(i));
                    button(tr("\u2190 \u0420\u0430\u0431\u043e\u0442\u044b"), this::showJobs);
                } else if (which == 2) editJob(index);
                else if (which == 3) {
                    try {
                        j.put("done", !j.optBoolean("done"));
                        saveJobs(); showJobs();
                    } catch (Exception e) { message(tr("\u041e\u0448\u0438\u0431\u043a\u0430")); }
                } else {
                    new AlertDialog.Builder(this).setMessage(tr("\u0423\u0434\u0430\u043b\u0438\u0442\u044c \u044d\u0442\u0443 \u0440\u0430\u0431\u043e\u0442\u0443?"))
                        .setNegativeButton(tr("\u041e\u0442\u043c\u0435\u043d\u0430"), null)
                        .setPositiveButton(tr("\u0423\u0434\u0430\u043b\u0438\u0442\u044c"), (d, w) -> {
                            jobs.remove(index); saveJobs(); showJobs();
                        }).show();
                }
            }).show();
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
            showJobs();
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
                showHome();
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

}
