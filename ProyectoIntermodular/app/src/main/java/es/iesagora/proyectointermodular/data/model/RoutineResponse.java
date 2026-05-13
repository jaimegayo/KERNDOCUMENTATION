package es.iesagora.proyectointermodular.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RoutineResponse implements Serializable {
    private int id;
    private String name;
    @SerializedName("created_at")
    private String createdAt;
    @SerializedName("total_exercises")
    private int totalExercises;
    @SerializedName("total_series")
    private int totalSeries;
    @SerializedName("total_volume")
    double totalVolume;
    private List<ExerciseResponse> exercises = new ArrayList<>(); // Inicializada para evitar null

    // Getters y Setters
    public int getId() { return id; }
    public String getName() { return name != null ? name : "Sin nombre"; }
    public int getTotalExercises() { return totalExercises; }
    public int getTotalSeries() { return totalSeries; }
    public double getTotalVolume() { return totalVolume; }
    public List<ExerciseResponse> getExercises() {
        return exercises != null ? exercises : new ArrayList<>();
    }

    public static class ExerciseResponse implements Serializable {
        private int id;
        private String name;
        private List<SerieResponse> series = new ArrayList<>(); // Inicializada

        // Campos adicionales para Tips e Instrucciones
        private String instructions;
        @SerializedName("pro_tips")
        private List<String> proTips = new ArrayList<>();
        @SerializedName("common_mistakes")
        private List<String> commonMistakes = new ArrayList<>();

        public int getId() { return id; }
        public String getName() { return name != null ? name : "Ejercicio"; }
        public List<SerieResponse> getSeries() {
            return series != null ? series : new ArrayList<>();
        }
        public String getInstructions() { return instructions != null ? instructions : "Sin instrucciones"; }
        public List<String> getProTips() {
            return proTips != null ? proTips : new ArrayList<>();
        }
        public List<String> getCommonMistakes() {
            return commonMistakes != null ? commonMistakes : new ArrayList<>();
        }
    }

    public static class SerieResponse implements Serializable {
        private int numSerie;
        private double kilos;
        private int reps;
        private String anterior; // Añadido para mostrar el historial del backend

        // Nuevos campos para mapear los valores numéricos del historial
        @SerializedName("prev_kilos")
        private double prevKilos;
        @SerializedName("prev_reps")
        private int prevReps;

        // Campo local (no viene de la API) para controlar el Check en la app
        private boolean isDone = false;

        public int getNumSerie() { return numSerie; }
        public double getKilos() { return kilos; }
        public int getReps() { return reps; }
        public String getAnterior() { return anterior != null ? anterior : "--"; } // Getter para el anterior
        public double getPrevKilos() { return prevKilos; }
        public int getPrevReps() { return prevReps; }
        public boolean isDone() { return isDone; }

        // Setters necesarios para actualizar los datos dinámicamente en el entrenamiento
        public void setKilos(double kilos) { this.kilos = kilos; }
        public void setReps(int reps) { this.reps = reps; }
        public void setDone(boolean done) { isDone = done; }
        public void setAnterior(String anterior) { this.anterior = anterior; }
        public void setPrevKilos(double prevKilos) { this.prevKilos = prevKilos; }
        public void setPrevReps(int prevReps) { this.prevReps = prevReps; }
    }
}