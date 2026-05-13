package es.iesagora.proyectointermodular.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;
import java.util.List;
import es.iesagora.proyectointermodular.R;
import es.iesagora.proyectointermodular.data.remote.ApiService;
import es.iesagora.proyectointermodular.data.model.RoutineResponse;

public class WorkoutDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_SESSION = "arg_session";
    private ApiService.WorkoutHistoryResponse session;

    public static WorkoutDetailBottomSheet newInstance(ApiService.WorkoutHistoryResponse session) {
        WorkoutDetailBottomSheet fragment = new WorkoutDetailBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_SESSION, new Gson().toJson(session));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String json = getArguments().getString(ARG_SESSION);
            session = new Gson().fromJson(json, ApiService.WorkoutHistoryResponse.class);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_workout_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTitle = view.findViewById(R.id.tv_bs_routine_name);
        LinearLayout llExercises = view.findViewById(R.id.ll_bs_exercises);

        if (session != null) {
            tvTitle.setText(session.getRoutineName());

            List<RoutineResponse.ExerciseResponse> exercises = session.getDataJson();
            if (exercises != null && !exercises.isEmpty()) {
                for (RoutineResponse.ExerciseResponse ex : exercises) {
                    View exView = getLayoutInflater().inflate(R.layout.item_bs_exercise, llExercises, false);
                    TextView tvExName = exView.findViewById(R.id.tv_bs_ex_name);
                    TextView tvExDetails = exView.findViewById(R.id.tv_bs_ex_details);

                    // data_json has "name" or "exercise_name" (RoutineResponse.ExerciseResponse has name)
                    String name = ex.getName() != null ? ex.getName() : "Ejercicio";
                    tvExName.setText(name);

                    StringBuilder details = new StringBuilder();
                    if (ex.getSeries() != null) {
                        for (RoutineResponse.SerieResponse s : ex.getSeries()) {
                            details.append("Serie ").append(s.getNumSerie()).append(": ")
                                    .append(s.getKilos()).append("kg x ")
                                    .append(s.getReps()).append("\n");
                        }
                    }
                    tvExDetails.setText(details.toString().trim());
                    llExercises.addView(exView);
                }
            } else {
                TextView tvEmpty = new TextView(getContext());
                tvEmpty.setText("No hay detalles guardados para este entrenamiento.");
                tvEmpty.setTextColor(0xFFBBBBBB);
                llExercises.addView(tvEmpty);
            }
        }
    }
}
