package es.iesagora.proyectointermodular.ui;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

import es.iesagora.proyectointermodular.R;
import es.iesagora.proyectointermodular.utils.HealthContent;

public class VitalityActivity extends AppCompatActivity {

    private ProgressBar pbTimer;
    private TextView tvTimerText;
    private MaterialButton btnFinishVitality;

    private TextView tvFuerzaEx, tvFuerzaDur;
    private ImageView ivFuerza;
    private TextView tvResistenciaEx, tvResistenciaDur;
    private ImageView ivResistencia;
    private TextView tvFlexibilidadEx, tvFlexibilidadDur;
    private ImageView ivFlexibilidad;
    private TextView tvAgilidadEx, tvAgilidadDur;
    private ImageView ivAgilidad;

    private View cardFuerza, cardResistencia, cardFlexibilidad, cardAgilidad;

    private CountDownTimer countDownTimer;
    private final long TOTAL_TIME_MS = 900000; // 15 minutos

    private Vibrator vibrator;
    private ArgbEvaluator colorEvaluator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vitality);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        colorEvaluator = new ArgbEvaluator();

        pbTimer = findViewById(R.id.pbTimer);
        tvTimerText = findViewById(R.id.tvTimerText);
        btnFinishVitality = findViewById(R.id.btnFinishVitality);

        cardFuerza = findViewById(R.id.cardFuerza);
        tvFuerzaEx = findViewById(R.id.tvFuerzaEx);
        tvFuerzaDur = findViewById(R.id.tvFuerzaDur);
        ivFuerza = findViewById(R.id.ivFuerza);

        cardResistencia = findViewById(R.id.cardResistencia);
        tvResistenciaEx = findViewById(R.id.tvResistenciaEx);
        tvResistenciaDur = findViewById(R.id.tvResistenciaDur);
        ivResistencia = findViewById(R.id.ivResistencia);

        cardFlexibilidad = findViewById(R.id.cardFlexibilidad);
        tvFlexibilidadEx = findViewById(R.id.tvFlexibilidadEx);
        tvFlexibilidadDur = findViewById(R.id.tvFlexibilidadDur);
        ivFlexibilidad = findViewById(R.id.ivFlexibilidad);

        cardAgilidad = findViewById(R.id.cardAgilidad);
        tvAgilidadEx = findViewById(R.id.tvAgilidadEx);
        tvAgilidadDur = findViewById(R.id.tvAgilidadDur);
        ivAgilidad = findViewById(R.id.ivAgilidad);

        setupCardInteractions();
        animateCardsIn();
        startRouletteEffect();
        startTimer();

        btnFinishVitality.setOnClickListener(v -> finishAndReturnToMain());
    }

    private void setupCardInteractions() {
        View[] cards = {cardFuerza, cardResistencia, cardFlexibilidad, cardAgilidad};
        for (View card : cards) {
            card.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                        break;
                }
                return true;
            });
        }
    }

    private void startRouletteEffect() {
        // Pre-seleccionar los ejercicios finales
        HealthContent.ExerciseNode finalFuerza = HealthContent.getRandomExercise(HealthContent.FUERZA);
        HealthContent.ExerciseNode finalResistencia = HealthContent.getRandomExercise(HealthContent.RESISTENCIA);
        HealthContent.ExerciseNode finalFlexibilidad = HealthContent.getRandomExercise(HealthContent.FLEXIBILIDAD);
        HealthContent.ExerciseNode finalAgilidad = HealthContent.getRandomExercise(HealthContent.AGILIDAD);

        Handler handler = new Handler(Looper.getMainLooper());
        int rouletteDuration = 1500;
        int interval = 100; // Cambiar cada 100ms

        // Definimos tiempos de parada escalonados para cada tarjeta
        int stopFuerza = rouletteDuration;
        int stopResistencia = rouletteDuration + 400;
        int stopFlexibilidad = rouletteDuration + 800;
        int stopAgilidad = rouletteDuration + 1200;

        Runnable rouletteRunnable = new Runnable() {
            int elapsedTime = 0;

            @Override
            public void run() {
                elapsedTime += interval;

                // Fuerza
                if (elapsedTime < stopFuerza) {
                    HealthContent.ExerciseNode temp = HealthContent.getRandomExercise(HealthContent.FUERZA);
                    tvFuerzaEx.setText(temp.name);
                    tvFuerzaDur.setText(temp.duration);
                    ivFuerza.setImageResource(temp.imageResId);
                } else if (elapsedTime == stopFuerza) {
                    setFinalCard(tvFuerzaEx, tvFuerzaDur, ivFuerza, finalFuerza);
                }

                // Resistencia
                if (elapsedTime < stopResistencia) {
                    HealthContent.ExerciseNode temp = HealthContent.getRandomExercise(HealthContent.RESISTENCIA);
                    tvResistenciaEx.setText(temp.name);
                    tvResistenciaDur.setText(temp.duration);
                    ivResistencia.setImageResource(temp.imageResId);
                } else if (elapsedTime == stopResistencia) {
                    setFinalCard(tvResistenciaEx, tvResistenciaDur, ivResistencia, finalResistencia);
                }

                // Flexibilidad
                if (elapsedTime < stopFlexibilidad) {
                    HealthContent.ExerciseNode temp = HealthContent.getRandomExercise(HealthContent.FLEXIBILIDAD);
                    tvFlexibilidadEx.setText(temp.name);
                    tvFlexibilidadDur.setText(temp.duration);
                    ivFlexibilidad.setImageResource(temp.imageResId);
                } else if (elapsedTime == stopFlexibilidad) {
                    setFinalCard(tvFlexibilidadEx, tvFlexibilidadDur, ivFlexibilidad, finalFlexibilidad);
                }

                // Agilidad
                if (elapsedTime < stopAgilidad) {
                    HealthContent.ExerciseNode temp = HealthContent.getRandomExercise(HealthContent.AGILIDAD);
                    tvAgilidadEx.setText(temp.name);
                    tvAgilidadDur.setText(temp.duration);
                    ivAgilidad.setImageResource(temp.imageResId);
                } else if (elapsedTime == stopAgilidad) {
                    setFinalCard(tvAgilidadEx, tvAgilidadDur, ivAgilidad, finalAgilidad);
                }

                if (elapsedTime <= stopAgilidad) {
                    handler.postDelayed(this, interval);
                }
            }
        };

        handler.post(rouletteRunnable);
    }

    private void setFinalCard(TextView tvEx, TextView tvDur, ImageView iv, HealthContent.ExerciseNode exercise) {
        tvEx.setText(exercise.name);
        tvDur.setText(exercise.duration);
        iv.setImageResource(exercise.imageResId);

        // Efecto visual flash al detenerse
        ObjectAnimator flash = ObjectAnimator.ofFloat(tvEx, "alpha", 0.5f, 1f);
        flash.setDuration(200);
        flash.start();

        triggerHapticPulse();
    }

    private void triggerHapticPulse() {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void animateCardsIn() {
        View[] cards = {cardFuerza, cardResistencia, cardFlexibilidad, cardAgilidad};
        for (int i = 0; i < cards.length; i++) {
            final View card = cards[i];

            // Animación de entrada
            card.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(150L * i)
                    .setDuration(400)
                    .setInterpolator(new DecelerateInterpolator())
                    .withEndAction(() -> {
                        // Animación de Shake una vez terminada la entrada
                        ObjectAnimator shake = ObjectAnimator.ofFloat(card, "translationX", 0f, 15f, -15f, 15f, -15f, 6f, -6f, 0f);
                        shake.setDuration(400);
                        shake.start();
                    })
                    .start();
        }
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(TOTAL_TIME_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                tvTimerText.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                pbTimer.setProgress((int) millisUntilFinished);

                // Interpolación de color: de Verde (#4CAF50) a Naranja (#FF9800) a Rojo (#F44336)
                // Usamos la fracción de tiempo restante
                float fraction = 1f - ((float) millisUntilFinished / TOTAL_TIME_MS);
                int color = (int) colorEvaluator.evaluate(fraction, Color.parseColor("#4CAF50"), Color.parseColor("#FF9800"));
                pbTimer.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
            }

            @Override
            public void onFinish() {
                tvTimerText.setText("00:00");
                pbTimer.setProgress(0);
                pbTimer.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336")));
                triggerMissionAccomplished();
            }
        }.start();
    }

    private void triggerMissionAccomplished() {
        btnFinishVitality.setText("¡Misión Cumplida!");
        btnFinishVitality.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFD700")));

        // Scale animation
        ObjectAnimator scaleAnim = ObjectAnimator.ofPropertyValuesHolder(
                btnFinishVitality,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.1f, 1f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.1f, 1f)
        );
        scaleAnim.setDuration(1000);
        scaleAnim.setRepeatCount(ObjectAnimator.INFINITE);
        scaleAnim.start();
    }

    private void finishAndReturnToMain() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
