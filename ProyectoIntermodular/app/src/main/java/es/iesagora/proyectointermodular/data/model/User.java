package es.iesagora.proyectointermodular.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Clase que representa un usuario, coincide con la respuesta de la API FastAPI.
 */
public class User {
    // Campos que coinciden con la API
    @SerializedName("id")
    private Integer id;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("phone")
    private String phone;

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("role")
    private String role;

    @SerializedName("bio")
    private String bio;

    // --- NUEVOS CAMPOS DEL CUESTIONARIO ---
    @SerializedName("has_completed_quiz")
    private boolean hasCompletedQuiz;

    @SerializedName("assigned_routine")
    private String assignedRoutine;
    // --------------------------------------

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("disabled")
    private Boolean disabled;

    // Token NO viene del API en el objeto user, se guarda por separado
    private String token;

    // Constructor vacío requerido por Gson
    public User() {
    }

    // Getters
    public Integer getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getRole() {
        return role;
    }

    public String getBio() {
        return bio;
    }

    public boolean isHasCompletedQuiz() {
        return hasCompletedQuiz;
    }

    public String getAssignedRoutine() {
        return assignedRoutine;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public String getToken() {
        return token;
    }

    // Setters
    public void setToken(String token) {
        this.token = token;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setHasCompletedQuiz(boolean hasCompletedQuiz) {
        this.hasCompletedQuiz = hasCompletedQuiz;
    }

    public void setAssignedRoutine(String assignedRoutine) {
        this.assignedRoutine = assignedRoutine;
    }

    // Métodos de compatibilidad para código existente
    public String getUserId() {
        return id != null ? id.toString() : null;
    }

    public String getName() {
        return fullName;
    }
}