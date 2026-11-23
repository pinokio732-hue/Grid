
package com.grid.team;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 80, 40, 40);

        TextView title = new TextView(this);
        title.setText("GRID");
        title.setTextSize(32);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Team Work Management");
        subtitle.setTextSize(18);
        layout.addView(subtitle);

        Button button = new Button(this);
        button.setText("Работы команды");
        layout.addView(button);

        button.setOnClickListener(v ->
            button.setText("Grid запущен!")
        );

        setContentView(layout);
    }
}
