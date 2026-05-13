package es.iesagora.proyectointermodular.viewmodel;

//VIEWMODEL COMPARTIDO
import android.os.CountDownTimer;
import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import es.iesagora.proyectointermodular.data.model.Exercise;
import es.iesagora.proyectointermodular.data.model.RoutineResponse;
import es.iesagora.proyectointermodular.data.model.Serie;
import es.iesagora.proyectointermodular.data.remote.ApiService;
import es.iesagora.proyectointermodular.data.repository.RoutineRepository;

import android.content.Context;
import android.content.SharedPreferences;

public class RoutineViewModel extends ViewModel {
    // Constantes para SharedPreferences
    private static final String PREFS_NAME = "workout_prefs";
    private static final String KEY_IS_ACTIVE = "is_active";
    private static final String KEY_START_TIME = "start_time";
    private static final String KEY_INITIAL_STEPS = "initial_steps";
    private static final String KEY_ROUTINE_JSON = "routine_json"; // Opcional por si queremos guardar la rutina entera
    //esta es la lista de ejercicios que el usuario va añadiendo a su nueva rutina
    private final MutableLiveData<List<Exercise>> selectedExercises = new MutableLiveData<>(new ArrayList<>());

    //nombre de la rutina
    private String routineName = "";

    // ID de la rutina que estamos editando (-1 significa que es nueva)
    private int editingRoutineId = -1;

    // Repositorio y estados de la petición
    private final RoutineRepository routineRepository = new RoutineRepository();
    private final es.iesagora.proyectointermodular.data.repository.WorkoutRepository workoutRepository = new es.iesagora.proyectointermodular.data.repository.WorkoutRepository();
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleteSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<List<es.iesagora.proyectointermodular.data.model.RoutineResponse>> myRoutines = new MutableLiveData<>();
    private final MutableLiveData<List<ApiService.WorkoutHistoryResponse>> workoutHistory = new MutableLiveData<>();

    // Variable para observar una sola rutina (el detalle)
    private final MutableLiveData<RoutineResponse> selectedRoutine = new MutableLiveData<>();

    // --- LOGICA DEL TEMPORIZADOR DE DESCANSO ---
    private CountDownTimer countDownTimer;
    private final MutableLiveData<Long> timeRemaining = new MutableLiveData<>(60000L); // 60 segundos por defecto
    private final MutableLiveData<Boolean> isTimerRunning = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> timerFinished = new MutableLiveData<>(false);

    public LiveData<Long> getTimeRemaining() { return timeRemaining; }
    public LiveData<Boolean> getIsTimerRunning() { return isTimerRunning; }
    public LiveData<Boolean> getTimerFinished() { return timerFinished; }

    public void startTimer() {
        if (Boolean.TRUE.equals(isTimerRunning.getValue())) return;

        countDownTimer = new CountDownTimer(timeRemaining.getValue() != null ? timeRemaining.getValue() : 60000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeRemaining.postValue(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                isTimerRunning.postValue(false);
                timerFinished.postValue(true);
                timeRemaining.postValue(0L);
            }
        }.start();
        isTimerRunning.setValue(true);
        timerFinished.setValue(false);
    }

