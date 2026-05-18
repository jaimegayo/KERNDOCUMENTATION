package es.iesagora.proyectointermodular.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.media.AudioManager;
import android.media.ToneGenerator;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import es.iesagora.proyectointermodular.R;
import es.iesagora.proyectointermodular.data.model.RoutineResponse;
import es.iesagora.proyectointermodular.data.remote.ApiService;
import es.iesagora.proyectointermodular.data.repository.TokenManager;
import es.iesagora.proyectointermodular.data.repository.WorkoutRepository;
import es.iesagora.proyectointermodular.services.StepCounterService;
import es.iesagora.proyectointermodular.ui.adapter.ActiveExerciseAdapter;
import es.iesagora.proyectointermodular.viewmodel.RoutineViewModel;

public class WorkoutActiveFragment extends Fragment {

    private long startTime = 0L;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private TextView tvSessionTimer;

    // Vistas para los contadores y la lista
    private TextView tvTotalVolume, tvTotalSets;
    private RecyclerView rvActiveExercises;
    private ActiveExerciseAdapter adapter;
    private RoutineResponse routine;
    private Button btnFinishWorkout, btnCancelWorkout;
    private ImageButton btnRestTimer;

    // ViewModel para persistencia del temporizador
    private RoutineViewModel routineViewModel;

    // NUEVO: Instancia del repositorio siguiendo tu patrón MVVM
    private WorkoutRepository workoutRepository;

    // Variable para almacenar el volumen actual calculado
    private double currentTotalVolume = 0.0;

    // Vistas para el contador de pasos
    private View btnStepCounter;
    private TextView tvStepCount;
    private static final int PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 100;

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            int hours = minutes / 60;
            seconds = seconds % 60;
            minutes = minutes % 60;

