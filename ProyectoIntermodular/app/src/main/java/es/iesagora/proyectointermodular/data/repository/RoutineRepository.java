package es.iesagora.proyectointermodular.data.repository;

import java.util.List;

import es.iesagora.proyectointermodular.data.model.RoutineResponse;
import es.iesagora.proyectointermodular.data.remote.ApiService;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RoutineRepository {
    private final ApiService apiService;
    private static final String BASE_URL = "https://kern-blue.vercel.app/";
    //para pruebas locales:
    //private static final String BASE_URL = "http://10.0.2.2:8002/";

    public interface RoutineCallback {
        void onSuccess(String message);
        void onError(String message);
    }

    public RoutineRepository() {
        this.apiService = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService.class);
    }

    // Hemos añadido el String token como primer parámetro
    public void saveRoutine(String token, ApiService.RoutineCreateRequest request, RoutineCallback callback) {

        // Creamos la cabecera con el formato que espera FastAPI (Bearer + espacio + token)
        String authHeader = "Bearer " + token;

        // Pasamos el authHeader al metodo createRoutine
        apiService.createRoutine(authHeader, request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess("Rutina guardada correctamente");
                } else {
                    // Si recibes un 401 aquí, es que el token no es válido o está vacío
                    callback.onError("Error al guardar: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onError("Fallo de red: " + t.getMessage());
            }
        });
    }

    // NUEVO METODO PARA ACTUALIZAR (PUT)
    public void updateRoutine(int routineId, String token, ApiService.RoutineCreateRequest request, RoutineCallback callback) {
        String authHeader = "Bearer " + token;

        apiService.updateRoutine(routineId, authHeader, request).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess("Rutina actualizada correctamente");
                } else {
                    callback.onError("Error al actualizar: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onError("Fallo de red: " + t.getMessage());
            }
        });
    }

    public void getMyRoutines(String token, RoutinesListCallback callback) {
        // Aseguramos el formato Bearer para el token si el ApiService lo requiere
        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

        apiService.getMyRoutines(authHeader).enqueue(new retrofit2.Callback<List<RoutineResponse>>() {
            @Override
            public void onResponse(Call<List<RoutineResponse>> call, retrofit2.Response<List<RoutineResponse>> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Error al obtener rutinas");
                }
            }

            @Override
            public void onFailure(Call<List<RoutineResponse>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    // METODO para obtener el detalle de una rutina específica
    public void getRoutineDetail(int routineId, String token, RoutineDetailCallback callback) {
        String authHeader = "Bearer " + token;
        apiService.getRoutineDetail(routineId, authHeader).enqueue(new Callback<RoutineResponse>() {
            @Override
            public void onResponse(Call<RoutineResponse> call, Response<RoutineResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Error al obtener el detalle: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<RoutineResponse> call, Throwable t) {
                callback.onError("Fallo de red: " + t.getMessage());
            }
        });
    }

    // NUEVO METODO PARA ELIMINAR (DELETE)
    public void deleteRoutine(int routineId, String token, RoutineCallback callback) {
        String authHeader = "Bearer " + token;

        apiService.deleteRoutine(routineId, authHeader).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess("Rutina eliminada correctamente");
                } else {
                    callback.onError("Error al eliminar: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                callback.onError("Fallo de red: " + t.getMessage());
            }
        });
    }

    public interface RoutinesListCallback {
        void onSuccess(List<RoutineResponse> routines);
        void onError(String message);
    }

    // Interfaz para el detalle de una sola rutina
    public interface RoutineDetailCallback {
        void onSuccess(RoutineResponse routine);
        void onError(String message);
    }
}