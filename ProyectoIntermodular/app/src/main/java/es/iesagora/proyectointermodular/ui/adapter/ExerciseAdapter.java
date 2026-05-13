package es.iesagora.proyectointermodular.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.security.spec.ECField;
import java.util.List;
import es.iesagora.proyectointermodular.data.model.Exercise;
import es.iesagora.proyectointermodular.databinding.ItemEjercicioBuscadorBinding;
import es.iesagora.proyectointermodular.viewmodel.ExerciseViewModel;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ExerciseViewHolder> {
    private List<Exercise> list;
    private final OnExerciseClickListener listener;

    //interfaz para que el fragment sepa que ejercicio se ha pulsado
    public interface OnExerciseClickListener {
        void onAddClick (Exercise exercise);
    }

    public ExerciseAdapter(List<Exercise> list, OnExerciseClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // vamos a usar el binding del xml que creamos antes (itemejerciciobuscador)
        ItemEjercicioBuscadorBinding binding = ItemEjercicioBuscadorBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ExerciseViewHolder(binding);
    }
    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        Exercise exercise = list.get(position);

        // rellenamos los datos en la tarjeta
        holder.binding.tvExerciseName.setText(exercise.getName());

        String info = exercise.getPrimaryMuscle() + " • " + exercise.getEquipment();
        holder.binding.tvExerciseInfo.setText(info);

        // configuramos el click del boton "+"
        holder.binding.btnAddToRoutine.setOnClickListener(v -> {
            listener.onAddClick(exercise);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
    // metodo importante para el buscador: actualiza la lista al filtrar
    public void setList(List<Exercise> newList) {
        this.list = newList;
        notifyDataSetChanged(); //con esto le decimos al RecyclerView que se redibuje
    }

    public static class ExerciseViewHolder extends RecyclerView.ViewHolder {
        ItemEjercicioBuscadorBinding binding;
        public ExerciseViewHolder(ItemEjercicioBuscadorBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
