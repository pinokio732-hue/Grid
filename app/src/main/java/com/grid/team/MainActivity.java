
package com.grid.team;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.text.InputType;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends Activity {

    LinearLayout layout;
    ArrayList<JSONObject> jobs = new ArrayList<>();
    SharedPreferences storage;

    int bg = Color.rgb(17, 25, 36);
    int panel = Color.rgb(38, 65, 86);

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        storage = getSharedPreferences("grid_data", MODE_PRIVATE);
        loadJobs();
        showHome();
    }

    TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.WHITE);
        t.setPadding(12, 16, 12, 16);
        return t;
    }

    void page(String heading) {
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

    void button(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setTextSize(17);
        b.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(panel)
        );

        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(-1, 115);
        p.setMargins(0, 8, 0, 12);
        layout.addView(b, p);

        b.setOnClickListener(v -> action.run());
    }

    void showHome() {
        page("GRID");

        TextView sub = text("TEAM WORK MANAGEMENT", 15);
        sub.setGravity(Gravity.CENTER);
        layout.addView(sub);

        button("ТУРБИНЫ", () -> showJobs());
        button("РАБОТЫ", () -> showJobs());
        button("ДОБАВИТЬ РАБОТУ", () -> editJob(-1));
        button("КАЛЕНДАРЬ", () -> showJobs());
        button("ФОТОГРАФИИ", () -> message("Добавим в следующей версии"));
        button("КОМАНДА", () -> message("Аккаунты добавим позже"));
        button("СИНХРОНИЗАЦИЯ", () -> message("Пока данные только на этом телефоне"));
    }

    void showJobs() {
        page("РАБОТЫ GRID");

        button("+ Добавить работу", () -> editJob(-1));

        if (jobs.isEmpty()) {
            layout.addView(text("Пока нет работ. Добавь первую!", 17));
        }

        for (int i = 0; i < jobs.size(); i++) {
            final int index = i;
            JSONObject j = jobs.get(i);

            String status = j.optBoolean("done")
                ? "✓ ВЫПОЛНЕНО" : "○ В РАБОТЕ";

            String info =
                j.optString("turbine") + "\n" +
                j.optString("name") + "\n" +
                j.optString("date") + "\n" +
                status;

            button(info, () -> jobOptions(index));
        }

        button("← Главное меню", () -> showHome());
    }

    void editJob(int index) {
        boolean editing = index >= 0;
        JSONObject old = editing ? jobs.get(index) : new JSONObject();

        page(editing ? "РЕДАКТИРОВАТЬ" : "НОВАЯ РАБОТА");

        EditText turbine = field("Номер или название турбины",
            old.optString("turbine"));

        EditText name = field("Название работы",
            old.optString("name"));

        EditText date = field("Дата работы",
            old.optString("date"));

        date.setFocusable(false);
        date.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            android.app.DatePickerDialog picker =
                new android.app.DatePickerDialog(this,
                    (view, year, month, day) ->
                        date.setText(String.format(
                            java.util.Locale.US,
                            "%02d.%02d.%04d",
                            day, month + 1, year)),
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH));
            picker.show();
        });

        EditText description = field("Описание работы",
            old.optString("description"));
        description.setMinLines(3);
        description.setGravity(Gravity.TOP);

        button("СОХРАНИТЬ", () -> {
            if (turbine.getText().toString().trim().isEmpty()
                || name.getText().toString().trim().isEmpty()) {
                message("Заполни турбину и название работы");
                return;
            }

            try {
                JSONObject j = new JSONObject();
                j.put("turbine", turbine.getText().toString());
                j.put("name", name.getText().toString());
                j.put("date", date.getText().toString());
                j.put("description", description.getText().toString());
                j.put("done", old.optBoolean("done", false));

                if (editing) jobs.set(index, j);
                else jobs.add(j);

                saveJobs();
                showJobs();
            } catch (Exception e) {
                message("Ошибка сохранения");
            }
        });

        button("ОТМЕНА", () -> showJobs());
    }

    EditText field(String hint, String value) {
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

    void jobOptions(int index) {
        JSONObject j = jobs.get(index);

        String[] options = {
            "Посмотреть описание",
            "Редактировать",
            j.optBoolean("done") ? "Вернуть в работу" : "Отметить выполненной",
            "Удалить"
        };

        new AlertDialog.Builder(this)
            .setTitle(j.optString("name"))
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    new AlertDialog.Builder(this)
                        .setTitle("Описание")
                        .setMessage(j.optString("description", ""))
                        .setPositiveButton("OK", null)
                        .show();
                } else if (which == 1) {
                    editJob(index);
                } else if (which == 2) {
                    try {
                        j.put("done", !j.optBoolean("done"));
                        saveJobs();
                        showJobs();
                    } catch (Exception e) {
                        message("Ошибка");
                    }
                } else {
                    new AlertDialog.Builder(this)
                        .setMessage("Удалить эту работу?")
                        .setNegativeButton("Отмена", null)
                        .setPositiveButton("Удалить", (d, w) -> {
                            jobs.remove(index);
                            saveJobs();
                            showJobs();
                        })
                        .show();
                }
            })
            .show();
    }

    void saveJobs() {
        JSONArray array = new JSONArray();
        for (JSONObject j : jobs) array.put(j);
        storage.edit().putString("jobs", array.toString()).apply();
    }

    void loadJobs() {
        try {
            JSONArray array = new JSONArray(
                storage.getString("jobs", "[]"));
            for (int i = 0; i < array.length(); i++)
                jobs.add(array.getJSONObject(i));
        } catch (Exception e) {
            message("Не удалось загрузить работы");
        }
    }

    void message(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
