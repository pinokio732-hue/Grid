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
        button(tr("ТУРБИНЫ"), this::showJobs);
        button(tr("РАБОТЫ"), this::showJobs);
        button(tr("ДОБАВИТЬ РАБОТУ"), () -> editJob(-1));
        button(tr("КАЛЕНДАРЬ"), this::showJobs);
        button(tr("ФОТОГРАФИИ"), this::showGallery);
        button(tr("КОМАНДА"), () -> message(tr("Аккаунты добавим позже")));
        button(tr("СИНХРОНИЗАЦИЯ"), () -> message(tr("Пока данные только на этом телефоне")));
        button(tr("ЯЗЫК / LANGUAGE"), this::chooseLanguage);
    }

    private void showJobs() {
        page(tr("РАБОТЫ GRID"));
        button(tr("+ Добавить работу"), () -> editJob(-1));
        if (jobs.isEmpty()) layout.addView(text(tr("Пока нет работ. Добавь первую!"), 17));
        for (int i = 0; i < jobs.size(); i++) {
            final int index = i;
            JSONObject j = jobs.get(i);
            String status = j.optBoolean("done") ? tr("✓ ВЫПОЛНЕНО") : tr("○ В РАБОТЕ");
            String info = j.optString("turbine") + "\n" + j.optString("name") + "\n"
                    + j.optString("date") + "\n" + status + tr(" • Фото: ") + photos(j).length();
            button(info, () -> jobOptions(index));
        }
        button(tr("← Главное меню"), this::showHome);
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
        page(index >= 0 ? tr("РЕДАКТИРОВАТЬ") : tr("НОВАЯ РАБОТА"));
        EditText turbine = field(tr("Номер или название турбины"), old.optString("turbine"));
        EditText name = field(tr("Название работы"), old.optString("name"));
        EditText date = field(tr("Дата работы"), old.optString("date"));
        date.setFocusable(false);
        date.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) ->
                date.setText(String.format(Locale.US, "%02d.%02d.%04d", day, month + 1, year)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
        EditText description = field(tr("Описание работы"), old.optString("description"));
        description.setMinLines(3);
        description.setGravity(Gravity.TOP);
        button(tr("📷 Добавить фотографии"), this::pickPhotos);
        TextView photoCount = text(tr("Прикреплено фотографий: ") + draftPhotos.size(), 16);
        layout.addView(photoCount);
        button(tr("Просмотреть / убрать фотографии"), () -> {
            if (draftPhotos.isEmpty()) { message(tr("Фотографий пока нет")); return; }
            String[] items = new String[draftPhotos.size()];
            for (int i = 0; i < items.length; i++) items[i] = tr("Фото ") + (i + 1);
            new AlertDialog.Builder(this).setTitle(tr("ФОТОГРАФИИ"))
                .setItems(items, (d, which) -> new AlertDialog.Builder(this)
                    .setTitle(tr("Фото ") + (which + 1))
                    .setItems(new String[]{tr("Открыть"), tr("Убрать из работы")}, (dialog, action) -> {
                        if (action == 0) openPhoto(draftPhotos.get(which));
                        else {
                            draftPhotos.remove(which);
                            photoCount.setText(tr("Прикреплено фотографий: ") + draftPhotos.size());
                        }
                    }).show()).show();
        });
        button(tr("СОХРАНИТЬ"), () -> {
            if (turbine.getText().toString().trim().isEmpty()
                || name.getText().toString().trim().isEmpty()) {
                message(tr("Заполни турбину и название работы")); return;
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
            } catch (Exception e) { message(tr("Ошибка сохранения")); }
        });
        button(tr("ОТМЕНА"), this::showJobs);
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
        message(tr("Добавлено фотографий: ") + added + tr(". Нажми СОХРАНИТЬ для записи работы."));
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
            message(tr("Не удалось сохранить доступ к фотографии"));
        }
        return false;
    }

    private JSONArray photos(JSONObject j) {
        JSONArray a = j.optJSONArray("photos");
        return a == null ? new JSONArray() : a;
    }

    private void showGallery() {
        page(tr("ФОТОГРАФИИ GRID"));
        int count = 0;
        for (int i = 0; i < jobs.size(); i++) {
            JSONObject j = jobs.get(i);
            JSONArray arr = photos(j);
            if (arr.length() == 0) continue;
            layout.addView(text(j.optString("turbine") + tr(" — ") + j.optString("name"), 19));
            for (int k = 0; k < arr.length(); k++) {
                String uri = arr.optString(k);
                if (uri.isEmpty()) continue;
                count++;
                addThumbnail(uri);
            }
        }
        if (count == 0) layout.addView(text(tr("Фотографий пока нет. Добавь их к работе."), 17));
        button(tr("← Главное меню"), this::showHome);
    }

    private void addThumbnail(String uri) {
        ImageView image = new ImageView(this);
        image.setAdjustViewBounds(true);
        image.setMaxHeight(480);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        try { image.setImageURI(Uri.parse(uri)); }
        catch (Exception e) { layout.addView(text(tr("Фото недоступно"), 14)); return; }
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
        } catch (Exception e) { message(tr("Не удалось открыть фотографию")); }
    }

    private void jobOptions(int index) {
        JSONObject j = jobs.get(index);
        String[] options = {tr("Посмотреть описание"), tr("Фотографии работы"), tr("Редактировать"),
            j.optBoolean("done") ? tr("Вернуть в работу") : tr("Отметить выполненной"), tr("Удалить")};
        new AlertDialog.Builder(this).setTitle(j.optString("name"))
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    new AlertDialog.Builder(this).setTitle(tr("Описание"))
                        .setMessage(j.optString("description", ""))
                        .setPositiveButton("OK", null).show();
                } else if (which == 1) {
                    page(tr("ФОТО РАБОТЫ"));
                    JSONArray a = photos(j);
                    if (a.length() == 0) layout.addView(text(tr("Фотографий нет"), 17));
                    for (int i = 0; i < a.length(); i++) addThumbnail(a.optString(i));
                    button(tr("← Работы"), this::showJobs);
                } else if (which == 2) editJob(index);
                else if (which == 3) {
                    try {
                        j.put("done", !j.optBoolean("done"));
                        saveJobs(); showJobs();
                    } catch (Exception e) { message(tr("Ошибка")); }
                } else {
                    new AlertDialog.Builder(this).setMessage(tr("Удалить эту работу?"))
                        .setNegativeButton(tr("Отмена"), null)
                        .setPositiveButton(tr("Удалить"), (d, w) -> {
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
        } catch (Exception e) { message(tr("Не удалось загрузить работы")); }
    }

    private void message(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
    private void chooseLanguage() {
        String[] names = {"🇷🇺 Русский", "🇬🇧 English", "🇩🇪 Deutsch", "🇱🇻 Latviešu"};
        String[] codes = {"ru", "en", "de", "lv"};
        int selected = 0;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(language)) selected = i;
        }
        new AlertDialog.Builder(this)
            .setTitle(tr("ЯЗЫК / LANGUAGE"))
            .setSingleChoiceItems(names, selected, (dialog, which) -> {
                language = codes[which];
                storage.edit().putString("language", language).apply();
                dialog.dismiss();
                showHome();
            })
            .setNegativeButton(tr("Отмена"), null)
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
        map.put("ТУРБИНЫ", new String[]{"TURBINES", "TURBINEN", "TURBĪNAS"});
        map.put("РАБОТЫ", new String[]{"JOBS", "ARBEITEN", "DARBI"});
        map.put("ДОБАВИТЬ РАБОТУ", new String[]{"ADD JOB", "ARBEIT HINZUFÜGEN", "PIEVIENOT DARBU"});
        map.put("КАЛЕНДАРЬ", new String[]{"CALENDAR", "KALENDER", "KALENDĀRS"});
        map.put("ФОТОГРАФИИ", new String[]{"PHOTOS", "FOTOS", "FOTOGRĀFIJAS"});
        map.put("КОМАНДА", new String[]{"TEAM", "TEAM", "KOMANDA"});
        map.put("СИНХРОНИЗАЦИЯ", new String[]{"SYNC", "SYNCHRONISIERUNG", "SINHRONIZĀCIJA"});
        map.put("ЯЗЫК / LANGUAGE", new String[]{"LANGUAGE", "SPRACHE", "VALODA"});
        map.put("Аккаунты добавим позже", new String[]{"Accounts will be added later", "Konten kommen später", "Konti tiks pievienoti vēlāk"});
        map.put("Пока данные только на этом телефоне", new String[]{"Data is stored on this phone only", "Daten sind nur auf diesem Handy gespeichert", "Dati pagaidām glabājas tikai šajā tālrunī"});
        map.put("РАБОТЫ GRID", new String[]{"GRID JOBS", "GRID ARBEITEN", "GRID DARBI"});
        map.put("+ Добавить работу", new String[]{"+ Add job", "+ Arbeit hinzufügen", "+ Pievienot darbu"});
        map.put("Пока нет работ. Добавь первую!", new String[]{"No jobs yet. Add your first!", "Noch keine Arbeiten. Erste hinzufügen!", "Darbu vēl nav. Pievieno pirmo!"});
        map.put("✓ ВЫПОЛНЕНО", new String[]{"✓ COMPLETED", "✓ ERLEDIGT", "✓ PABEIGTS"});
        map.put("○ В РАБОТЕ", new String[]{"○ IN PROGRESS", "○ IN BEARBEITUNG", "○ PROCESĀ"});
        map.put(" • Фото: ", new String[]{" • Photos: ", " • Fotos: ", " • Foto: "});
        map.put("← Главное меню", new String[]{"← Home", "← Hauptmenü", "← Galvenā izvēlne"});
        map.put("РЕДАКТИРОВАТЬ", new String[]{"EDIT JOB", "ARBEIT BEARBEITEN", "REDIĢĒT DARBU"});
        map.put("НОВАЯ РАБОТА", new String[]{"NEW JOB", "NEUE ARBEIT", "JAUNS DARBS"});
        map.put("Номер или название турбины", new String[]{"Turbine number or name", "Turbinennummer oder Name", "Turbīnas numurs vai nosaukums"});
        map.put("Название работы", new String[]{"Job title", "Arbeitsbezeichnung", "Darba nosaukums"});
        map.put("Дата работы", new String[]{"Job date", "Arbeitsdatum", "Darba datums"});
        map.put("Описание работы", new String[]{"Job description", "Arbeitsbeschreibung", "Darba apraksts"});
        map.put("📷 Добавить фотографии", new String[]{"📷 Add photos", "📷 Fotos hinzufügen", "📷 Pievienot foto"});
        map.put("Прикреплено фотографий: ", new String[]{"Attached photos: ", "Angehängte Fotos: ", "Pievienotie foto: "});
        map.put("Просмотреть / убрать фотографии", new String[]{"View / remove photos", "Fotos ansehen / entfernen", "Skatīt / noņemt foto"});
        map.put("Фотографий пока нет", new String[]{"No photos yet", "Noch keine Fotos", "Foto vēl nav"});
        map.put("Фото ", new String[]{"Photo ", "Foto ", "Foto "});
        map.put("Открыть", new String[]{"Open", "Öffnen", "Atvērt"});
        map.put("Убрать из работы", new String[]{"Remove from job", "Aus Arbeit entfernen", "Noņemt no darba"});
        map.put("СОХРАНИТЬ", new String[]{"SAVE", "SPEICHERN", "SAGLABĀT"});
        map.put("Заполни турбину и название работы", new String[]{"Enter turbine and job title", "Turbine und Arbeitsbezeichnung eingeben", "Ievadi turbīnu un darba nosaukumu"});
        map.put("Ошибка сохранения", new String[]{"Could not save", "Speichern fehlgeschlagen", "Neizdevās saglabāt"});
        map.put("ОТМЕНА", new String[]{"CANCEL", "ABBRECHEN", "ATCELT"});
        map.put("Добавлено фотографий: ", new String[]{"Photos added: ", "Fotos hinzugefügt: ", "Pievienoti foto: "});
        map.put(". Нажми СОХРАНИТЬ для записи работы.", new String[]{". Tap SAVE to save the job.", ". Tippe SPEICHERN, um die Arbeit zu speichern.", ". Nospied SAGLABĀT, lai saglabātu darbu."});
        map.put("Не удалось сохранить доступ к фотографии", new String[]{"Could not retain access to photo", "Fotozugriff konnte nicht gespeichert werden", "Neizdevās saglabāt piekļuvi foto"});
        map.put("ФОТОГРАФИИ GRID", new String[]{"GRID PHOTOS", "GRID FOTOS", "GRID FOTOGRĀFIJAS"});
        map.put(" — ", new String[]{" — ", " — ", " — "});
        map.put("Фотографий пока нет. Добавь их к работе.", new String[]{"No photos yet. Attach photos to a job.", "Noch keine Fotos. Füge einer Arbeit Fotos hinzu.", "Foto vēl nav. Pievieno tos darbam."});
        map.put("Фото недоступно", new String[]{"Photo unavailable", "Foto nicht verfügbar", "Foto nav pieejams"});
        map.put("Не удалось открыть фотографию", new String[]{"Could not open photo", "Foto konnte nicht geöffnet werden", "Neizdevās atvērt foto"});
        map.put("Посмотреть описание", new String[]{"View description", "Beschreibung anzeigen", "Skatīt aprakstu"});
        map.put("Фотографии работы", new String[]{"Job photos", "Arbeitsfotos", "Darba foto"});
        map.put("Редактировать", new String[]{"Edit", "Bearbeiten", "Rediģēt"});
        map.put("Вернуть в работу", new String[]{"Mark in progress", "Als in Bearbeitung markieren", "Atzīmēt kā procesā"});
        map.put("Отметить выполненной", new String[]{"Mark completed", "Als erledigt markieren", "Atzīmēt kā pabeigtu"});
        map.put("Удалить", new String[]{"Delete", "Löschen", "Dzēst"});
        map.put("Описание", new String[]{"Description", "Beschreibung", "Apraksts"});
        map.put("ФОТО РАБОТЫ", new String[]{"JOB PHOTOS", "ARBEITSFOTOS", "DARBA FOTO"});
        map.put("Фотографий нет", new String[]{"No photos", "Keine Fotos", "Nav foto"});
        map.put("← Работы", new String[]{"← Jobs", "← Arbeiten", "← Darbi"});
        map.put("Ошибка", new String[]{"Error", "Fehler", "Kļūda"});
        map.put("Удалить эту работу?", new String[]{"Delete this job?", "Diese Arbeit löschen?", "Dzēst šo darbu?"});
        map.put("Отмена", new String[]{"Cancel", "Abbrechen", "Atcelt"});
        map.put("Не удалось загрузить работы", new String[]{"Could not load jobs", "Arbeiten konnten nicht geladen werden", "Neizdevās ielādēt darbus"});
        return map;
    }

}
```
