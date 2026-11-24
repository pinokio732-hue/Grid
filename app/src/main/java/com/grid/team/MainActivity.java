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
        button(tr("孝校袪袘袠袧蝎"), this::showJobs);
        button(tr("袪袗袘袨孝蝎"), this::showJobs);
        button(tr("袛袨袘袗袙袠孝鞋 袪袗袘袨孝校"), () -> editJob(-1));
        button(tr("袣袗袥袝袧袛袗袪鞋"), this::showJobs);
        button(tr("肖袨孝袨袚袪袗肖袠袠"), this::showGallery);
        button(tr("袣袨袦袗袧袛袗"), () -> message(tr("袗泻泻邪褍薪褌褘 写芯斜邪胁懈屑 锌芯蟹卸械")));
        button(tr("小袠袧啸袪袨袧袠袟袗笑袠携"), () -> message(tr("袩芯泻邪 写邪薪薪褘械 褌芯谢褜泻芯 薪邪 褝褌芯屑 褌械谢械褎芯薪械")));
        button(tr("楔袗袘袥袨袧 袪袗袘袨孝"), this::importTemplate);
        button(tr("携袟蝎袣 / LANGUAGE"), this::chooseLanguage);
    }

    private void showJobs() {
        page(tr("袪袗袘袨孝蝎 GRID"));
        button(tr("+ 袛芯斜邪胁懈褌褜 褉邪斜芯褌褍"), () -> editJob(-1));
        if (jobs.isEmpty()) layout.addView(text(tr("袩芯泻邪 薪械褌 褉邪斜芯褌. 袛芯斜邪胁褜 锌械褉胁褍褞!"), 17));
        for (int i = 0; i < jobs.size(); i++) {
            final int index = i;
            JSONObject j = jobs.get(i);
            String status = j.optBoolean("done") ? tr("鉁� 袙蝎袩袨袥袧袝袧袨") : tr("鈼� 袙 袪袗袘袨孝袝");
            String info = j.optString("turbine") + "\n" + j.optString("name") + "\n"
                    + j.optString("date") + "\n" + status + tr(" 鈥� 肖芯褌芯: ") + photos(j).length();
            button(info, () -> jobOptions(index));
        }
        button(tr("鈫� 袚谢邪胁薪芯械 屑械薪褞"), this::showHome);
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
        page(index >= 0 ? tr("袪袝袛袗袣孝袠袪袨袙袗孝鞋") : tr("袧袨袙袗携 袪袗袘袨孝袗"));
        EditText turbine = field(tr("袧芯屑械褉 懈谢懈 薪邪蟹胁邪薪懈械 褌褍褉斜懈薪褘"), old.optString("turbine"));
        EditText name = field(tr("袧邪蟹胁邪薪懈械 褉邪斜芯褌褘"), old.optString("name"));
        EditText date = field(tr("袛邪褌邪 褉邪斜芯褌褘"), old.optString("date"));
        date.setFocusable(false);
        date.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) ->
                date.setText(String.format(Locale.US, "%02d.%02d.%04d", day, month + 1, year)),
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
        EditText description = field(tr("袨锌懈褋邪薪懈械 褉邪斜芯褌褘"), old.optString("description"));
        description.setMinLines(3);
        description.setGravity(Gravity.TOP);
        button(tr("馃摲 袛芯斜邪胁懈褌褜 褎芯褌芯谐褉邪褎懈懈"), this::pickPhotos);
        TextView photoCount = text(tr("袩褉懈泻褉械锌谢械薪芯 褎芯褌芯谐褉邪褎懈泄: ") + draftPhotos.size(), 16);
        layout.addView(photoCount);
        button(tr("袩褉芯褋屑芯褌褉械褌褜 / 褍斜褉邪褌褜 褎芯褌芯谐褉邪褎懈懈"), () -> {
            if (draftPhotos.isEmpty()) { message(tr("肖芯褌芯谐褉邪褎懈泄 锌芯泻邪 薪械褌")); return; }
            String[] items = new String[draftPhotos.size()];
            for (int i = 0; i < items.length; i++) items[i] = tr("肖芯褌芯 ") + (i + 1);
            new AlertDialog.Builder(this).setTitle(tr("肖袨孝袨袚袪袗肖袠袠"))
                .setItems(items, (d, which) -> new AlertDialog.Builder(this)
                    .setTitle(tr("肖芯褌芯 ") + (which + 1))
                    .setItems(new String[]{tr("袨褌泻褉褘褌褜"), tr("校斜褉邪褌褜 懈蟹 褉邪斜芯褌褘")}, (dialog, action) -> {
                        if (action == 0) openPhoto(draftPhotos.get(which));
                        else {
                            draftPhotos.remove(which);
                            photoCount.setText(tr("袩褉懈泻褉械锌谢械薪芯 褎芯褌芯谐褉邪褎懈泄: ") + draftPhotos.size());
                        }
                    }).show()).show();
        });
        button(tr("小袨啸袪袗袧袠孝鞋"), () -> {
            if (turbine.getText().toString().trim().isEmpty()
                || name.getText().toString().trim().isEmpty()) {
                message(tr("袟邪锌芯谢薪懈 褌褍褉斜懈薪褍 懈 薪邪蟹胁邪薪懈械 褉邪斜芯褌褘")); return;
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
            } catch (Exception e) { message(tr("袨褕懈斜泻邪 褋芯褏褉邪薪械薪懈褟")); }
        });
        button(tr("袨孝袦袝袧袗"), this::showJobs);
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
        message(tr("袛芯斜邪胁谢械薪芯 褎芯褌芯谐褉邪褎懈泄: ") + added + tr(". 袧邪卸屑懈 小袨啸袪袗袧袠孝鞋 写谢褟 蟹邪锌懈褋懈 褉邪斜芯褌褘."));
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
            message(tr("袧械 褍写邪谢芯褋褜 褋芯褏褉邪薪懈褌褜 写芯褋褌褍锌 泻 褎芯褌芯谐褉邪褎懈懈"));
        }
        return false;
    }

    private JSONArray photos(JSONObject j) {
        JSONArray a = j.optJSONArray("photos");
        return a == null ? new JSONArray() : a;
    }

    private void showGallery() {
        page(tr("肖袨孝袨袚袪袗肖袠袠 GRID"));
        int count = 0;
        for (int i = 0; i < jobs.size(); i++) {
            JSONObject j = jobs.get(i);
            JSONArray arr = photos(j);
            if (arr.length() == 0) continue;
            layout.addView(text(j.optString("turbine") + tr(" 鈥� ") + j.optString("name"), 19));
            for (int k = 0; k < arr.length(); k++) {
                String uri = arr.optString(k);
                if (uri.isEmpty()) continue;
                count++;
                addThumbnail(uri);
            }
        }
        if (count == 0) layout.addView(text(tr("肖芯褌芯谐褉邪褎懈泄 锌芯泻邪 薪械褌. 袛芯斜邪胁褜 懈褏 泻 褉邪斜芯褌械."), 17));
        button(tr("鈫� 袚谢邪胁薪芯械 屑械薪褞"), this::showHome);
    }

    private void addThumbnail(String uri) {
        ImageView image = new ImageView(this);
        image.setAdjustViewBounds(true);
        image.setMaxHeight(480);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        try { image.setImageURI(Uri.parse(uri)); }
        catch (Exception e) { layout.addView(text(tr("肖芯褌芯 薪械写芯褋褌褍锌薪芯"), 14)); return; }
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
        } catch (Exception e) { message(tr("袧械 褍写邪谢芯褋褜 芯褌泻褉褘褌褜 褎芯褌芯谐褉邪褎懈褞")); }
    }

    private void jobOptions(int index) {
        JSONObject j = jobs.get(index);
        String[] options = {tr("袩芯褋屑芯褌褉械褌褜 芯锌懈褋邪薪懈械"), tr("肖芯褌芯谐褉邪褎懈懈 褉邪斜芯褌褘"), tr("袪械写邪泻褌懈褉芯胁邪褌褜"),
            j.optBoolean("done") ? tr("袙械褉薪褍褌褜 胁 褉邪斜芯褌褍") : tr("袨褌屑械褌懈褌褜 胁褘锌芯谢薪械薪薪芯泄"), tr("校写邪谢懈褌褜")};
        new AlertDialog.Builder(this).setTitle(j.optString("name"))
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    new AlertDialog.Builder(this).setTitle(tr("袨锌懈褋邪薪懈械"))
                        .setMessage(j.optString("description", ""))
                        .setPositiveButton("OK", null).show();
                } else if (which == 1) {
                    page(tr("肖袨孝袨 袪袗袘袨孝蝎"));
                    JSONArray a = photos(j);
                    if (a.length() == 0) layout.addView(text(tr("肖芯褌芯谐褉邪褎懈泄 薪械褌"), 17));
                    for (int i = 0; i < a.length(); i++) addThumbnail(a.optString(i));
                    button(tr("鈫� 袪邪斜芯褌褘"), this::showJobs);
                } else if (which == 2) editJob(index);
                else if (which == 3) {
                    try {
                        j.put("done", !j.optBoolean("done"));
                        saveJobs(); showJobs();
                    } catch (Exception e) { message(tr("袨褕懈斜泻邪")); }
                } else {
                    new AlertDialog.Builder(this).setMessage(tr("校写邪谢懈褌褜 褝褌褍 褉邪斜芯褌褍?"))
                        .setNegativeButton(tr("袨褌屑械薪邪"), null)
                        .setPositiveButton(tr("校写邪谢懈褌褜"), (d, w) -> {
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
        } catch (Exception e) { message(tr("袧械 褍写邪谢芯褋褜 蟹邪谐褉褍蟹懈褌褜 褉邪斜芯褌褘")); }
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
        input.setHint(tr("袧芯屑械褉 褌褍褉斜懈薪褘"));
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(tr("楔袗袘袥袨袧 袪袗袘袨孝"))
            .setMessage(tr("袛芯斜邪胁懈褌褜 薪芯胁褘泄 褋锌懈褋芯泻 褉邪斜芯褌 斜械蟹 芯褌屑械褌芯泻 胁褘锌芯谢薪械薪懈褟?"))
            .setView(input).setNegativeButton(tr("袨褌屑械薪邪"), null)
            .setPositiveButton(tr("袛芯斜邪胁懈褌褜 褕邪斜谢芯薪"), null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String turbine = input.getText().toString().trim();
            if (turbine.isEmpty()) { input.setError(tr("袧芯屑械褉 褌褍褉斜懈薪褘")); return; }
            for (JSONObject job : jobs) {
                if (turbine.equalsIgnoreCase(job.optString("turbine").trim())) {
                    input.setError(tr("袛谢褟 褝褌芯泄 褌褍褉斜懈薪褘 褍卸械 械褋褌褜 褉邪斜芯褌褘")); return;
                }
            }
            ArrayList<JSONObject> added = new ArrayList<>();
            try {
                for (String[] row : TEMPLATE) {
                    JSONObject job = new JSONObject();
                    job.put("turbine", turbine);
                    job.put("name", row[0] + " 鈥� " + row[1]);
                    job.put("description", row[1]);
                    job.put("date", "");
                    job.put("done", false);
                    job.put("photos", new JSONArray());
                    added.add(job);
                }
            } catch (Exception e) { message(tr("袨褕懈斜泻邪")); return; }
            jobs.addAll(added);
            saveJobs();
            dialog.dismiss();
            showJobs();
        }));
        dialog.show();
    }

    private void chooseLanguage() {
        String[] names = {"馃嚪馃嚭 袪褍褋褋泻懈泄", "馃嚞馃嚙 English", "馃嚛馃嚜 Deutsch", "馃嚤馃嚮 Latvie拧u"};
        String[] codes = {"ru", "en", "de", "lv"};
        int selected = 0;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(language)) selected = i;
        }
        new AlertDialog.Builder(this)
            .setTitle(tr("携袟蝎袣 / LANGUAGE"))
            .setSingleChoiceItems(names, selected, (dialog, which) -> {
                language = codes[which];
                storage.edit().putString("language", language).apply();
                dialog.dismiss();
                showHome();
            })
            .setNegativeButton(tr("袨褌屑械薪邪"), null)
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
        map.put("孝校袪袘袠袧蝎", new String[]{"TURBINES", "TURBINEN", "TURB莫NAS"});
        map.put("袪袗袘袨孝蝎", new String[]{"JOBS", "ARBEITEN", "DARBI"});
        map.put("袛袨袘袗袙袠孝鞋 袪袗袘袨孝校", new String[]{"ADD JOB", "ARBEIT HINZUF脺GEN", "PIEVIENOT DARBU"});
        map.put("袣袗袥袝袧袛袗袪鞋", new String[]{"CALENDAR", "KALENDER", "KALEND膧RS"});
        map.put("肖袨孝袨袚袪袗肖袠袠", new String[]{"PHOTOS", "FOTOS", "FOTOGR膧FIJAS"});
        map.put("袣袨袦袗袧袛袗", new String[]{"TEAM", "TEAM", "KOMANDA"});
        map.put("小袠袧啸袪袨袧袠袟袗笑袠携", new String[]{"SYNC", "SYNCHRONISIERUNG", "SINHRONIZ膧CIJA"});
        map.put("携袟蝎袣 / LANGUAGE", new String[]{"LANGUAGE", "SPRACHE", "VALODA"});
        map.put("袗泻泻邪褍薪褌褘 写芯斜邪胁懈屑 锌芯蟹卸械", new String[]{"Accounts will be added later", "Konten kommen sp盲ter", "Konti tiks pievienoti v膿l膩k"});
        map.put("袩芯泻邪 写邪薪薪褘械 褌芯谢褜泻芯 薪邪 褝褌芯屑 褌械谢械褎芯薪械", new String[]{"Data is stored on this phone only", "Daten sind nur auf diesem Handy gespeichert", "Dati pagaid膩m glab膩jas tikai 拧aj膩 t膩lrun墨"});
        map.put("袪袗袘袨孝蝎 GRID", new String[]{"GRID JOBS", "GRID ARBEITEN", "GRID DARBI"});
        map.put("+ 袛芯斜邪胁懈褌褜 褉邪斜芯褌褍", new String[]{"+ Add job", "+ Arbeit hinzuf眉gen", "+ Pievienot darbu"});
        map.put("袩芯泻邪 薪械褌 褉邪斜芯褌. 袛芯斜邪胁褜 锌械褉胁褍褞!", new String[]{"No jobs yet. Add your first!", "Noch keine Arbeiten. Erste hinzuf眉gen!", "Darbu v膿l nav. Pievieno pirmo!"});
        map.put("鉁� 袙蝎袩袨袥袧袝袧袨", new String[]{"鉁� COMPLETED", "鉁� ERLEDIGT", "鉁� PABEIGTS"});
        map.put("鈼� 袙 袪袗袘袨孝袝", new String[]{"鈼� IN PROGRESS", "鈼� IN BEARBEITUNG", "鈼� PROCES膧"});
        map.put(" 鈥� 肖芯褌芯: ", new String[]{" 鈥� Photos: ", " 鈥� Fotos: ", " 鈥� Foto: "});
        map.put("鈫� 袚谢邪胁薪芯械 屑械薪褞", new String[]{"鈫� Home", "鈫� Hauptmen眉", "鈫� Galven膩 izv膿lne"});
        map.put("袪袝袛袗袣孝袠袪袨袙袗孝鞋", new String[]{"EDIT JOB", "ARBEIT BEARBEITEN", "REDI蘑膾T DARBU"});
        map.put("袧袨袙袗携 袪袗袘袨孝袗", new String[]{"NEW JOB", "NEUE ARBEIT", "JAUNS DARBS"});
        map.put("袧芯屑械褉 懈谢懈 薪邪蟹胁邪薪懈械 褌褍褉斜懈薪褘", new String[]{"Turbine number or name", "Turbinennummer oder Name", "Turb墨nas numurs vai nosaukums"});
        map.put("袧邪蟹胁邪薪懈械 褉邪斜芯褌褘", new String[]{"Job title", "Arbeitsbezeichnung", "Darba nosaukums"});
        map.put("袛邪褌邪 褉邪斜芯褌褘", new String[]{"Job date", "Arbeitsdatum", "Darba datums"});
        map.put("袨锌懈褋邪薪懈械 褉邪斜芯褌褘", new String[]{"Job description", "Arbeitsbeschreibung", "Darba apraksts"});
        map.put("馃摲 袛芯斜邪胁懈褌褜 褎芯褌芯谐褉邪褎懈懈", new String[]{"馃摲 Add photos", "馃摲 Fotos hinzuf眉gen", "馃摲 Pievienot foto"});
        map.put("袩褉懈泻褉械锌谢械薪芯 褎芯褌芯谐褉邪褎懈泄: ", new String[]{"Attached photos: ", "Angeh盲ngte Fotos: ", "Pievienotie foto: "});
        map.put("袩褉芯褋屑芯褌褉械褌褜 / 褍斜褉邪褌褜 褎芯褌芯谐褉邪褎懈懈", new String[]{"View / remove photos", "Fotos ansehen / entfernen", "Skat墨t / no艈emt foto"});
        map.put("肖芯褌芯谐褉邪褎懈泄 锌芯泻邪 薪械褌", new String[]{"No photos yet", "Noch keine Fotos", "Foto v膿l nav"});
        map.put("肖芯褌芯 ", new String[]{"Photo ", "Foto ", "Foto "});
        map.put("袨褌泻褉褘褌褜", new String[]{"Open", "脰ffnen", "Atv膿rt"});
        map.put("校斜褉邪褌褜 懈蟹 褉邪斜芯褌褘", new String[]{"Remove from job", "Aus Arbeit entfernen", "No艈emt no darba"});
        map.put("小袨啸袪袗袧袠孝鞋", new String[]{"SAVE", "SPEICHERN", "SAGLAB膧T"});
        map.put("袟邪锌芯谢薪懈 褌褍褉斜懈薪褍 懈 薪邪蟹胁邪薪懈械 褉邪斜芯褌褘", new String[]{"Enter turbine and job title", "Turbine und Arbeitsbezeichnung eingeben", "Ievadi turb墨nu un darba nosaukumu"});
        map.put("袨褕懈斜泻邪 褋芯褏褉邪薪械薪懈褟", new String[]{"Could not save", "Speichern fehlgeschlagen", "Neizdev膩s saglab膩t"});
        map.put("袨孝袦袝袧袗", new String[]{"CANCEL", "ABBRECHEN", "ATCELT"});
        map.put("袛芯斜邪胁谢械薪芯 褎芯褌芯谐褉邪褎懈泄: ", new String[]{"Photos added: ", "Fotos hinzugef眉gt: ", "Pievienoti foto: "});
        map.put(". 袧邪卸屑懈 小袨啸袪袗袧袠孝鞋 写谢褟 蟹邪锌懈褋懈 褉邪斜芯褌褘.", new String[]{". Tap SAVE to save the job.", ". Tippe SPEICHERN, um die Arbeit zu speichern.", ". Nospied SAGLAB膧T, lai saglab膩tu darbu."});
        map.put("袧械 褍写邪谢芯褋褜 褋芯褏褉邪薪懈褌褜 写芯褋褌褍锌 泻 褎芯褌芯谐褉邪褎懈懈", new String[]{"Could not retain access to photo", "Fotozugriff konnte nicht gespeichert werden", "Neizdev膩s saglab膩t piek募uvi foto"});
        map.put("肖袨孝袨袚袪袗肖袠袠 GRID", new String[]{"GRID PHOTOS", "GRID FOTOS", "GRID FOTOGR膧FIJAS"});
        map.put(" 鈥� ", new String[]{" 鈥� ", " 鈥� ", " 鈥� "});
        map.put("肖芯褌芯谐褉邪褎懈泄 锌芯泻邪 薪械褌. 袛芯斜邪胁褜 懈褏 泻 褉邪斜芯褌械.", new String[]{"No photos yet. Attach photos to a job.", "Noch keine Fotos. F眉ge einer Arbeit Fotos hinzu.", "Foto v膿l nav. Pievieno tos darbam."});
        map.put("肖芯褌芯 薪械写芯褋褌褍锌薪芯", new String[]{"Photo unavailable", "Foto nicht verf眉gbar", "Foto nav pieejams"});
        map.put("袧械 褍写邪谢芯褋褜 芯褌泻褉褘褌褜 褎芯褌芯谐褉邪褎懈褞", new String[]{"Could not open photo", "Foto konnte nicht ge枚ffnet werden", "Neizdev膩s atv膿rt foto"});
        map.put("袩芯褋屑芯褌褉械褌褜 芯锌懈褋邪薪懈械", new String[]{"View description", "Beschreibung anzeigen", "Skat墨t aprakstu"});
        map.put("肖芯褌芯谐褉邪褎懈懈 褉邪斜芯褌褘", new String[]{"Job photos", "Arbeitsfotos", "Darba foto"});
        map.put("袪械写邪泻褌懈褉芯胁邪褌褜", new String[]{"Edit", "Bearbeiten", "Redi模膿t"});
        map.put("袙械褉薪褍褌褜 胁 褉邪斜芯褌褍", new String[]{"Mark in progress", "Als in Bearbeitung markieren", "Atz墨m膿t k膩 proces膩"});
        map.put("袨褌屑械褌懈褌褜 胁褘锌芯谢薪械薪薪芯泄", new String[]{"Mark completed", "Als erledigt markieren", "Atz墨m膿t k膩 pabeigtu"});
        map.put("校写邪谢懈褌褜", new String[]{"Delete", "L枚schen", "Dz膿st"});
        map.put("袨锌懈褋邪薪懈械", new String[]{"Description", "Beschreibung", "Apraksts"});
        map.put("肖袨孝袨 袪袗袘袨孝蝎", new String[]{"JOB PHOTOS", "ARBEITSFOTOS", "DARBA FOTO"});
        map.put("肖芯褌芯谐褉邪褎懈泄 薪械褌", new String[]{"No photos", "Keine Fotos", "Nav foto"});
        map.put("鈫� 袪邪斜芯褌褘", new String[]{"鈫� Jobs", "鈫� Arbeiten", "鈫� Darbi"});
        map.put("袨褕懈斜泻邪", new String[]{"Error", "Fehler", "K募奴da"});
        map.put("校写邪谢懈褌褜 褝褌褍 褉邪斜芯褌褍?", new String[]{"Delete this job?", "Diese Arbeit l枚schen?", "Dz膿st 拧o darbu?"});
        map.put("袨褌屑械薪邪", new String[]{"Cancel", "Abbrechen", "Atcelt"});
        map.put("袧械 褍写邪谢芯褋褜 蟹邪谐褉褍蟹懈褌褜 褉邪斜芯褌褘", new String[]{"Could not load jobs", "Arbeiten konnten nicht geladen werden", "Neizdev膩s iel膩d膿t darbus"});
        map.put("楔袗袘袥袨袧 袪袗袘袨孝", new String[]{"JOB TEMPLATE", "ARBEITSVORLAGE", "DARBU VEIDNE"});
        map.put("袧芯屑械褉 褌褍褉斜懈薪褘", new String[]{"Turbine number", "Turbinennummer", "Turb墨nas numurs"});
        map.put("袛芯斜邪胁懈褌褜 薪芯胁褘泄 褋锌懈褋芯泻 褉邪斜芯褌 斜械蟹 芯褌屑械褌芯泻 胁褘锌芯谢薪械薪懈褟?", new String[]{"Add a new unchecked job list?", "Neue Aufgabenliste ohne Erledigungsmarkierungen hinzuf眉gen?", "Pievienot jaunu darbu sarakstu bez izpildes atz墨m膿m?"});
        map.put("袛芯斜邪胁懈褌褜 褕邪斜谢芯薪", new String[]{"Add template", "Vorlage hinzuf眉gen", "Pievienot veidni"});
        map.put("袛谢褟 褝褌芯泄 褌褍褉斜懈薪褘 褍卸械 械褋褌褜 褉邪斜芯褌褘", new String[]{"Jobs already exist for this turbine", "F眉r diese Turbine gibt es bereits Aufgaben", "艩ai turb墨nai jau ir darbi"});
        return map;
    }

}
