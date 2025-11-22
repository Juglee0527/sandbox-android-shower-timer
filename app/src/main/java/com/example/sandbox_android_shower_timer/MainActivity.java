package com.example.sandbox_android_shower_timer; // <= 본인 패키지명으로 수정

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvMainTime;
    private Button btnStartPause, btnReset, btnOpenYoutube, btnSettings;

    // 스톱워치용 핸들러/러너블
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    // 시간 관련
    private long startTimeMillis = 0L;   // 스톱워치 시작 기준 시각
    private long elapsedMillis = 0L;     // 경과 시간(ms)
    private boolean isRunning = false;

    // 설정 값 (분 단위)
    private long totalMinutes = 10;      // 목표 샤워 시간 (분)
    private long intervalMinutes = 3;    // 알림 간격 (분)
    private boolean voiceOn = true;      // 음성 안내 여부
    private String youtubeSource;        // 유튜브 링크 or 검색어

    // 내부 상태 (ms 단위)
    private long nextIntervalMillis;     // 다음 알림 시각
    private boolean targetAnnounced = false;

    // TTS
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        loadSettings();   // SharedPreferences에서 값 읽기 (없으면 기본값)
        initTts();
        initTimerRunnable();
        initButtons();

        updateMainTimeText();
    }

    private void initViews() {
        tvMainTime = findViewById(R.id.tvMainTime);
        btnStartPause = findViewById(R.id.btnStartPause);
        btnReset = findViewById(R.id.btnReset);
        btnOpenYoutube = findViewById(R.id.btnOpenYoutube);
        btnSettings = findViewById(R.id.btnSettings);
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences("shower_prefs", MODE_PRIVATE);

        // 아직 SettingsActivity가 없으므로, 기본값 중심으로 동작
        totalMinutes = prefs.getLong("total_minutes", 10);          // 기본 10분
        intervalMinutes = prefs.getLong("interval_minutes", 3);     // 기본 3분
        voiceOn = prefs.getBoolean("voice_on", true);
        youtubeSource = prefs.getString(
                "youtube_source",
                "https://www.youtube.com/results?search_query=shower+music+playlist"
        );

        // 내부 상태 초기화
        nextIntervalMillis = intervalMinutes * 60 * 1000L;
        targetAnnounced = false;
    }

    private void initTts() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.KOREAN);
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "TTS 한국어를 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "TTS 초기화 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initTimerRunnable() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                // 현재 시각 기준 경과 시간 계산
                elapsedMillis = System.currentTimeMillis() - startTimeMillis;
                updateMainTimeText();
                checkIntervalAnnounce();
                checkTargetAnnounce();

                // 1초마다 반복
                timerHandler.postDelayed(this, 1000);
            }
        };
    }

    private void initButtons() {
        btnStartPause.setOnClickListener(v -> {
            if (isRunning) {
                pauseStopwatch();
            } else {
                startStopwatch();
            }
        });

        btnReset.setOnClickListener(v -> resetStopwatch());

        btnOpenYoutube.setOnClickListener(v -> openYoutube());

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    // 스톱워치 시작 (또는 재시작)
    private void startStopwatch() {
        if (!isRunning) {
            // 이미 일부 경과한 상태에서 다시 시작할 수 있으므로, 이전 elapsed 고려
            startTimeMillis = System.currentTimeMillis() - elapsedMillis;
            timerHandler.post(timerRunnable);
            isRunning = true;
            btnStartPause.setText("Pause");
        }
    }

    // 일시정지
    private void pauseStopwatch() {
        if (isRunning) {
            timerHandler.removeCallbacks(timerRunnable);
            isRunning = false;
            btnStartPause.setText("Start");
        }
    }

    // 리셋
    private void resetStopwatch() {
        pauseStopwatch();
        elapsedMillis = 0L;
        nextIntervalMillis = intervalMinutes * 60 * 1000L;
        targetAnnounced = false;
        updateMainTimeText();
    }

    // 화면에 보이는 시간 업데이트 (경과 시간만 표시)
    private void updateMainTimeText() {
        tvMainTime.setText(formatTime(elapsedMillis));
    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    /**
     * 알림 간격마다 "n분 지났습니다" 음성 안내
     */
    private void checkIntervalAnnounce() {
        // 알림 간격이 0이거나 음성 비활성화면 스킵
        if (!voiceOn || intervalMinutes <= 0) return;

        while (elapsedMillis >= nextIntervalMillis) {
            long minutes = nextIntervalMillis / 1000 / 60;
            speak(minutes + "분 지났습니다.");
            nextIntervalMillis += intervalMinutes * 60 * 1000L;
        }
    }

    /**
     * 목표 샤워 시간 도달 시 한 번만 안내
     */
    private void checkTargetAnnounce() {
        if (!voiceOn || targetAnnounced || totalMinutes <= 0) return;

        long targetMillis = totalMinutes * 60 * 1000L;
        if (elapsedMillis >= targetMillis) {
            speak("목표 샤워 시간 " + totalMinutes + "분이 지났습니다. 샤워를 마무리해 주세요.");
            targetAnnounced = true;
        }
    }

    private void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "SHOWER_STOPWATCH_TTS");
        }
    }

    private void openYoutube() {
        // youtubeSource가 URL이면 그대로 열고, 아니면 검색어로 처리
        if (youtubeSource != null && youtubeSource.startsWith("http")) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(youtubeSource));
            startActivity(intent);
            return;
        }

        // 유튜브 앱 패키지 먼저 시도
        Intent appIntent = getPackageManager()
                .getLaunchIntentForPackage("com.google.android.youtube");

        if (appIntent != null) {
            startActivity(appIntent);
        } else {
            // 앱이 없으면 웹 검색
            String query = (youtubeSource == null || youtubeSource.isEmpty())
                    ? "shower music playlist"
                    : youtubeSource;
            Intent webIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/results?search_query=" + query));
            startActivity(webIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pauseStopwatch();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}