    public void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning.setValue(false);
    }

    public void resetTimer() {
        pauseTimer();
        timeRemaining.setValue(60000L); // Volvemos al minuto por defecto
        timerFinished.setValue(false);
    }

    public void addTime(long ms) {
        long current = timeRemaining.getValue() != null ? timeRemaining.getValue() : 0;
        long newTime = Math.max(0, current + ms);
        timeRemaining.setValue(newTime);

        // Si el timer estaba corriendo, tenemos que reiniciarlo con el nuevo tiempo
        if (Boolean.TRUE.equals(isTimerRunning.getValue())) {
            pauseTimer();
            startTimer();
        }
    }

    public void resetTimerFinished() {
        timerFinished.setValue(false);
    }

    public LiveData<RoutineResponse> getSelectedRoutine() {
        return selectedRoutine;
    }

    public LiveData<List<es.iesagora.proyectointermodular.data.model.RoutineResponse>> getMyRoutines() {
        return myRoutines;
    }

    public LiveData<List<ApiService.WorkoutHistoryResponse>> getWorkoutHistory() {
        return workoutHistory;
    }

    public LiveData<List<Exercise>> getSelectedExercises(){
        return selectedExercises;
    }

    // Getters para que el Fragment pueda observar el estado del guardado
    public LiveData<Boolean> getSaveSuccess() {
        return saveSuccess;
    }

    public LiveData<Boolean> getDeleteSuccess() {
        return deleteSuccess;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // --- LOGICA DEL CONTADOR DE PASOS ---
    private final MutableLiveData<Integer> stepsSession = new MutableLiveData<>(0);
    public LiveData<Integer> getStepsSession() { return stepsSession; }

    public void setSteps(int steps) {
        stepsSession.setValue(steps);
    }

    // --- NUEVA LÓGICA DE PERSISTENCIA CON VINCULACIÓN DE USUARIO ---

    /**
     * Obtiene una clave única por usuario para evitar fugas de datos entre sesiones.
     */
    private String getPrefKey(Context context, String baseKey) {
        SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);
        return baseKey + "_user_" + userId;
    }

    public void saveSessionState(Context context, long startTime, int stepsAtStart, RoutineResponse routine) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String routineJson = new Gson().toJson(routine);

        SharedPreferences.Editor editor = prefs.edit()
                .putBoolean(getPrefKey(context, KEY_IS_ACTIVE), true)
                .putLong(getPrefKey(context, KEY_START_TIME), startTime)
                .putString(getPrefKey(context, KEY_ROUTINE_JSON), routineJson);

        // Solo guardamos los pasos si nos pasan un valor válido
        if (stepsAtStart != -1) {
            editor.putInt(getPrefKey(context, KEY_INITIAL_STEPS), stepsAtStart);
        }

        editor.apply();
    }

    public RoutineResponse getSavedRoutine(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(getPrefKey(context, KEY_ROUTINE_JSON), null);
        if (json != null) {
            return new Gson().fromJson(json, RoutineResponse.class);
        }
        return null;
    }

    public boolean isSessionActive(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(getPrefKey(context, KEY_IS_ACTIVE), false);
    }

    public long getSavedStartTime(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(getPrefKey(context, KEY_START_TIME), 0L);
    }

    public int getSavedInitialSteps(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(getPrefKey(context, KEY_INITIAL_STEPS), -1);
    }

    public void clearSessionState(Context context) {
        // Al cerrar la sesión del entrenamiento, borramos solo los datos del usuario actual
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Obtenemos el ID antes de borrar nada, para saber qué claves quitar
        SharedPreferences authPrefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int userId = authPrefs.getInt("userId", -1);

        if (userId != -1) {
            String suffix = "_user_" + userId;
            prefs.edit()
                    .remove(KEY_IS_ACTIVE + suffix)
                    .remove(KEY_START_TIME + suffix)
                    .remove(KEY_ROUTINE_JSON + suffix)
                    .remove(KEY_INITIAL_STEPS + suffix)
                    .apply();
        }
    }

    // METODO para guardar la rutina completa en el servidor
    // Hemos añadido el String token para poder autorizar la petición
    public void saveAllRoutine(int userId, String token) {
        if (routineName.isEmpty() || selectedExercises.getValue() == null || selectedExercises.getValue().isEmpty()) {
            errorMessage.setValue("Nombre o ejercicios vacíos");
            return;
        }

        // Mapeamos de nuestros modelos de UI a los de la petición (Request)
        List<ApiService.RoutineCreateRequest.ExerciseRequest> ejerciciosDTO = new ArrayList<>();

        if (selectedExercises.getValue() != null) {
            for (Exercise ex : selectedExercises.getValue()) {
                List<ApiService.RoutineCreateRequest.SerieRequest> seriesDTO = new ArrayList<>();
                if (ex.getSeries() != null) {
                    for (Serie s : ex.getSeries()) {
                        seriesDTO.add(new ApiService.RoutineCreateRequest.SerieRequest(s.getNumSerie(), s.getKilos(), s.getReps()));
                    }
                }
                ejerciciosDTO.add(new ApiService.RoutineCreateRequest.ExerciseRequest(ex.getName(), seriesDTO));
            }
        }

        ApiService.RoutineCreateRequest request = new ApiService.RoutineCreateRequest(userId, routineName, ejerciciosDTO);

        // Decidimos si llamar a UPDATE o a SAVE basándonos en editingRoutineId
        if (editingRoutineId == -1) {
            // MODO CREACIÓN
            routineRepository.saveRoutine(token, request, new RoutineRepository.RoutineCallback() {
                @Override
                public void onSuccess(String message) {
                    saveSuccess.postValue(true);
                }

                @Override
                public void onError(String message) {
                    errorMessage.postValue(message);
                }
            });
        } else {
            // MODO EDICIÓN (PUT)
            routineRepository.updateRoutine(editingRoutineId, token, request, new RoutineRepository.RoutineCallback() {
                @Override
                public void onSuccess(String message) {
                    saveSuccess.postValue(true);
                }

                @Override
                public void onError(String message) {
                    errorMessage.postValue(message);
                }
            });
        }
    }

    // NUEVO METODO PARA ELIMINAR
    public void deleteRoutine(int routineId, String token) {
        routineRepository.deleteRoutine(routineId, token, new RoutineRepository.RoutineCallback() {
            @Override
            public void onSuccess(String message) {
                deleteSuccess.postValue(true);
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue(message);
            }
        });
    }

    public void clearAllData() {
        // Vaciamos los LiveData para que la UI se refresque y no muestre nada viejo
        if (myRoutines != null) {
            myRoutines.setValue(new ArrayList<>());
        }
        if (selectedExercises != null) {
            selectedExercises.setValue(new ArrayList<>());
        }
        // Limpiamos cronómetro y pasos para el siguiente entrenamiento
        stepsSession.setValue(0);
        timeRemaining.setValue(60000L);
        isTimerRunning.setValue(false);
        timerFinished.setValue(false);

        routineName = "";
        editingRoutineId = -1;
    }

    // METODO PARA RESETEAR el semáforo de para que podamos seguir creando rutinas
    public void ressetSaveStatus() {
        saveSuccess.setValue(false);
        deleteSuccess.setValue(false);
        errorMessage.setValue(null);
        //también vaciaremos la lista para la siguiente rutina
        selectedExercises.setValue(new ArrayList<>());
        routineName = "";
        editingRoutineId = -1;
    }

    public void resetDeleteStatus() {
        deleteSuccess.setValue(false);
    }

    // METODO para añadir un ejercicio desde el buscador
    public void addExercise(Exercise exercise){
        List<Exercise> currentList = selectedExercises.getValue();
        if (currentList != null) {
            // creamos una nueva instancia de la lista (copia)
            List<Exercise> newList = new ArrayList<>(currentList);

            // al añadirlo le vamos a crear la primera serie por defecto si no tiene
            if (exercise.getSeries() == null || exercise.getSeries().isEmpty()) {
                List<Serie> initialSeries = new ArrayList<>();
                initialSeries.add(new Serie(1, 0.0, 0));
                exercise.setSeries(initialSeries);
            }

            // añadimos el ejercicio a la nueva lista
            newList.add(exercise);
            selectedExercises.postValue(newList); // uso postValue para mayor seguridad
        }
    }

    // METODO para eliminar un ejercicio de la lista
    public void removeExercise(Exercise exercise){
        List<Exercise> currentList = selectedExercises.getValue();
        if (currentList != null){
            List<Exercise> newList = new ArrayList<>(currentList);
            newList.remove(exercise);
            selectedExercises.setValue(newList);
        }
    }

    public void setRoutineName(String name) {
        this.routineName = name;
    }

    public String getRoutineName(){
        return routineName;
    }

    // Getters y Setters para el modo edición
    public int getEditingRoutineId() {
        return editingRoutineId;
    }

    public void setEditingRoutineId(int editingRoutineId) {
        this.editingRoutineId = editingRoutineId;
    }

    // Metodo para inyectar ejercicios directamente (útil al empezar a editar)
    public void setSelectedExercises(List<Exercise> exercises) {
        this.selectedExercises.setValue(exercises);
    }

    // Metodo para limpiar los datos después de guardar
    public void clearRoutineData() {
        selectedExercises.setValue(new ArrayList<>());
        routineName = "";
        saveSuccess.setValue(false);
        errorMessage.setValue(null);
        editingRoutineId = -1;
    }

    // Nuevo METODO para traer las rutinas del servidor
    public void fetchMyRoutines(String token) {
        // Aquí llamaríamos al repositorio.
        routineRepository.getMyRoutines(token, new RoutineRepository.RoutinesListCallback() {
            @Override
            public void onSuccess(List<es.iesagora.proyectointermodular.data.model.RoutineResponse> routines) {
                myRoutines.postValue(routines);
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue(message);
            }
        });
    }

    // METODO para traer el detalle de una sola rutina por ID
    public void fetchRoutineById(int routineId, String token) {
        // Usamos el repositorio para traer la información detallada
        routineRepository.getRoutineDetail(routineId, token, new RoutineRepository.RoutineDetailCallback() {
            @Override
            public void onSuccess(RoutineResponse routine) {
                selectedRoutine.postValue(routine);
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue(message);
            }
        });
    }

    // Nuevo METODO para traer el historial completo y usarlo en el Gráfico de Volumen y Carrusel
    public void fetchWorkoutHistory(String token) {
        workoutRepository.getWorkoutHistory(token, new es.iesagora.proyectointermodular.data.repository.WorkoutRepository.HistoryCallback() {
            @Override
            public void onSuccess(List<ApiService.WorkoutHistoryResponse> historyList) {
                workoutHistory.postValue(historyList);
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue(message);
            }
        });
    }
}