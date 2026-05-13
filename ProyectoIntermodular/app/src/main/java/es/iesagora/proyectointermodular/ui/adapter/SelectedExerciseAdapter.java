package es.iesagora.proyectointermodular.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import es.iesagora.proyectointermodular.R;
import es.iesagora.proyectointermodular.data.model.Exercise;
import es.iesagora.proyectointermodular.data.model.Serie;
import es.iesagora.proyectointermodular.databinding.ItemEjercicioSeleccionadoBinding;
import es.iesagora.proyectointermodular.databinding.ItemFilaSerieBinding;
public class SelectedExerciseAdapter extends RecyclerView.Adapter<SelectedExerciseAdapter.ViewHolder>{
    private List<Exercise> selectedExercises;

    public SelectedExerciseAdapter(List<Exercise> selectedExercises) {
        this.selectedExercises = selectedExercises;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEjercicioSeleccionadoBinding binding = ItemEjercicioSeleccionadoBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Exercise exercise = selectedExercises.get(position);
        holder.bind(exercise);
    }

    @Override
    public int getItemCount() {
        return selectedExercises.size();
    }

    public void setList(List<Exercise> newList) {
        this.selectedExercises = newList;
        // Usamos notifyDataSetChanged para asegurar que el RecyclerView
        // recalcule su altura total dentro del ScrollView
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemEjercicioSeleccionadoBinding binding;

        public ViewHolder(ItemEjercicioSeleccionadoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Exercise exercise) {
            binding.tvNombreEjercicioSeleccionado.setText(exercise.getName());
            binding.tvInstruccionesDetalle.setText(exercise.getInstructions());
            binding.tvTipsDetalle.setText(exercise.getFormattedTips());

            //Logica del desplegable (protips, commonmistakes...) del card de ejercicio cuando pulsas sobre el title
            binding.layoutCabeceraEjercicio.setOnClickListener(v -> {
                int visibility = binding.layoutDetalleExpandible.getVisibility() == View.GONE ? View.VISIBLE : View.GONE;
                binding.layoutDetalleExpandible.setVisibility(visibility);
            });

            //Dibujamos las series que ya tiene el ejercicio
            refreshSeriesUI(exercise);

            //Botón de añadir nueva serie
            binding.btnAddSerieFila.setOnClickListener(v -> {
                int nuevoNum = exercise.getSeries().size() + 1;
                exercise.getSeries().add(new Serie(nuevoNum, 0.0, 10));
                refreshSeriesUI(exercise);
            });

            //Botón para eliminar ejercicio de la rutina
            binding.btnEliminarEjercicio.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    selectedExercises.remove(pos);
                    notifyItemRemoved(pos);
                    notifyItemRangeChanged(pos, selectedExercises.size());

                    // Importante: Si la lista se queda vacía tras borrar,
                    // el observe del fragment se encargará de mostrar el Empty State automáticamente.
                }
            });
        }

        private void refreshSeriesUI(Exercise exercise) {
            //limpiamos el contenedor para no duplicar filas al reciclar la vista
            binding.containerSeries.removeAllViews();

            for (Serie serie : exercise.getSeries()) {
                //inflamos el XML de la fila usando ViewBinding para la fila
                ItemFilaSerieBinding filaBinding = ItemFilaSerieBinding.inflate(
                        LayoutInflater.from(binding.getRoot().getContext()),
                        binding.containerSeries,
                        false);

                filaBinding.tvNumSerie.setText(String.valueOf(serie.getNumSerie()));
                //evitaremos que los ceros molesten al escribir
                filaBinding.etKilosSerie.setText(serie.getKilos() == 0 ? "" : String.valueOf(serie.getKilos()));
                filaBinding.etRepsSerie.setText(serie.getReps() == 0 ? "" : String.valueOf(serie.getReps()));

                //TEXTWATCHERS
                //GUARDAR LOS KILOS
                filaBinding.etKilosSerie.addTextChangedListener(new android.text.TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override public void afterTextChanged(android.text.Editable s) {
                        try {
                            serie.setKilos(Double.parseDouble(s.toString()));
                        } catch (NumberFormatException e) {
                            serie.setKilos(0.0);
                        }
                    }
                });

                //GUARDAR REPETICIONES
                filaBinding.etRepsSerie.addTextChangedListener(new android.text.TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    @Override public void afterTextChanged(android.text.Editable s) {
                        try {
                            serie.setReps(Integer.parseInt(s.toString()));
                        } catch (NumberFormatException e) {
                            serie.setReps(0);
                        }
                    }
                });
                //Botón para eliminar esta serie en concreto
                filaBinding.btnEliminarFilaSerie.setOnClickListener(v -> {
                    exercise.getSeries().remove(serie);
                    //reordenar los números de serie tras borrar
                    for (int i = 0; i < exercise.getSeries().size(); i++) {
                        exercise.getSeries().get(i).setNumSerie(i + 1);
                    }
                    refreshSeriesUI(exercise);
                });

                binding.containerSeries.addView(filaBinding.getRoot());
            }
        }
    }
}

