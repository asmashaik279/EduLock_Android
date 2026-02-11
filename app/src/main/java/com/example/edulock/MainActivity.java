package com.example.edulock;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button continueBtn = findViewById(R.id.btnContinue);

        continueBtn.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    HomeDashboardActivity.class
            );
            startActivity(intent);
            finish(); // optional: prevents going back
        });
    }
}
