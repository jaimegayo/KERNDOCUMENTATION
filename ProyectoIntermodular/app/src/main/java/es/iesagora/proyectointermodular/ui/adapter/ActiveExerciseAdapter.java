package es.iesagora.proyectointermodular.ui.adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import es.iesagora.proyectointermodular.R;
import es.iesagora.proyectointermodular.data.model.RoutineResponse;

public class ActiveExerciseAdapter extends RecyclerView.Adapter<ActiveExerciseAdapter.ExerciseViewHolder> {

    private List<RoutineResponse.ExerciseResponse> exercises;
    private OnVolumenChangeListener volumenListener;

    // Interfaz para avisar al Fragment que el volumen ha cambiado
    public interface OnVolumenChangeListener {
        void onVolumenChanged(double totalVolume, int totalSets);
    }

    public ActiveExerciseAdapter(List<RoutineResponse.ExerciseResponse> exercises, OnVolumenChangeListener listener) {
        this.exercises = exercises;
        this.volumenListener = listener;
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_active_exercise, parent, false);
        return new ExerciseViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        RoutineResponse.ExerciseResponse exercise = exercises.get(position);
        holder.tvExerciseName.setText(exercise.getName());

        // Configurar los Tips (ocultos por defecto)
        StringBuilder tipsText = new StringBuilder();
        if (exercise.getInstructions() != null && !exercise.getInstructions().isEmpty()) {
            tipsText.append("INSTRUCCIONES:\n").append(exercise.getInstructions()).append("\n\n");
        }
        tipsText.append("PRO TIPS:\n");
        if (exercise.getProTips() != null && !exercise.getProTips().isEmpty()) {
            for(String tip : exercise.getProTips()) tipsText.append("- ").append(tip).append("\n");
        } else {
            tipsText.append("- No hay tips disponibles\n");
        }

        tipsText.append("\nERRORES COMUNES:\n");
        if (exercise.getCommonMistakes() != null && !exercise.getCommonMistakes().isEmpty()) {
            for(String mistake : exercise.getCommonMistakes()) tipsText.append("- ").append(mistake).append("\n");
        } else {
            tipsText.append("- No hay errores registrados\n");
        }

        holder.tvTipsContent.setText(tipsText.toString());

        // Click en el nombre para mostrar/ocultar tips
        holder.tvExerciseName.setOnClickListener(v -> {
            int visibility = holder.layoutTips.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
            holder.layoutTips.setVisibility(visibility);
        });

        // Aquí inflaremos las series
        setupSeries(holder, exercise);

        // Lógica para añadir nuevas series
        holder.btnAddSerie.setOnClickListener(v -> {
            RoutineResponse.SerieResponse nuevaSerie = new RoutineResponse.SerieResponse();
            nuevaSerie.setAnterior("--"); // Por defecto para nuevas series
            exercise.getSeries().add(nuevaSerie);
            setupSeries(holder, exercise);
            calcularVolumenTotal();
        });
    }

    private void setupSeries(ExerciseViewHolder holder, RoutineResponse.ExerciseResponse exercise) {
        holder.llSeriesContainer.removeAllViews();
        for (int i = 0; i < exercise.getSeries().size(); i++) {
            RoutineResponse.SerieResponse serie = exercise.getSeries().get(i);
            View serieView = LayoutInflater.from(holder.itemView.getContext()).inflate(R.layout.item_active_serie, holder.llSeriesContainer, false);

            TextView tvNum = serieView.findViewById(R.id.tvSerieNum);
            TextView tvAnterior = serieView.findViewById(R.id.tvSerieAnterior); // Referencia al campo histórico
            EditText etKilos = serieView.findViewById(R.id.etKilos);
            EditText etReps = serieView.findViewById(R.id.etReps);
            CheckBox cbDone = serieView.findViewById(R.id.cbDone);

            tvNum.setText(String.valueOf(i + 1));
            tvAnterior.setText(serie.getAnterior()); // Seteamos el valor del "Anterior" que viene del backend

            // LÓGICA DE PROGRESIÓN: Rellenar automáticamente con lo hecho la vez anterior
            // Priorizamos los datos históricos (prevKilos) sobre los valores base de la rutina
            if (serie.getPrevKilos() > 0) {
                serie.setKilos(serie.getPrevKilos());
                serie.setReps(serie.getPrevReps());
            }

            // Mostrar los valores en los EditText (si son 0, los dejamos vacíos o puedes poner el valor)
            etKilos.setText(serie.getKilos() == 0 ? "" : String.valueOf(serie.getKilos()));
            etReps.setText(serie.getReps() == 0 ? "" : String.valueOf(serie.getReps()));

            cbDone.setChecked(serie.isDone());

            // --- LÓGICA DE BORRADO DE SERIE POR CLIC LARGO ---
            serieView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(v.getContext())
                        .setTitle("Eliminar serie")
                        .setMessage("¿Quieres borrar esta serie?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            exercise.getSeries().remove(serie);
                            setupSeries(holder, exercise); // Refrescamos las series de este ejercicio
                            calcularVolumenTotal(); // Recalculamos totales
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
                return true;
            });

            etKilos.addTextChangedListener(new SimpleTextWatcher(s -> {
                try { serie.setKilos(Double.parseDouble(s)); } catch (Exception e) { serie.setKilos(0); }
                if(serie.isDone()) calcularVolumenTotal();
            }));

            etReps.addTextChangedListener(new SimpleTextWatcher(s -> {
                try { serie.setReps(Integer.parseInt(s)); } catch (Exception e) { serie.setReps(0); }
                if(serie.isDone()) calcularVolumenTotal();
            }));

            cbDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
                serie.setDone(isChecked);
                calcularVolumenTotal();
            });

            holder.llSeriesContainer.addView(serieView);
        }
    }

    public void calcularVolumenTotal() {
        double vol = 0;
        int sets = 0;
        for (RoutineResponse.ExerciseResponse ex : exercises) {
            if (ex.getSeries() != null) {
                for (RoutineResponse.SerieResponse s : ex.getSeries()) {
                    if (s.isDone()) {
                        vol += (s.getKilos() * s.getReps());
                        sets++;
                    }
                }
            }
        }
        volumenListener.onVolumenChanged(vol, sets);
    }

    @Override
    public int getItemCount() { return exercises.size(); }

    static class ExerciseViewHolder extends RecyclerView.ViewHolder {
        TextView tvExerciseName, tvTipsContent;
        LinearLayout layoutTips, llSeriesContainer;
        Button btnAddSerie;

        public ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvExerciseName = itemView.findViewById(R.id.tvExerciseName);
            tvTipsContent = itemView.findViewById(R.id.tvTipsContent);
            layoutTips = itemView.findViewById(R.id.layoutTips);
            llSeriesContainer = itemView.findViewById(R.id.llSeriesContainer);
            btnAddSerie = itemView.findViewById(R.id.btnAddSerie);
        }
    }

    private interface TextChanged { void onUpdate(String s); }
    private static class SimpleTextWatcher implements TextWatcher {
        private final TextChanged listener;
        public SimpleTextWatcher(TextChanged listener) { this.listener = listener; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { listener.onUpdate(s.toString()); }
        @Override public void afterTextChanged(Editable s) {}
    }
}