package com.grid.team;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.Gravity;
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
    private final int bg = Color.rgb(17, 25, 36);
    private final int panel = Color.rgb(38, 65, 86);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        storage = getSharedPreferences("grid_data", MODE_PRIVATE);
        loadJobs();
        showHome();
    }

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.WHITE);
        t.setPadding(12, 16, 12, 16);
        return t;
    }

    private void page(String heading) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bg);
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 40, 24, 40);
        TextView title = text(heading, 30);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);
        scroll.addView(layout);
        setContentView(scroll);
    }

    private void button(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setBackgroundTintList(ColorStateList.valueOf(panel));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 8, 0, 12);
        layout.addView(b, p);
        b.setMinHeight(96);
        b.setOnClickListener(v -> action.run());
    }

    private void showHome() {
        page("GRID");
        TextView sub = text("TEAM WORK MANAGEMENT", 15);
        sub.setGravity(Gravity.CENTER);
        layout.addView(sub);
        button("ТУРБИНЫ", this::showJobs);
        button("РАБОТЫ", this::showJobs);
        button("ДОБАВИТЬ РАБОТУ", () -> editJob(-1));
        button("КАЛЕНДАРЬ", this::showJobs);
        button("ФОТОГРАФИИ", this::showGallery);
        button("КОМАНДА", () -> message("Аккаунты добавим позже"));
        button("СИНХРОНИЗАЦИЯ", () -> message("Пока данные только на этом телефоне"));
    }

    private void showJobs() {
        page("РАБОТЫ GRID");
        button("+ Добавить работу", () -> editJob(-1));
        if (jobs.isEmpty()) layout.addView(text("Пока нет работ. Добавь первую!", 17));
        for (int i = 0; i < jobs.size(); i++) {
            final int index = i;
            JSONObject j = jobs.get(i);
            String status = j.optBoolean("done") ? "✓ ВЫПОЛНЕНО" : "○ В РАБОТЕ";
            String info = j.optString("turbine") + "\n" + j.optString("name") + "\n"
                    + j.optString("date") + "\n" + status + " • Фото: " + photos(j).length();
            button(info, () -> jobOptions(index));
        }
        button("← Главное меню", this::showHome);
    }

    private EditText field(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(Color.LTGRAY);
        e.setTextColor(Color.WHITE);
        e.setText(value);
        e.setTextSize(17);
        e.setPadding(12, 20, 12, 20);
        layout.addView(e);
        return e;
    }

    private void editJob(int index) {
        editingIndex = index;
        JSONObject old = index >= 0 ? jobs.get(index) : new JSONObject();
        draftPhotos.clear();
        JSONArray saved = photos(old);
        for (int i = 0; i < saved.length(); i++) draftPhotos.add(saved.optString(i));
        page(index >= 0 ? "РЕДАКТИРОВАТЬ" : "НОВАЯ РАБОТА");
        EditText turbine = field("Номер или название турбины", old.optString("turbine"));
        EditText name = field("Название работы", old.optString("name"));
        EditText date = field("Дата работы", old.optString("date"));
        date.setFocusable(false);
        date.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) ->
                date.setText(String.format(Locale.US, "%02d.%02d.%04d", day, month + 1, year)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
        EditText description = field("Описание работы", old.optString("description"));
        description.setMinLines(3);
        description.setGravity(Gravity.TOP);
        button("📷 Добавить фотографии", this::pickPhotos);
        TextView photoCount = text("Прикреплено фотографий: " + draftPhotos.size(), 16);
        layout.addView(photoCount);
        button("Просмотреть / убрать фотографии", () -> {
            if (draftPhotos.isEmpty()) { message("Фотографий пока нет"); return; }
            String[] items = new String[draftPhotos.size()];
            for (int i = 0; i < items.length; i++) items[i] = "Фото " + (i + 1);
            new AlertDialog.Builder(this).setTitle("Фотографии")
                .setItems(items, (d, which) -> new AlertDialog.Builder(this)
                    .setTitle("Фото " + (which + 1))
                    .setItems(new String[]{"Открыть", "Убрать из работы"}, (dialog, action) -> {
                        if (action == 0) openPhoto(draftPhotos.get(which));
                        else {
                            draftPhotos.remove(which);
                            photoCount.setText("Прикреплено фотографий: " + draftPhotos.size());
                        }
                    }).show()).show();
        });
        button("СОХРАНИТЬ", () -> {
            if (turbine.getText().toString().trim().isEmpty()
                || name.getText().toString().trim().isEmpty()) {
                message("Заполни турбину и название работы"); return;
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
            } catch (Exception e) { message("Ошибка сохранения"); }
        });
        button("ОТМЕНА", this::showJobs);
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
        message("Добавлено фотографий: " + added + ". Нажми СОХРАНИТЬ для записи работы.");
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
            message("Не удалось сохранить доступ к фотографии");
        }
        return false;
    }

    private JSONArray photos(JSONObject j) {
        JSONArray a = j.optJSONArray("photos");
        return a == null ? new JSONArray() : a;
    }

    private void showGallery() {
        page("ФОТОГРАФИИ GRID");
        int count = 0;
        for (int i = 0; i < jobs.size(); i++) {
            JSONObject j = jobs.get(i);
            JSONArray arr = photos(j);
            if (arr.length() == 0) continue;
            layout.addView(text(j.optString("turbine") + " — " + j.optString("name"), 19));
            for (int k = 0; k < arr.length(); k++) {
                String uri = arr.optString(k);
                if (uri.isEmpty()) continue;
                count++;
                addThumbnail(uri);
            }
        }
        if (count == 0) layout.addView(text("Фотографий пока нет. Добавь их к работе.", 17));
        button("← Главное меню", this::showHome);
    }

    private void addThumbnail(String uri) {
        ImageView image = new ImageView(this);
        image.setAdjustViewBounds(true);
        image.setMaxHeight(480);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        try { image.setImageURI(Uri.parse(uri)); }
        catch (Exception e) { layout.addView(text("Фото недоступно", 14)); return; }
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
        } catch (Exception e) { message("Не удалось открыть фотографию"); }
    }

    private void jobOptions(int index) {
        JSONObject j = jobs.get(index);
        String[] options = {"Посмотреть описание", "Фотографии работы", "Редактировать",
            j.optBoolean("done") ? "Вернуть в работу" : "Отметить выполненной", "Удалить"};
        new AlertDialog.Builder(this).setTitle(j.optString("name"))
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    new AlertDialog.Builder(this).setTitle("Описание")
                        .setMessage(j.optString("description", ""))
                        .setPositiveButton("OK", null).show();
                } else if (which == 1) {
                    page("ФОТО РАБОТЫ");
                    JSONArray a = photos(j);
                    if (a.length() == 0) layout.addView(text("Фотографий нет", 17));
                    for (int i = 0; i < a.length(); i++) addThumbnail(a.optString(i));
                    button("← Работы", this::showJobs);
                } else if (which == 2) editJob(index);
                else if (which == 3) {
                    try {
                        j.put("done", !j.optBoolean("done"));
                        saveJobs(); showJobs();
                    } catch (Exception e) { message("Ошибка"); }
                } else {
                    new AlertDialog.Builder(this).setMessage("Удалить эту работу?")
                        .setNegativeButton("Отмена", null)
                        .setPositiveButton("Удалить", (d, w) -> {
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
        } catch (Exception e) { message("Не удалось загрузить работы"); }
    }

    private void message(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
