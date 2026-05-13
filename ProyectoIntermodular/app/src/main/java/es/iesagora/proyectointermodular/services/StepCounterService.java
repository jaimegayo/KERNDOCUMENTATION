package es.iesagora.proyectointermodular.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.MutableLiveData;

public class StepCounterService extends Service implements SensorEventListener {

    public static final String CHANNEL_ID = "StepCounterChannel";
    // LiveData estático para que el fragmento/ViewModel pueda observarlo fácilmente
    public static MutableLiveData<Integer> stepsLiveData = new MutableLiveData<>(0);

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private int stepsAtStart = -1; // Offset para que el entrenamiento empiece en 0

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Entrenamiento activo")
                .setContentText("Registrando tus pasos durante la sesión...")
                .setSmallIcon(android.R.drawable.ic_menu_directions)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(2, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH);
        } else {
            startForeground(2, notification);
        }

        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int totalStepsSinceReboot = (int) event.values[0];

            // LÓGICA DE OFFSET: Recuperamos el punto de inicio para este usuario
            if (stepsAtStart == -1) {
                SharedPreferences authPrefs = getSharedPreferences("prefs", MODE_PRIVATE);
                int userId = authPrefs.getInt("userId", -1);
                String userKey = "initial_steps_user_" + userId;

                SharedPreferences workoutPrefs = getSharedPreferences("workout_prefs", MODE_PRIVATE);
                stepsAtStart = workoutPrefs.getInt(userKey, -1);

                // Si es una sesión nueva, capturamos el valor actual del sensor como base
                if (stepsAtStart == -1) {
                    stepsAtStart = totalStepsSinceReboot;
                    workoutPrefs.edit().putInt(userKey, stepsAtStart).apply();
                }
            }

            // Los pasos de la sesión son la diferencia entre el total y el inicio
            int sessionSteps = Math.max(0, totalStepsSinceReboot - stepsAtStart);
            stepsLiveData.postValue(sessionSteps);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No se requiere implementación para este caso
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        // Limpiamos los pasos al destruir el servicio
        stepsLiveData.postValue(0);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Canal de Conteo de Pasos",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
