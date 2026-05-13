package es.iesagora.proyectointermodular.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import es.iesagora.proyectointermodular.data.model.RoutineResponse.ExerciseResponse;
import es.iesagora.proyectointermodular.data.model.RoutineResponse.SerieResponse;
import es.iesagora.proyectointermodular.databinding.ItemEjercicioDetalleBinding;
import es.iesagora.proyectointermodular.databinding.ItemSerieDetalleRowBinding;

public class RoutineDetailAdapter extends RecyclerView.Adapter<RoutineDetailAdapter.ViewHolder> {

    private List<ExerciseResponse> exercises;

    public RoutineDetailAdapter(List<ExerciseResponse> exercises) {
        this.exercises = exercises;
    }

    // METODO para actualizar la lista cuando lleguen datos de la API
    public void setExercises(List<ExerciseResponse> exercises) {
        this.exercises = exercises;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEjercicioDetalleBinding binding = ItemEjercicioDetalleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExerciseResponse exercise = exercises.get(position);

        // >>> DEBUG: Comprobamos qué ejercicio estamos procesando
        android.util.Log.d("DEBUG_APP", "Pintando ejercicio: " + exercise.getName());

        // Ponemos el nombre del ejercicio (en mayúsculas para el estilo premium)
        holder.binding.tvExerciseDetailName.setText(exercise.getName().toUpperCase());
        // Limpiamos el contenedor de series para evitar duplicados al reciclar vistas
        holder.binding.llSeriesContainer.removeAllViews();

        // LÓGICA PARA INFLAR LAS SERIES DINÁMICAMENTE (MODIFICADO: Mostramos solo la primera serie como resumen)
        if (exercise.getSeries() != null && !exercise.getSeries().isEmpty()) {
            SerieResponse serie = exercise.getSeries().get(0);

            // Inflamos la fila de resumen usando el layout item_serie_detalle_row
            ItemSerieDetalleRowBinding serieBinding = ItemSerieDetalleRowBinding.inflate(
                    LayoutInflater.from(holder.itemView.getContext()),
                    holder.binding.llSeriesContainer,
                    false
            );

            // --- Lógica de Prioridad para el Resumen Informativo ---
            if (serie.getPrevKilos() > 0) {
                // Si hay historial, mostramos el progreso del último entrenamiento
                serieBinding.tvSerieNum.setText("ÚLT.");
                serieBinding.tvSerieKg.setText(serie.getPrevKilos() + "kg");
                serieBinding.tvSerieReps.setText(serie.getPrevReps() + "reps");
            } else {
                // Si es la primera vez o no hay historial, mostramos los valores base
                serieBinding.tvSerieNum.setText("BASE");
                serieBinding.tvSerieKg.setText(serie.getKilos() + "kg");
                serieBinding.tvSerieReps.setText(serie.getReps() + "reps");
            }

            // Añadimos la fila resumen al contenedor
            holder.binding.llSeriesContainer.addView(serieBinding.getRoot());
        }

        // De momento dejamos la imagen por defecto o podrías cargarla con Glide/Picasso más adelante
    }

    @Override
    public int getItemCount() {
        return exercises != null ? exercises.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ItemEjercicioDetalleBinding binding;

        public ViewHolder(@NonNull ItemEjercicioDetalleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}