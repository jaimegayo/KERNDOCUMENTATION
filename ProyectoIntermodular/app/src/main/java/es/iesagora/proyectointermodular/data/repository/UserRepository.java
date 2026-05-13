package es.iesagora.proyectointermodular.data.repository;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import es.iesagora.proyectointermodular.data.model.LoginResponse;
import es.iesagora.proyectointermodular.data.model.User;
import es.iesagora.proyectointermodular.data.remote.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UserRepository {

    private final ApiService apiService;
    private static final String BASE_URL = "https://kern-blue.vercel.app/";
    //para pruebas locales:
    //private static final String BASE_URL = "http://10.0.2.2:8002/";

    public interface AuthCallback {
        void onSuccess(User user);
        void onFailure(String message);
    }

    public interface StatsCallback {
        void onSuccess(ApiService.UserStats stats);
        void onFailure(String message);
    }

    public UserRepository() {
        this.apiService = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService.class);
    }

    public void performLogin(android.content.Context context, String email, String password, final AuthCallback callback) {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        apiService.login(body).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    User loggedInUser = loginResponse.getUser();
                    String token = loginResponse.getAccessToken();

                    if (loggedInUser != null && token != null) {
                        android.content.SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);//faltaba esto
                        prefs.edit().putString("accessToken", token).apply();
                        prefs.edit().putInt("userId", loggedInUser.getId()).apply();//faltaba esto
                        loggedInUser.setToken(token);
                        callback.onSuccess(loggedInUser);
                    } else {
                        callback.onFailure("Usuario nulo en la respuesta.");
                    }
                } else {
                    callback.onFailure("Credenciales incorrectas (Error " + response.code() + ")");
                }
            }
            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                callback.onFailure("Error de conexión: " + t.getMessage());
            }
        });
    }

    //Modificamos el registro para que guarde el token
    public void performRegister(android.content.Context context, String email, String password, String name, String surname, String phone, final AuthCallback callback) {
        Map<String, String> body = new HashMap<>();
        body.put("username", email);
        body.put("email", email);
        body.put("password", password);
        body.put("full_name", name + " " + surname);
        body.put("phone", phone);

        apiService.register(body).enqueue(new Callback<LoginResponse>() {//Usamos el LoginResponse
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    //Extraemos el token que ahora envía la API de python
                    String token = response.body().getAccessToken();
                    //Guardamos el token en el dispositivo
                    android.content.SharedPreferences prefs = context.getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE);
                    prefs.edit().putString("accessToken", token).apply();

                    callback.onSuccess(response.body().getUser());
                } else {
                    callback.onFailure("Error en el registro.");
                }
            }
            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                callback.onFailure("Error de red: " + t.getMessage());
            }
        });
    }

    //Recuperar el perfil
    public void getUserProfile(android.content.Context context, final AuthCallback callback){
        //primero recuperamos el token guardado
        android.content.SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        String token = prefs.getString("accessToken", "");

        if (token == null || token.isEmpty()){
            callback.onFailure("Token no encontrado");
            return;
        }
        //Nos AEGURAMOS de que solo haya UN espacio entre Bearer y el token
        String authToken = "Bearer " + token.trim();

        //*--* esto lo usamos para ver en el logcat a que ruta nos envia (pruebas) *--*
        Log.d("API_DEBUG", "URL: " + BASE_URL + "users/me");
        Log.d("API_DEBUG", "Header enviado: " + authToken);

        // lo segundo es hacer la llamada al endpoint con el formato "Bearer "
        apiService.getUserProfile(authToken).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null){
                    User user = response.body();
                    // IMPORTANTE: Aseguramos que el userId esté guardado para la lógica de sesiones
                    android.content.SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("userId", user.getId());
                    editor.apply();

                    callback.onSuccess(user);
                } else {
                    if (response.code() == 401){
                        callback.onFailure("SESSION_EXPIRED");
                    } else {
                        callback.onFailure("Error al obtener perfil" + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                callback.onFailure("Error de red: " + t.getMessage());
            }
        });
    }

    //Ahora modificamos el cuestionario para que lea el Token
    public void completeQuiz(android.content.Context context, String routineName, final AuthCallback callback) {
        //Recuperamos el Token que guardamos en el registro
        android.content.SharedPreferences prefs = context.getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE);
        String token = prefs.getString("accessToken", "");

        String authToken = "Bearer " + token; //el fokin espacio de detras de Bearer se me olvidaba
        ApiService.QuizRequest request =  new ApiService.QuizRequest(routineName);

        apiService.completeQuiz(authToken, request).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    if (response.code() == 401) {
                        callback.onFailure("SESSION_EXPIRED");
                    } else {
                        callback.onFailure("Error " + response.code() + ": No estás autorizado.");
                    }
                }
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) {
                callback.onFailure("Error de conexión: " + t.getMessage());
            }
        });
    }

    public void getUserStats(android.content.Context context, final StatsCallback callback) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        String token = prefs.getString("accessToken", "");

        if (token == null || token.isEmpty()) {
            callback.onFailure("Token no encontrado");
            return;
        }

        String authToken = "Bearer " + token.trim();

        apiService.getUserStats(authToken).enqueue(new Callback<ApiService.UserStats>() {
            @Override
            public void onResponse(Call<ApiService.UserStats> call, Response<ApiService.UserStats> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    if (response.code() == 401) {
                        callback.onFailure("SESSION_EXPIRED");
                    } else {
                        callback.onFailure("Error al obtener estadísticas: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiService.UserStats> call, Throwable t) {
                callback.onFailure("Error de red: " + t.getMessage());
            }
        });
    }

    public void updateUserAvatar(Context context, String avatarUrl, final AuthCallback callback) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        String token = prefs.getString("accessToken", "");

        if (token == null || token.isEmpty()) {
            callback.onFailure("Token no encontrado");
            return;
        }

        String authToken = "Bearer " + token.trim();
        ApiService.AvatarUpdateRequest request = new ApiService.AvatarUpdateRequest(avatarUrl);

        apiService.updateAvatar(authToken, request).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    if (response.code() == 401) {
                        callback.onFailure("SESSION_EXPIRED");
                    } else {
                        callback.onFailure("Error al actualizar avatar: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                callback.onFailure("Error de red: " + t.getMessage());
            }
        });
    }

    public void updateUsername(Context context, String newUsername, final AuthCallback callback) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        String token = prefs.getString("accessToken", "");

        if (token == null || token.isEmpty()) {
            callback.onFailure("Token no encontrado");
            return;
        }

        String authToken = "Bearer " + token.trim();
        ApiService.UsernameUpdateRequest request = new ApiService.UsernameUpdateRequest(newUsername);

        apiService.updateUsername(authToken, request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Refrescar el token en SharedPreferences
                    String newToken = response.body().getAccessToken();
                    if (newToken != null) {
                        prefs.edit().putString("accessToken", newToken).apply();
                    }
                    callback.onSuccess(response.body().getUser());
                } else {
                    if (response.code() == 401) {
                        callback.onFailure("SESSION_EXPIRED");
                    } else if (response.code() == 400) {
                        try {
                            String errorBody = response.errorBody() != null ? response.errorBody().string() : "El nombre ya está en uso";
                            callback.onFailure("Error: " + errorBody);
                        } catch (Exception e) {
                            callback.onFailure("El nombre de usuario ya está en uso");
                        }
                    } else {
                        callback.onFailure("Error al actualizar nombre de usuario: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                callback.onFailure("Error de red: " + t.getMessage());
            }
        });
    }
}