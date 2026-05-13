package es.iesagora.proyectointermodular.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    // Nombre del archivo unificado
    private static final String PREF_NAME = "prefs";
    private static final String KEY_TOKEN = "accessToken";

    // Función para GUARDAR el token
    public static void saveToken(Context context, String token) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_TOKEN, token);
        editor.apply();
    }

    // Función para SACAR el token
    public static String getToken(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_TOKEN, null);
    }

    // Función para BORRAR el token (Logout)
    public static void clearToken(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().remove(KEY_TOKEN).apply();
    }
}