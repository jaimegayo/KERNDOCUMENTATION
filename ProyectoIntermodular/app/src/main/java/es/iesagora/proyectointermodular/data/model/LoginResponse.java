package es.iesagora.proyectointermodular.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Modelo exacto para la respuesta del endpoint POST /login de la API de FastAPI.
 */
public class LoginResponse {

    // Vincula con la clave "accessToken" que definimos en FastAPI (app_clean.py)
    @SerializedName("accessToken")
    private String accessToken;

    // Vincula con "token_type" (standard en OAuth2)
    @SerializedName("token_type")
    private String tokenType;

    // Este es el punto clave: Aquí Retrofit inyectará el objeto User
    // que ya contiene el campo has_completed_quiz.
    @SerializedName("user")
    private User user;

    // Constructor vacío requerido por GSON para instanciar la respuesta
    public LoginResponse() {
    }

    // --- Getters ---

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public User getUser() {
        return user;
    }

    // --- Setters (Opcionales para Retrofit, pero útiles para testing) ---

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public void setUser(User user) {
        this.user = user;
    }
}