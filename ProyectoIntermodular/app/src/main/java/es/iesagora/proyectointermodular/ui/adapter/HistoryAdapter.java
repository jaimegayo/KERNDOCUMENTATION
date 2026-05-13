package es.iesagora.proyectointermodular.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import es.iesagora.proyectointermodular.R;
import es.iesagora.proyectointermodular.data.remote.ApiService;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ApiService.WorkoutHistoryResponse session);
    }

    private List<ApiService.WorkoutHistoryResponse> historyList;
    private OnItemClickListener listener;

    public HistoryAdapter(List<ApiService.WorkoutHistoryResponse> historyList, OnItemClickListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    public void setHistoryList(List<ApiService.WorkoutHistoryResponse> historyList) {
        this.historyList = historyList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        ApiService.WorkoutHistoryResponse item = historyList.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return historyList == null ? 0 : historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoutineName, tvDate, tvVolume, tvDuration;
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()); // ej: 25 Oct 2023

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoutineName = itemView.findViewById(R.id.tv_history_routine_name);
            tvDate = itemView.findViewById(R.id.tv_history_date);
            tvVolume = itemView.findViewById(R.id.tv_history_volume);
            tvDuration = itemView.findViewById(R.id.tv_history_duration);
        }

        public void bind(ApiService.WorkoutHistoryResponse item, OnItemClickListener listener) {
            tvRoutineName.setText(item.getRoutineName() != null ? item.getRoutineName() : "Entrenamiento");
            tvVolume.setText(item.getTotalVolume() + " kg");

            int minutes = item.getDurationSeconds() / 60;
            tvDuration.setText(minutes + " min");

            try {
                if (item.getCreatedAt() != null) {
                    Date date = inputFormat.parse(item.getCreatedAt().split("T")[0]);
                    tvDate.setText(outputFormat.format(date));
                } else {
                    tvDate.setText("Desconocida");
                }
            } catch (Exception e) {
                tvDate.setText(item.getCreatedAt());
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }
    }
}
