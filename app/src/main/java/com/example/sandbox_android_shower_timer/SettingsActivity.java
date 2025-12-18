package com.example.sandbox_android_shower_timer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {

    private EditText etTotalMinutes, etintervalSeconds, etYoutube;
    private Switch switchVoice;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        etTotalMinutes = findViewById(R.id.etTotalMinutes);
        etintervalSeconds = findViewById(R.id.etintervalSeconds);
        etYoutube = findViewById(R.id.etYoutube);
        switchVoice = findViewById(R.id.switchVoice);
        btnSave = findViewById(R.id.btnSave);

        loadCurrentSettings();

        btnSave.setOnClickListener(v -> saveSettings());
    }

    private void loadCurrentSettings() {
        SharedPreferences prefs = getSharedPreferences("shower_prefs", MODE_PRIVATE);

        long total = prefs.getLong("total_minutes", 10);
        long interval = prefs.getLong("interval_seconds", 3);
        boolean voice = prefs.getBoolean("voice_on", true);
        String youtube = prefs.getString("youtube_source", "");

        etTotalMinutes.setText(String.valueOf(total));
        etintervalSeconds.setText(String.valueOf(interval));
        etYoutube.setText(youtube);
        switchVoice.setChecked(voice);
    }

    private void saveSettings() {

        String totalStr = etTotalMinutes.getText().toString().trim();
        String intervalStr = etintervalSeconds.getText().toString().trim();
        String youtube = etYoutube.getText().toString().trim();
        boolean voice = switchVoice.isChecked();

        if (totalStr.isEmpty() || intervalStr.isEmpty()) {
            Toast.makeText(this, "시간을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        long total = Long.parseLong(totalStr);
        long interval = Long.parseLong(intervalStr);

        SharedPreferences prefs = getSharedPreferences("shower_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putLong("total_minutes", total);
        editor.putLong("interval_seconds", interval);
        editor.putBoolean("voice_on", voice);
        editor.putString("youtube_source", youtube);

        editor.apply();

        Toast.makeText(this, "설정이 저장되었습니다.", Toast.LENGTH_SHORT).show();
        finish();  // 메인 화면으로 돌아가기
    }
}
