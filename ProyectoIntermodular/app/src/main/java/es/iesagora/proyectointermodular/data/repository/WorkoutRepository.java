package es.iesagora.proyectointermodular.data.repository;

import es.iesagora.proyectointermodular.data.remote.ApiService;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WorkoutRepository {
    private final ApiService apiService;
    private static final String BASE_URL = "https://kern-blue.vercel.app/";

    public interface WorkoutCallback {
        void onSuccess(String message);
        void onError(String message);
    }

    public interface HistoryCallback {
        void onSuccess(java.util.List<ApiService.WorkoutHistoryResponse> historyList);
        void onError(String message);
    }

    public WorkoutRepository() {
        this.apiService = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService.class);
    }

    public void finishWorkout(String token, ApiService.WorkoutFinishRequest request, WorkoutCallback callback) {
        String authHeader = "Bearer " + token;

        apiService.finishWorkout(authHeader, request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess("Entrenamiento guardado correctamente");
                } else {
                    callback.onError("Error al guardar: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onError("Fallo de red: " + t.getMessage());
            }
        });
    }

    public void getWorkoutHistory(String token, HistoryCallback callback) {
        String authHeader = "Bearer " + token;

        apiService.getWorkoutHistory(authHeader).enqueue(new Callback<java.util.List<ApiService.WorkoutHistoryResponse>>() {
            @Override
            public void onResponse(Call<java.util.List<ApiService.WorkoutHistoryResponse>> call, Response<java.util.List<ApiService.WorkoutHistoryResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    if (response.code() != 401) {
                        callback.onError("Error al obtener historial: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<java.util.List<ApiService.WorkoutHistoryResponse>> call, Throwable t) {
                callback.onError("Fallo de red: " + t.getMessage());
            }
        });
    }
}