package es.iesagora.proyectointermodular.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import es.iesagora.proyectointermodular.R;
import es.iesagora.proyectointermodular.data.model.Exercise;
import es.iesagora.proyectointermodular.data.model.Serie;
import es.iesagora.proyectointermodular.data.model.RoutineResponse;
import es.iesagora.proyectointermodular.data.repository.TokenManager;
import es.iesagora.proyectointermodular.databinding.FragmentRoutineDetailBinding;
import es.iesagora.proyectointermodular.ui.adapter.RoutineDetailAdapter;
import es.iesagora.proyectointermodular.viewmodel.RoutineViewModel;
import es.iesagora.proyectointermodular.data.remote.ApiService;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RoutineDetailFragment extends Fragment {

    private FragmentRoutineDetailBinding binding;
    private RoutineViewModel viewModel;
    private RoutineDetailAdapter adapter;
    private int routineId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Recuperamos el ID de la rutina que nos pasa el fragment anterior
        if (getArguments() != null) {
            routineId = getArguments().getInt("routine_id");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRoutineDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Usamos requireActivity() para asegurar que compartimos el ViewModel correctamente
        viewModel = new ViewModelProvider(requireActivity()).get(RoutineViewModel.class);

        setupRecyclerView();
        setupChart();

        // 1. Cargar los datos de la rutina y el historial
        String token = TokenManager.getToken(getContext());
        if (token != null) {
            viewModel.fetchRoutineById(routineId, token);
            viewModel.fetchWorkoutHistory(token);
        }

        // 2. Observar los cambios (Aquí es donde ocurre la magia)
        viewModel.getSelectedRoutine().observe(getViewLifecycleOwner(), routine -> {
            if (routine != null) {
                // Pintamos los datos generales (Los que NO van en el adapter)
                // Usamos toUpperCase para el estilo del prototipo
                if (routine.getName() != null) {
                    binding.tvDetailRoutineName.setText(routine.getName().toUpperCase());
                }

                // Concatenamos el texto para que no se borren las etiquetas "Ejercicios |" y "Series |"
                binding.tvDetailExercisesCount.setText("Ejercicios | " + routine.getTotalExercises());
                binding.tvDetailSeriesCount.setText("Series | " + routine.getTotalSeries());

                binding.tvDetailTotalVolume.setText(routine.getTotalVolume() + " kg");

                // Pasamos los ejercicios al adapter para que los pinte en la lista
                if (routine.getExercises() != null) {
                    adapter.setExercises(routine.getExercises());
                }

                // Si ya tenemos el historial cargado, actualizamos el gráfico
                if (viewModel.getWorkoutHistory().getValue() != null) {
                    updateChart(viewModel.getWorkoutHistory().getValue(), routine.getName());
                }
            }
        });

        // Observar el historial para actualizar el gráfico cuando llegue
        viewModel.getWorkoutHistory().observe(getViewLifecycleOwner(), history -> {
            RoutineResponse routine = viewModel.getSelectedRoutine().getValue();
            if (routine != null && history != null) {
                updateChart(history, routine.getName());
            }
        });

        // Observar posibles errores del servidor
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });

        // 3. Botón de Empezar Entrenamiento
        binding.btnStartWorkout.setOnClickListener(v -> {
            // REGLA DE EXCLUSIVIDAD: Comprobamos si ya hay un entrenamiento activo
            if (viewModel.isSessionActive(requireContext())) {
                mostrarDialogoSesionActiva();
                return;
            }

            RoutineResponse routine = viewModel.getSelectedRoutine().getValue();
            if (routine != null) {
                Toast.makeText(getContext(), "¡A darle caña!", Toast.LENGTH_SHORT).show();
                // Navegamos a la pantalla de entreno pasando la rutina actual
                Bundle bundle = new Bundle();
                bundle.putSerializable("routine", routine);
                Navigation.findNavController(v).navigate(R.id.action_routineDetailFragment_to_workoutActiveFragment, bundle);
            }
        });

        // 4. btnEditAction para Editar
        binding.btnEditAction.setOnClickListener(v -> {
            RoutineResponse routine = viewModel.getSelectedRoutine().getValue();
            if (routine != null) {
                // Preparamos el ViewModel con los datos actuales
                viewModel.setEditingRoutineId(routine.getId());
                viewModel.setRoutineName(routine.getName());

                // Pasamos los ejercicios actuales al "cesto" de seleccionados
                // Convertimos de RoutineResponse.ExerciseResponse a nuestro modelo Exercise común
                List<Exercise> exercisesToEdit = new ArrayList<>();
                if (routine.getExercises() != null) {
                    for (RoutineResponse.ExerciseResponse exRes : routine.getExercises()) {
                        Exercise ex = new Exercise();
                        ex.setName(exRes.getName());

                        // También convertimos las series
                        List<Serie> seriesList = new ArrayList<>();
                        if (exRes.getSeries() != null) {
                            for (RoutineResponse.SerieResponse sRes : exRes.getSeries()) {
                                seriesList.add(new Serie(sRes.getNumSerie(), sRes.getKilos(), sRes.getReps()));
                            }
                        }
                        ex.setSeries(seriesList);
                        exercisesToEdit.add(ex);
                    }
                }
                viewModel.setSelectedExercises(exercisesToEdit);

                // Navegamos a la pantalla de crear (que ahora actuará como editor)
                Navigation.findNavController(v).navigate(R.id.action_routineDetail_to_crearRutina);
            }
        });

        // 5. NUEVO: Lógica para borrar la rutina
        binding.ivDeleteAction.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar rutina")
                    .setMessage("¿Estás seguro de que quieres borrar esta rutina? Esta acción no se puede deshacer.")
                    .setPositiveButton("Eliminar", (dialog, which) -> {
                        String userToken = TokenManager.getToken(getContext());
                        if (userToken != null) {
                            viewModel.deleteRoutine(routineId, userToken);
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });

        // Observamos si el borrado ha sido exitoso para salir de la pantalla
        viewModel.getDeleteSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Toast.makeText(getContext(), "Rutina eliminada", Toast.LENGTH_SHORT).show();
                viewModel.resetDeleteStatus();
                // Volvemos atrás (al listado)
                Navigation.findNavController(requireView()).popBackStack();
            }
        });
    }

    private void mostrarDialogoSesionActiva() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Entrenamiento en curso")
                .setMessage("Ya tienes un entrenamiento activo. Debes terminarlo o cancelarlo antes de empezar uno nuevo.")
                .setPositiveButton("Ir al entrenamiento actual", (dialog, which) -> {
                    // Navegamos al entrenamiento activo (sin pasar rutina, el fragment la recuperará de SharedPreferences)
                    Navigation.findNavController(requireView()).navigate(R.id.workoutActiveFragment);
                })
                .setNegativeButton("Entendido", null)
                .show();
    }

    private void setupRecyclerView() {
        adapter = new RoutineDetailAdapter(new ArrayList<>());
        binding.rvRoutineDetailExercises.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvRoutineDetailExercises.setAdapter(adapter);
    }

    private void setupChart() {
        if (binding == null || binding.volumeChart == null) return;

        LineChart chart = binding.volumeChart;
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(false);
        chart.setDrawGridBackground(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(0xFFFFFFFF);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(0xFFFFFFFF);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(0x33FFFFFF);

        chart.getAxisRight().setEnabled(false);
    }

    private void updateChart(List<ApiService.WorkoutHistoryResponse> historyList, String routineName) {
        if (historyList == null || routineName == null || binding == null || binding.volumeChart == null) return;

        List<Entry> entries = new ArrayList<>();
        final List<String> dates = new ArrayList<>();

        List<ApiService.WorkoutHistoryResponse> filteredList = new ArrayList<>();
        for (ApiService.WorkoutHistoryResponse h : historyList) {
            if (routineName.equalsIgnoreCase(h.getRoutineName())) {
                filteredList.add(h);
            }
        }

        if (filteredList.isEmpty()) {
            binding.volumeChart.setVisibility(View.GONE);
            return;
        } else {
            binding.volumeChart.setVisibility(View.VISIBLE);
        }

        // Invertimos la lista porque la API devuelve los más recientes primero
        Collections.reverse(filteredList);

        int index = 0;
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());

        for (ApiService.WorkoutHistoryResponse session : filteredList) {
            float volume = (float) session.getTotalVolume();
            entries.add(new Entry(index, volume));

            try {
                // created_at tiene formato ISO 8601 (2023-10-25T14:30:00)
                Date date = inputFormat.parse(session.getCreatedAt().split("T")[0]);
                dates.add(outputFormat.format(date));
            } catch (Exception e) {
                dates.add("");
            }
            index++;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Volumen");
        dataSet.setColor(0xFFFF5722); // Color Naranja KERN
        dataSet.setLineWidth(3f);
        dataSet.setCircleColor(0xFFFF5722);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextColor(0xFFFFFFFF);
        dataSet.setValueTextSize(10f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Curvas suaves

        LineData lineData = new LineData(dataSet);
        binding.volumeChart.setData(lineData);

        binding.volumeChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                if (idx >= 0 && idx < dates.size()) {
                    return dates.get(idx);
                }
                return "";
            }
        });

        binding.volumeChart.invalidate(); // Refrescar el gráfico
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}