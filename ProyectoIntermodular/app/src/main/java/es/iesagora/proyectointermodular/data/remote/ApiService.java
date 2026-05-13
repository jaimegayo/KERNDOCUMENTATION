package es.iesagora.proyectointermodular.data.remote;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;
import es.iesagora.proyectointermodular.data.model.LoginResponse;
import es.iesagora.proyectointermodular.data.model.RoutineResponse;
import es.iesagora.proyectointermodular.data.model.User;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {

    @POST("login")
    Call<LoginResponse> login(@Body Map<String, String> body);

    @POST("register")
    Call<LoginResponse> register(@Body Map<String, String> body);

    @POST("users/complete-quiz")
    Call<User> completeQuiz(
            @Header("Authorization") String token,
            @Body QuizRequest body
    );

    @POST("routines/create")
    Call<ResponseBody> createRoutine(@Header("Authorization") String token, @Body RoutineCreateRequest routine);

    @GET("users/me")
    Call<User> getUserProfile(@Header("Authorization") String token);

    @GET("users/my-routines")
    Call<List<RoutineResponse>> getMyRoutines(@Header("Authorization") String token);

    // NUEVO: Para obtener el detalle de una rutina específica por su ID
    @GET("routines/{id}")
    Call<RoutineResponse> getRoutineDetail(
            @Path("id") int id,
            @Header("Authorization") String token
    );

    @PUT("routines/{routine_id}")
    Call<ResponseBody> updateRoutine(
            @Path("routine_id") int routineId,
            @Header("Authorization") String token,
            @Body RoutineCreateRequest request
    );

    // NUEVO: Para eliminar una rutina
    @DELETE("routines/{routine_id}")
    Call<ResponseBody> deleteRoutine(
            @Path("routine_id") int id,
            @Header("Authorization") String token
    );

    // NUEVO: Para actualizar el avatar del usuario
    @PUT("users/avatar")
    Call<User> updateAvatar(
            @Header("Authorization") String token,
            @Body AvatarUpdateRequest request
    );

    // NUEVO: Para actualizar el nombre de usuario
    @PUT("users/update_name")
    Call<LoginResponse> updateUsername(
            @Header("Authorization") String token,
            @Body UsernameUpdateRequest request
    );

    // NUEVO: Para obtener estadísticas globales del usuario
    @GET("users/stats")
    Call<UserStats> getUserStats(@Header("Authorization") String token);

    // NUEVO: Para obtener el historial de entrenamientos completo
    @GET("users/my-history")
    Call<List<WorkoutHistoryResponse>> getWorkoutHistory(@Header("Authorization") String token);

    // NUEVO: Para finalizar entrenamiento y guardar historial
    @POST("workouts/finish")
    Call<ResponseBody> finishWorkout(
            @Header("Authorization") String token,
            @Body WorkoutFinishRequest body
    );

    public class UserStats {
        @SerializedName("total_workouts")
        private int totalWorkouts;
        @SerializedName("total_steps")
        private int totalSteps;
        @SerializedName("training_days")
        private List<String> trainingDays;

        public int getTotalWorkouts() { return totalWorkouts; }
        public int getTotalSteps() { return totalSteps; }
        public List<String> getTrainingDays() { return trainingDays; }
    }

    public class RoutineCreateRequest {
        @SerializedName("user_id")
        private int userId;
        @SerializedName("nombre")
        private String nombre;
        @SerializedName("ejercicios")
        private List<ExerciseRequest> ejercicios;

        public RoutineCreateRequest(int userId, String nombre, List<ExerciseRequest> ejercicios) {
            this.userId = userId;
            this.nombre = nombre;
            this.ejercicios = ejercicios;
        }

        public static class ExerciseRequest {
            private String nombre;
            private List<SerieRequest> series;

            public ExerciseRequest(String nombre, List<SerieRequest> series) {
                this.nombre = nombre;
                this.series = series;
            }
        }

        public static class SerieRequest {
            private int numSerie;
            private double kilos;
            private int reps;

            public SerieRequest(int numSerie, double kilos, int reps) {
                this.numSerie = numSerie;
                this.kilos = kilos;
                this.reps = reps;
            }
        }
    }

    // Clase para la petición de fin de entrenamiento (fuera de RoutineCreateRequest para claridad)
    public class WorkoutFinishRequest {
        @SerializedName("routine_name")
        private String routineName;
        @SerializedName("duration_seconds")
        private int durationSeconds;
        @SerializedName("total_volume")
        private double totalVolume;
        @SerializedName("steps")
        private int steps;
        @SerializedName("data_json")
        private List<ExerciseFinishData> dataJson;

        public WorkoutFinishRequest(String routineName, int durationSeconds, double totalVolume, int steps, List<ExerciseFinishData> dataJson) {
            this.routineName = routineName;
            this.durationSeconds = durationSeconds;
            this.totalVolume = totalVolume;
            this.steps = steps;
            this.dataJson = dataJson;
        }

        public static class ExerciseFinishData {
            private String name;
            private List<RoutineResponse.SerieResponse> series;

            public ExerciseFinishData(String name, List<RoutineResponse.SerieResponse> series) {
                this.name = name;
                this.series = series;
            }
        }
    }

    class QuizRequest {
        @SerializedName("assigned_routine")
        private String assigned_routine;

        public QuizRequest(String assigned_routine) {
            this.assigned_routine = assigned_routine;
        }

        public String getAssigned_routine() {
            return assigned_routine;
        }

        public void setAssigned_routine(String assigned_routine) {
            this.assigned_routine = assigned_routine;
        }
    }

    class AvatarUpdateRequest {
        @SerializedName("avatar_url")
        private String avatarUrl;

        public AvatarUpdateRequest(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }
    }

    class UsernameUpdateRequest {
        @SerializedName("username")
        private String username;

        public UsernameUpdateRequest(String username) {
            this.username = username;
        }

        public String getUsername() {
            return username;
        }
    }

    class WorkoutHistoryResponse {
        @SerializedName("id")
        private int id;
        @SerializedName("user_id")
        private int userId;
        @SerializedName("routine_name")
        private String routineName;
        @SerializedName("duration_seconds")
        private int durationSeconds;
        @SerializedName("total_volume")
        private double totalVolume;
        @SerializedName("steps")
        private int steps;
        @SerializedName("created_at")
        private String createdAt;
        @SerializedName("data_json")
        private java.util.List<RoutineResponse.ExerciseResponse> dataJson;

        public int getId() { return id; }
        public int getUserId() { return userId; }
        public String getRoutineName() { return routineName; }
        public int getDurationSeconds() { return durationSeconds; }
        public double getTotalVolume() { return totalVolume; }
        public int getSteps() { return steps; }
        public String getCreatedAt() { return createdAt; }
        public java.util.List<RoutineResponse.ExerciseResponse> getDataJson() { return dataJson; }
    }
}