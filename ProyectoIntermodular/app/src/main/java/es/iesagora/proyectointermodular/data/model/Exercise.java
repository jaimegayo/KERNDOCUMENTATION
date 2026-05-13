package es.iesagora.proyectointermodular.data.model;

import java.util.List;
import java.util.ArrayList;
import com.google.gson.annotations.SerializedName;

public class Exercise {

    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("primary_muscle")
    private String primaryMuscle;

    @SerializedName("secondary_muscles")
    private List<String> secondaryMuscles;

    @SerializedName("equipment")
    private String equipment;

    @SerializedName("difficulty")
    private String difficulty;

    @SerializedName("exercise_type")
    private String exerciseType;

    @SerializedName("movement_pattern")
    private String movementPattern;

    @SerializedName("unilateral")
    private boolean unilateral;

    @SerializedName("instructions")
    private String instructions;

    @SerializedName("pro_tips")
    private List<String> proTips;

    @SerializedName("common_mistakes")
    private List<String> commonMistakes;

    //NUEVO ATRIBUTO PARA LA RUTINA -- no viene en el json, lo va a llenar el usuario en lo de crear rutina
    private List<Serie> series;

    public Exercise(){
        //inicializamos la lista de series para evitar errores de NullPointerException
        this.series = new ArrayList<>();
    }

    // --- Getters ---
    public int getId() { return id; }
    public String getName() { return name; }
    public String getPrimaryMuscle() { return primaryMuscle; }
    public List<String> getSecondaryMuscles() { return secondaryMuscles; }
    public String getEquipment() { return equipment; }
    public String getDifficulty() { return difficulty; }
    public String getExerciseType() { return exerciseType; }
    public String getMovementPattern() { return movementPattern; }
    public boolean isUnilateral() { return unilateral; }
    public String getInstructions() { return instructions; }
    public List<String> getProTips() { return proTips; }
    public List<String> getCommonMistakes() { return commonMistakes; }

    // --- Setters ---

    // AÑADIDO: METODO necesario para que RoutineDetailFragment pueda setear el nombre al editar
    public void setName(String name) {
        this.name = name;
    }

    //NUEVO metodo para las series
    public List<Serie> getSeries(){
        return series;
    }
    public void setSeries(List<Serie> series){
        this.series = series;
    }

    /**
     * -- METODO AUXILIAR --
     * Convertir una lista de strings (como pro_tips) en un solo texto con puntos.
     */

    public String getFormattedTips(){
        if (proTips == null || proTips.isEmpty()) return "Sin consejos adicionales.";
        StringBuilder sb = new StringBuilder();
        for (String tip : proTips) {
            sb.append("• ").append(tip).append("\n");
        }
        return sb.toString().trim();
    }
}