            String timeText = String.format(Locale.getDefault(), "%dh %02dm %02ds", hours, minutes, seconds);
            if (tvSessionTimer != null) {
                tvSessionTimer.setText(timeText);
            }

            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflamos el layout
        return inflater.inflate(R.layout.fragment_workout_active, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializamos el repositorio
        workoutRepository = new WorkoutRepository();

        // Inicializamos las vistas
        tvSessionTimer = view.findViewById(R.id.tvSessionTimer);
        tvTotalVolume = view.findViewById(R.id.tvTotalVolume);
        tvTotalSets = view.findViewById(R.id.tvTotalSets);
        rvActiveExercises = view.findViewById(R.id.rvActiveExercises);
        btnFinishWorkout = view.findViewById(R.id.btnFinishWorkout);
        btnCancelWorkout = view.findViewById(R.id.btnCancelWorkout);
        btnRestTimer = view.findViewById(R.id.btnRestTimer);
        btnStepCounter = view.findViewById(R.id.btnStepCounter);
        tvStepCount = view.findViewById(R.id.tvStepCount);

        // Inicializamos el ViewModel compartido
        routineViewModel = new ViewModelProvider(requireActivity()).get(RoutineViewModel.class);

        // Verificamos permisos de actividad física antes de iniciar el servicio
        checkActivityRecognitionPermission();

        // Lógica del botón de pasos (manual)
        if (btnStepCounter != null) {
            btnStepCounter.setOnClickListener(v -> {
                Integer steps = StepCounterService.stepsLiveData.getValue();
                if (steps != null) {
                    tvStepCount.setText(String.valueOf(steps));
                    routineViewModel.setSteps(steps);
                }
            });
        }

        // Observamos el contador de pasos en tiempo real
        StepCounterService.stepsLiveData.observe(getViewLifecycleOwner(), steps -> {
            if (tvStepCount != null) {
                tvStepCount.setText(String.valueOf(steps));
                routineViewModel.setSteps(steps);
            }
        });

        // Recuperamos la rutina pasada por argumentos
        if (getArguments() != null) {
            routine = (RoutineResponse) getArguments().getSerializable("routine");
        }

        // Lógica de Persistencia: Recuperamos o Iniciamos el cronómetro y la rutina
        if (routineViewModel.isSessionActive(requireContext())) {
            startTime = routineViewModel.getSavedStartTime(requireContext());
            // Si la app se cerró por completo, 'routine' vendrá nulo de los argumentos
            if (routine == null) {
                routine = routineViewModel.getSavedRoutine(requireContext());
            }
            Log.d("WORKOUT_RESTORE", "Sesión recuperada. Inicio: " + startTime);
        } else {
            startTime = System.currentTimeMillis();
            // Guardamos el estado inicial para que el servicio tenga el offset de pasos
            if (routine != null) {
                routineViewModel.saveSessionState(requireContext(), startTime, -1, routine);
            }
            Log.d("WORKOUT_START", "Nueva sesión iniciada: " + startTime);
        }

        // Configuración del RecyclerView y el Adaptador dinámico
        if (routine != null && routine.getExercises() != null) {
            rvActiveExercises.setLayoutManager(new LinearLayoutManager(getContext()));

            adapter = new ActiveExerciseAdapter(routine.getExercises(), new ActiveExerciseAdapter.OnVolumenChangeListener() {
                @Override
                public void onVolumenChanged(double totalVolume, int totalSets) {
                    // Guardamos el volumen en la variable local para el envío final
                    currentTotalVolume = totalVolume;
                    // Actualizamos los TextViews de la cabecera en tiempo real
                    tvTotalVolume.setText(String.format(Locale.getDefault(), "%.1f kg", totalVolume));
                    tvTotalSets.setText(String.valueOf(totalSets));

                    // PERSISTENCIA: Cada vez que hay un cambio, guardamos el estado de la rutina (series hechas, pesos, etc.)
                    if (routine != null) {
                        routineViewModel.saveSessionState(requireContext(), startTime, -1, routine);
                    }
                }
            });

            rvActiveExercises.setAdapter(adapter);

            // Recalcular volumen total y series completadas al restaurar/cargar la interfaz
            adapter.calcularVolumenTotal();

            // Implementación de deslizar para eliminar ejercicio (Fase 3)
            ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    int position = viewHolder.getAdapterPosition();
                    routine.getExercises().remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.calcularVolumenTotal(); // Recalcular al eliminar
                    Toast.makeText(getContext(), "Ejercicio eliminado de la sesión", Toast.LENGTH_SHORT).show();
                }
            };
            new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(rvActiveExercises);
        }

        // Lógica del botón Finalizar Entrenamiento
        if (btnFinishWorkout != null) {
            btnFinishWorkout.setOnClickListener(v -> finishWorkoutSession());
        }

        if (btnRestTimer != null) {
            btnRestTimer.setOnClickListener(v -> showRestTimerDialog());
        }

        if (btnCancelWorkout != null) {
            btnCancelWorkout.setOnClickListener(v -> showCancelConfirmationDialog());
        }

        // Observar cuando el timer termina para sonar el beep
        routineViewModel.getTimerFinished().observe(getViewLifecycleOwner(), finished -> {
            if (Boolean.TRUE.equals(finished)) {
                playTimerBeep();
                routineViewModel.resetTimerFinished();
            }
        });

        timerHandler.postDelayed(timerRunnable, 0);
    }

    /**
     * Recopila los datos de la sesión, filtra las series hechas y las envía a FastAPI
     * usando el WorkoutRepository
     */
    private void finishWorkoutSession() {
        if (routine == null) return;

        // 1. Calcular duración en segundos
        long millis = System.currentTimeMillis() - startTime;
        int totalSeconds = (int) (millis / 1000);

        // 2. Filtrar ejercicios y series completadas (isDone = true)
        List<ApiService.WorkoutFinishRequest.ExerciseFinishData> completedExercises = new ArrayList<>();

        for (RoutineResponse.ExerciseResponse ex : routine.getExercises()) {
            List<RoutineResponse.SerieResponse> doneSeries = new ArrayList<>();
            for (RoutineResponse.SerieResponse s : ex.getSeries()) {
                if (s.isDone()) {
                    doneSeries.add(s);
                }
            }
            if (!doneSeries.isEmpty()) {
                completedExercises.add(new ApiService.WorkoutFinishRequest.ExerciseFinishData(ex.getName(), doneSeries));
            }
        }

        // Validación: si no ha marcado nada, no guardamos
        if (completedExercises.isEmpty()) {
            Toast.makeText(getContext(), "Marca al menos una serie como completada antes de finalizar", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Preparar el objeto de petición para el historial (incluyendo pasos)
        int steps = routineViewModel.getStepsSession().getValue() != null ? routineViewModel.getStepsSession().getValue() : 0;
        ApiService.WorkoutFinishRequest request = new ApiService.WorkoutFinishRequest(
                routine.getName(),
                totalSeconds,
                currentTotalVolume,
                steps,
                completedExercises
        );

        // 4. Recuperar el Token de la "mochila" usando tu TokenManager
        String token = TokenManager.getToken(requireContext());
        if (token == null) {
            Toast.makeText(getContext(), "Error de sesión: No se encontró el token", Toast.LENGTH_SHORT).show();
            return;
        }

        // 5. Enviar a la API a través del repositorio (siguiendo tu arquitectura)
        workoutRepository.finishWorkout(token, request, new WorkoutRepository.WorkoutCallback() {
            @Override
            public void onSuccess(String message) {
                Toast.makeText(getContext(), "¡Entrenamiento guardado en tu historial!", Toast.LENGTH_LONG).show();

                // --- LIMPIEZA ATÓMICA TRAS ÉXITO ---
                // 1. Apagamos el "interruptor" local en SharedPreferences
                routineViewModel.clearSessionState(requireContext());

                // 2. Reseteamos contadores en el ViewModel (pasos, crono)
                routineViewModel.clearAllData();

                // 3. Detenemos el servicio de pasos definitivamente
                stopStepCounterService();

                if (getActivity() != null) {
                    // 4. Volvemos al Home o pantalla anterior
                    getActivity().onBackPressed();
                }
            }

            @Override
            public void onError(String message) {
                Log.e("API_ERROR", message);
                Toast.makeText(getContext(), "Error al guardar entrenamiento: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Muestra un diálogo de confirmación para abortar el entrenamiento sin guardar nada.
     */
    private void showCancelConfirmationDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("¿Cancelar entrenamiento?")
                .setMessage("Se perderá el progreso actual y no se guardará en el historial.")
                .setPositiveButton("Sí, cancelar", (dialog, which) -> {
                    // LIMPIEZA TOTAL Y LOCAL (Sin llamar a API)
                    routineViewModel.clearSessionState(requireContext());
                    routineViewModel.clearAllData();
                    stopStepCounterService();

                    // Navegamos de vuelta al Home
                    androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.homeFragment);
                    Toast.makeText(getContext(), "Entrenamiento cancelado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No, continuar", null)
                .show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // IMPORTANTE: Quitamos los callbacks para evitar fugas de memoria
        timerHandler.removeCallbacks(timerRunnable);
        // Detenemos el servicio de pasos al salir
        stopStepCounterService();
    }

    /**
     * Lógica de permisos para Actividad Física (Requerido en Android 10+)
     */
    private void checkActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                        PERMISSION_REQUEST_ACTIVITY_RECOGNITION);
            } else {
                startStepCounterService();
            }
        } else {
            startStepCounterService();
        }
    }

    private void startStepCounterService() {
        Intent intent = new Intent(getContext(), StepCounterService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent);
        } else {
            requireContext().startService(intent);
        }
    }

    private void stopStepCounterService() {
        Intent intent = new Intent(getContext(), StepCounterService.class);
        requireContext().stopService(intent);
    }

    /**
     * Muestra el diálogo del temporizador de descanso sincronizado con el ViewModel
     */
    private void showRestTimerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rest_timer, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvDisplay = dialogView.findViewById(R.id.tvTimerDisplay);
        ImageButton btnPlayPause = dialogView.findViewById(R.id.btnPlayPause);
        Button btnMinus = dialogView.findViewById(R.id.btnMinus15);
        Button btnPlus = dialogView.findViewById(R.id.btnPlus15);
        Button btnReset = dialogView.findViewById(R.id.btnResetTimer);

        // Observar tiempo restante
        routineViewModel.getTimeRemaining().observe(getViewLifecycleOwner(), millis -> {
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            tvDisplay.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
        });

        // Observar estado del timer (Play/Pause)
        routineViewModel.getIsTimerRunning().observe(getViewLifecycleOwner(), running -> {
            if (Boolean.TRUE.equals(running)) {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            }
        });

        btnPlayPause.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(routineViewModel.getIsTimerRunning().getValue())) {
                routineViewModel.pauseTimer();
            } else {
                routineViewModel.startTimer();
            }
        });

        btnMinus.setOnClickListener(v -> routineViewModel.addTime(-15000L));
        btnPlus.setOnClickListener(v -> routineViewModel.addTime(15000L));
        btnReset.setOnClickListener(v -> routineViewModel.resetTimer());

        dialog.show();
    }

    /**
     * Reproduce un sonido de aviso al finalizar el descanso
     */
    private void playTimerBeep() {
        try {
            ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            toneGen.startTone(ToneGenerator.TONE_CDMA_PIP, 500);
        } catch (Exception e) {
            Log.e("TIMER_SOUND", "Error al reproducir sonido", e);
        }
    }
}