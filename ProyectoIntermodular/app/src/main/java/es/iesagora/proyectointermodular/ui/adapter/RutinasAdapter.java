package es.iesagora.proyectointermodular.ui.adapter;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import es.iesagora.proyectointermodular.R;
import es.iesagora.proyectointermodular.data.model.RoutineResponse;

public class RutinasAdapter extends RecyclerView.Adapter<RutinasAdapter.RutinaViewHolder> {

    private List<RoutineResponse> rutinas;
    private OnRutinaClickListener listener;

    // Interfaz para gestionar los clics en los botones
    public interface OnRutinaClickListener {
        void onEmpezarClick(RoutineResponse rutina);
    }

    public RutinasAdapter(List<RoutineResponse> rutinas, OnRutinaClickListener listener) {
        this.rutinas = rutinas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RutinaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rutina_card, parent, false);
        return new RutinaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RutinaViewHolder holder, int position) {
        RoutineResponse rutina = rutinas.get(position);
        holder.tvNombre.setText(rutina.getName().toUpperCase());

        // CLIC EN LA TARJETA COMPLETA: Navegar al detalle
        holder.itemView.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putInt("routine_id", rutina.getId());
            Navigation.findNavController(v).navigate(R.id.action_workoutFragment_to_routineDetailFragment, bundle);
        });

        // CLIC EN EL BOTÓN: Empezar rutina (usa el listener)
        holder.btnEmpezar.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEmpezarClick(rutina);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rutinas != null ? rutinas.size() : 0;
    }

    public void updateList(List<RoutineResponse> newList) {
        this.rutinas = newList;
        notifyDataSetChanged();
    }

    static class RutinaViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre;
        View btnEmpezar;

        public RutinaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombreRutina);
            btnEmpezar = itemView.findViewById(R.id.btnEmpezarRutina);
        }
    }
}