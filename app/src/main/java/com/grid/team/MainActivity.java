
package com.grid.team;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(17, 25, 36));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 65, 32, 32);

        TextView title = new TextView(this);
        title.setText("GRID");
        title.setTextSize(42);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("TEAM WORK MANAGEMENT");
        subtitle.setTextSize(15);
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 0, 0, 55);
        layout.addView(subtitle);

        addButton(layout, "ТУРБИНЫ");
        addButton(layout, "РАБОТЫ");
        addButton(layout, "КАЛЕНДАРЬ");
        addButton(layout, "ФОТОГРАФИИ");
        addButton(layout, "КОМАНДА");
        addButton(layout, "СИНХРОНИЗАЦИЯ");

        scroll.addView(layout);
        setContentView(scroll);
    }

    private void addButton(LinearLayout layout, String name) {
        Button button = new Button(this);
        button.setText(name);
        button.setTextSize(18);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(
                Color.rgb(38, 65, 86)
            )
        );

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 125
            );
        params.setMargins(0, 0, 0, 20);

        layout.addView(button, params);

        button.setOnClickListener(v ->
            Toast.makeText(this,
                name + " — скоро добавим!",
                Toast.LENGTH_SHORT).show()
        );
    }
}
