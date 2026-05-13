package es.iesagora.proyectointermodular.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;

import java.util.ArrayList;
import java.util.List;

import es.iesagora.proyectointermodular.R;
import es.iesagora.proyectointermodular.data.model.RoutineResponse;
import es.iesagora.proyectointermodular.data.repository.TokenManager;
import es.iesagora.proyectointermodular.databinding.FragmentWorkoutBinding;
import es.iesagora.proyectointermodular.ui.adapter.RutinasAdapter;
import es.iesagora.proyectointermodular.viewmodel.RoutineViewModel;

public class WorkoutFragment extends Fragment {
    private FragmentWorkoutBinding binding;
    // ERROR SOLUCIONADO: Declaramos la variable aquí
    private RoutineViewModel routineViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWorkoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializamos el ViewModel
        routineViewModel = new ViewModelProvider(requireActivity()).get(RoutineViewModel.class);

        // --- PROTECCIÓN DE NAVEGACIÓN (BottomNav) ---
        // Si ya hay un entrenamiento activo, redirigimos automáticamente al fragmento activo
        if (routineViewModel.isSessionActive(requireContext())) {
            Navigation.findNavController(view).navigate(R.id.workoutActiveFragment);
            return; // Detenemos la ejecución de este fragmento
        }

        // 1. Configurar RecyclerView (2 columnas)
        binding.rvMisRutinas.setLayoutManager(new GridLayoutManager(getContext(), 2));

        // 2. Cargar datos al entrar
        refreshData();

        // 3. Observar las rutinas
        routineViewModel.getMyRoutines().observe(getViewLifecycleOwner(), routines -> {
            if (routines != null) {
                List<RoutineResponse> personalizadas = new ArrayList<>();
                RoutineResponse predefinida = null;

                for (RoutineResponse r : routines) {
                    String nombre = r.getName();

                    // --- CAMBIO AQUÍ: Ampliamos el filtro para detectar las rutinas del cuestionario ---
                    if (nombre != null && (
                            nombre.equalsIgnoreCase("Rutina Predeterminada") ||
                                    nombre.contains("Plan Especial") ||
                                    nombre.toLowerCase().contains("inicial") ||
                                    nombre.toLowerCase().contains("full body") ||
                                    nombre.toLowerCase().contains("torso-pierna") ||
                                    nombre.toLowerCase().contains("ppl")
                    )) {
                        predefinida = r;
                    } else {
                        personalizadas.add(r);
                    }
                }

                // LÓGICA DEL EMPTY STATE
                if (personalizadas.isEmpty()) {
                    binding.rvMisRutinas.setVisibility(View.GONE);
                    binding.layoutEmptyState.setVisibility(View.VISIBLE);
                } else {
                    binding.rvMisRutinas.setVisibility(View.VISIBLE);
                    binding.layoutEmptyState.setVisibility(View.GONE);

                    // Declaramos la variable UNA SOLA VEZ aquí
                    RutinasAdapter adapter = new RutinasAdapter(personalizadas, rutina -> {
                        // --- NUEVA VALIDACIÓN: BLOQUEO SI YA HAY SESIÓN ACTIVA ---
                        if (routineViewModel.isSessionActive(requireContext())) {
                            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Entrenamiento en curso")
                                    .setMessage("Ya tienes un entrenamiento activo. Debes terminarlo o cancelarlo antes de empezar uno nuevo.")
                                    .setPositiveButton("Ir al entrenamiento actual", (dialog, which) -> {
                                        Navigation.findNavController(requireView()).navigate(R.id.workoutActiveFragment);
                                    })
                                    .setNegativeButton("Entendido", null)
                                    .show();
                            return;
                        }

                        // 1. Mostrar feedback al usuario
                        Toast.makeText(getContext(), "Cargando historial de " + rutina.getName(), Toast.LENGTH_SHORT).show();

                        // 2. Disparar la carga del detalle (que ya trae los campos prev_kilos y prev_reps)
                        String token = TokenManager.getToken(getContext());
                        routineViewModel.fetchRoutineById(rutina.getId(), token);

                        // 3. Observamos el resultado una sola vez para navegar
                        routineViewModel.getSelectedRoutine().observe(getViewLifecycleOwner(), new Observer<RoutineResponse>() {
                            @Override
                            public void onChanged(RoutineResponse routineWithHistory) {
                                if (routineWithHistory != null && routineWithHistory.getId() == rutina.getId()) {
                                    Bundle bundle = new Bundle();
                                    bundle.putSerializable("routine", routineWithHistory);
                                    Navigation.findNavController(requireView()).navigate(R.id.action_workoutFragment_to_workoutActiveFragment, bundle);

                                    // Importante: eliminamos el observador para evitar navegaciones múltiples
                                    routineViewModel.getSelectedRoutine().removeObserver(this);
                                }
                            }
                        });
                    });
                    binding.rvMisRutinas.setAdapter(adapter);
                }

                // Actualizar contador
                binding.tvMisRutinasCount.setText("MIS RUTINAS (" + personalizadas.size() + ")");

                // Configurar botón Predeterminada
                final RoutineResponse finalPre = predefinida;
                binding.btnRutinaPredeterminada.setOnClickListener(v -> {
                    // --- NUEVA VALIDACIÓN PARA RUTINA PREDETERMINADA ---
                    if (routineViewModel.isSessionActive(requireContext())) {
                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                .setTitle("Entrenamiento en curso")
                                .setMessage("Ya tienes un entrenamiento activo.")
                                .setPositiveButton("Ir al entrenamiento actual", (dialog, which) -> {
                                    Navigation.findNavController(requireView()).navigate(R.id.workoutActiveFragment);
                                })
                                .setNegativeButton("Entendido", null)
                                .show();
                        return;
                    }

                    if (finalPre != null) {
                        Toast.makeText(getContext(), "Cargando " + finalPre.getName() + "...", Toast.LENGTH_SHORT).show();
                        // Nota: Aquí faltaba la lógica para empezar la predeterminada si el usuario hace clic
                        // la añadimos para que sea funcional
                        String token = TokenManager.getToken(getContext());
                        routineViewModel.fetchRoutineById(finalPre.getId(), token);
                        routineViewModel.getSelectedRoutine().observe(getViewLifecycleOwner(), new Observer<RoutineResponse>() {
                            @Override
                            public void onChanged(RoutineResponse routineWithHistory) {
                                if (routineWithHistory != null && routineWithHistory.getId() == finalPre.getId()) {
                                    Bundle bundle = new Bundle();
                                    bundle.putSerializable("routine", routineWithHistory);
                                    Navigation.findNavController(requireView()).navigate(R.id.action_workoutFragment_to_workoutActiveFragment, bundle);
                                    routineViewModel.getSelectedRoutine().removeObserver(this);
                                }
                            }
                        });
                    } else {
                        Toast.makeText(getContext(), "Haz el cuestionario primero", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        binding.btnNuevaRutina.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_workoutFragment_to_crearRutinaFragment);
        });
    }

    private void refreshData() {
        String token = es.iesagora.proyectointermodular.data.repository.TokenManager.getToken(getContext());
        if (token != null) {
            routineViewModel.fetchMyRoutines("Bearer " + token);
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        // Esto se ejecuta SIEMPRE que la pantalla vuelve a estar visible
        refreshData();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}