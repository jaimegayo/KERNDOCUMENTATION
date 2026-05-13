package es.iesagora.proyectointermodular.data.model;

import com.google.gson.annotations.SerializedName;

public class Serie {
    private int numSerie;
    private double kilos;
    private int reps;

    // Campos para el historial
    private String anterior;
    @SerializedName("prev_kilos")
    private double prevKilos;
    @SerializedName("prev_reps")
    private int prevReps;

    public Serie (int numSerie, double kilos, int reps) {
        this.numSerie = numSerie;
        this.kilos = kilos;
        this.reps = reps;
        this.anterior = "--"; // Valor por defecto
    }
    public int getNumSerie() { return numSerie; }
    public void setNumSerie(int numSerie) { this.numSerie = numSerie; }
    public double getKilos() { return kilos; }
    public void setKilos(double kilos) { this.kilos = kilos; }
    public int getReps() { return reps; }
    public void setReps(int reps) { this.reps = reps; }

    // Getters y setters para los nuevos campos del historial
    public String getAnterior() { return anterior; }
    public void setAnterior(String anterior) { this.anterior = anterior; }
    public double getPrevKilos() { return prevKilos; }
    public void setPrevKilos(double prevKilos) { this.prevKilos = prevKilos; }
    public int getPrevReps() { return prevReps; }
    public void setPrevReps(int prevReps) { this.prevReps = prevReps; }
}