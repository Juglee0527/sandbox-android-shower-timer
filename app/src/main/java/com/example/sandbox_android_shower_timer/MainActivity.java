package com.example.sandbox_android_shower_timer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvMainTime, tvElapsedTime, tvRemainingTime;
    private Button btnStartPause, btnReset, btnOpenYoutube;

    private CountDownTimer countDownTimer;
    private boolean isRunning = false;

    // 기본: 총 10분, 3분마다 알림
    private long totalMillis = 10 * 60 * 1000L;
    private long remainingMillis = totalMillis;
    private long elapsedMillis = 0L;

    private long notifyIntervalMillis = 3 * 60 * 1000L;
    private long nextAnnounceMillis = notifyIntervalMillis;

    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initTts();
        initButtons();
        updateTimeTexts();
    }

    private void initViews() {
        tvMainTime = findViewById(R.id.tvMainTime);
        tvElapsedTime = findViewById(R.id.tvElapsedTime);
        tvRemainingTime = findViewById(R.id.tvRemainingTime);
        btnStartPause = findViewById(R.id.btnStartPause);
        btnReset = findViewById(R.id.btnReset);
        btnOpenYoutube = findViewById(R.id.btnOpenYoutube);
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

    private void initButtons() {
        btnStartPause.setOnClickListener(v -> {
            if (isRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        btnReset.setOnClickListener(v -> resetTimer());

        btnOpenYoutube.setOnClickListener(v -> openYoutube());
    }

    private void startTimer() {
        if (remainingMillis <= 0) {
            remainingMillis = totalMillis;
            elapsedMillis = 0;
            nextAnnounceMillis = notifyIntervalMillis;
        }

        countDownTimer = new CountDownTimer(remainingMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMillis = millisUntilFinished;
                elapsedMillis = totalMillis - remainingMillis;

                updateTimeTexts();
                checkAnnounce();
            }

            @Override
            public void onFinish() {
                remainingMillis = 0;
                elapsedMillis = totalMillis;
                updateTimeTexts();
                speak("샤워 시간이 종료되었습니다.");
                isRunning = false;
                btnStartPause.setText("Start");
            }
        }.start();

        isRunning = true;
        btnStartPause.setText("Pause");
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isRunning = false;
        btnStartPause.setText("Start");
    }

    private void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        remainingMillis = totalMillis;
        elapsedMillis = 0;
        nextAnnounceMillis = notifyIntervalMillis;
        isRunning = false;
        btnStartPause.setText("Start");
        updateTimeTexts();
    }

    private void updateTimeTexts() {
        tvMainTime.setText(formatTime(remainingMillis));
        tvElapsedTime.setText(formatTime(elapsedMillis));
        tvRemainingTime.setText(formatTime(remainingMillis));
    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    /**
     * 3분, 6분, 9분 ... 경과했을 때 음성 안내
     */
    private void checkAnnounce() {
        if (elapsedMillis >= nextAnnounceMillis) {
            long minutes = elapsedMillis / 1000 / 60;
            speak(minutes + "분 지났습니다.");
            nextAnnounceMillis += notifyIntervalMillis;
        }
    }

    private void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, "SHOWER_TIMER_TTS");
        }
    }

    private void openYoutube() {
        // 1순위: 유튜브 앱 실행
        Intent intent = getPackageManager()
                .getLaunchIntentForPackage("com.google.android.youtube");

        if (intent != null) {
            startActivity(intent);
            return;
        }

        // 유튜브 앱이 없으면 브라우저로 열기 (샤워 노래 검색)
        Intent webIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/results?search_query=shower+music+playlist"));
        startActivity(webIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
