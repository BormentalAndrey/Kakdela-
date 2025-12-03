package com.kakdela.messenger;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button b = findViewById(R.id.btn_start_chat);
        b.setOnClickListener(v -> Toast.makeText(this, "Start chat (stub)", Toast.LENGTH_SHORT).show());
    }
}
