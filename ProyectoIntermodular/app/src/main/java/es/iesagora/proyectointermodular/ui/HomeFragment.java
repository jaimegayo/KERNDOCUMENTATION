package es.iesagora.proyectointermodular.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import es.iesagora.proyectointermodular.R;
import es.iesagora.proyectointermodular.data.model.User;
import es.iesagora.proyectointermodular.viewmodel.HomeViewModel;
import es.iesagora.proyectointermodular.viewmodel.RoutineViewModel;
import es.iesagora.proyectointermodular.data.repository.TokenManager;

public class HomeFragment extends Fragment {
    private HomeViewModel homeViewModel;
    private RoutineViewModel routineViewModel; // Miembro para compartir entre métodos
    private TextView tvWelcome, tvRoutineName;
    private View cardResume; // Ahora es miembro para acceder desde los observadores
    private es.iesagora.proyectointermodular.ui.adapter.HistoryAdapter historyAdapter;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        // Vinculamos los componentes del fragment_home.xml
        tvWelcome = root.findViewById(R.id.tv_welcome);
        tvRoutineName = root.findViewById(R.id.tv_routine_name);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        // 1. INICIALIZACIÓN CRÍTICA: Primero los ViewModels
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        routineViewModel = new ViewModelProvider(requireActivity()).get(RoutineViewModel.class);

        // 2. LÓGICA DE PERSISTENCIA INMEDIATA (Banner de recuperación)
        // Intentamos cargar esto lo primero usando el ID que ya esté en caché
        cardResume = view.findViewById(R.id.card_resume_workout);
        Button btnResume = view.findViewById(R.id.btn_resume_workout);
        Button btnStartRoutine = view.findViewById(R.id.btn_start_routine_home);

        if (routineViewModel.isSessionActive(requireContext())) {
            if (cardResume != null) cardResume.setVisibility(View.VISIBLE);
            if (btnResume != null) {
                btnResume.setOnClickListener(v -> {
                    Navigation.findNavController(view).navigate(R.id.workoutActiveFragment);
                });
            }
        } else {
            if (cardResume != null) cardResume.setVisibility(View.GONE);
        }

        // 3. Configuramos los observadores (ya pueden usar los ViewModels)
        setupObservers();

        // 4. Pedimos los datos del perfil a la API
        homeViewModel.fetchUserProfile();

        // Lógica para el botón "EMPEZAR RUTINA"
        if (btnStartRoutine != null) {
            btnStartRoutine.setOnClickListener(v -> {
                if (routineViewModel.isSessionActive(requireContext())) {
                    mostrarDialogoSesionActiva();
                } else {
                    Navigation.findNavController(view).navigate(R.id.workoutFragment);
                }
            });
        }

        // --- SPRINT 3: CARRUSEL DE HISTORIAL ---
        androidx.recyclerview.widget.RecyclerView rvHistory = view.findViewById(R.id.rv_history_carousel);
        if (rvHistory != null) {
            historyAdapter = new es.iesagora.proyectointermodular.ui.adapter.HistoryAdapter(new java.util.ArrayList<>(), session -> {
                WorkoutDetailBottomSheet bottomSheet = WorkoutDetailBottomSheet.newInstance(session);
                bottomSheet.show(getParentFragmentManager(), "WorkoutDetailBottomSheet");
            });
            rvHistory.setAdapter(historyAdapter);

            String token = TokenManager.getToken(getContext());
            if (token != null) {
                routineViewModel.fetchWorkoutHistory(token);
            }
        }
    }

    private void mostrarDialogoSesionActiva() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Entrenamiento en curso")
                .setMessage("Ya tienes un entrenamiento activo. Debes terminarlo o cancelarlo antes de empezar uno nuevo.")
                .setPositiveButton("Ir al entrenamiento actual", (dialog, which) -> {
                    Navigation.findNavController(requireView()).navigate(R.id.workoutActiveFragment);
                })
                .setNegativeButton("Entendido", null)
                .show();
    }

    private void setupObservers(){
        // Observamos cuando llega el usuario
        homeViewModel.getUserLiveData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                // Actualizamos los textos con los datos reales de la BD
                if (user.getFullName() != null){
                    tvWelcome.setText("Bienvenido de nuevo, " + user.getFullName());
                } else if (user.getUsername() != null) {
                    tvWelcome.setText("Bienvenido de nuevo, " + user.getUsername());
                }

                if (user.getAssignedRoutine() != null){
                    tvRoutineName.setText(user.getAssignedRoutine());
                }

                // --- RE-COMPROBACIÓN DE SESIÓN ACTIVA TRAS TENER EL USER ID ---
                if (routineViewModel.isSessionActive(requireContext())) {
                    if (cardResume != null) cardResume.setVisibility(View.VISIBLE);
                } else {
                    if (cardResume != null) cardResume.setVisibility(View.GONE);
                }
            }
        });

        // Observamos si hay algún error
        homeViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null){
                if (errorMessage.equals("SESSION_EXPIRED")) {
                    realizarLogOutLimpiandoTodo();
                    Toast.makeText(getContext(), "Sesión caducada. Inicia sesión de nuevo.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Observar el historial y actualizar el carrusel
        routineViewModel.getWorkoutHistory().observe(getViewLifecycleOwner(), history -> {
            if (historyAdapter != null && history != null) {
                // Hacemos una copia para no modificar la original del ViewModel
                java.util.List<es.iesagora.proyectointermodular.data.remote.ApiService.WorkoutHistoryResponse> sortedHistory = new java.util.ArrayList<>(history);
                // Ordenar del más reciente al más antiguo por si el backend no lo hace
                java.util.Collections.sort(sortedHistory, (h1, h2) -> {
                    if (h1.getCreatedAt() == null || h2.getCreatedAt() == null) return 0;
                    return h2.getCreatedAt().compareTo(h1.getCreatedAt());
                });
                historyAdapter.setHistoryList(sortedHistory);
            }
        });
    }

    private void realizarLogOutLimpiandoTodo() {
        // --- CORRECCIÓN: NO BORRAMOS EL ESTADO DEL ENTRENAMIENTO AL SALIR ---
        // El estado debe persistir en SharedPreferences con su clave _user_ID.
        // Solo limpiamos los datos en memoria del ViewModel para el siguiente usuario.
        if (routineViewModel != null) {
            routineViewModel.clearAllData();
        }

        // 1. Detener servicio de pasos
        try {
            Intent intent = new Intent(getContext(), es.iesagora.proyectointermodular.services.StepCounterService.class);
            requireContext().stopService(intent);
        } catch (Exception e) {}

        // 2. Borrar Token y userId selectivamente para cerrar la sesión de red
        TokenManager.clearToken(requireContext());
        SharedPreferences prefs = requireActivity().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        prefs.edit()
                .remove("userId")
                .remove("accessToken")
                .apply();

        // 3. Navegar al Login limpiando la pila
        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build();

        if (getView() != null) {
            Navigation.findNavController(getView()).navigate(R.id.loginFragment, null, navOptions);
        }
    }
}