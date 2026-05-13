package es.iesagora.proyectointermodular.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import es.iesagora.proyectointermodular.R;
import es.iesagora.proyectointermodular.databinding.FragmentCrearRutinaBinding;
import es.iesagora.proyectointermodular.ui.adapter.SelectedExerciseAdapter;
import es.iesagora.proyectointermodular.viewmodel.RoutineViewModel;

public class CrearRutinaFragment extends Fragment {
    private FragmentCrearRutinaBinding binding;
    private RoutineViewModel viewModel;
    private SelectedExerciseAdapter adapter;

    // Lista de palabras reservadas para evitar conflictos con las rutinas predeterminadas
    private final List<String> nombresReservados = Arrays.asList(
            "inicial", "predeterminada", "plan especial", "full body", "torso-pierna", "ppl"
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle bundle) {
        binding = FragmentCrearRutinaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //Conectamos con el ViewModel COMPARTIDO
        // Usamos requireActivity() para que sea el mismo buzón que el del Buscador
        viewModel = new ViewModelProvider(requireActivity()).get(RoutineViewModel.class);

        // --- MODO EDICIÓN: Rellenamos el nombre si ya existe en el ViewModel ---
        if (viewModel.getEditingRoutineId() != -1) {
            binding.etNombreRutina.setText(viewModel.getRoutineName());
            binding.btnCrearRutinaFinal.setText("GUARDAR CAMBIOS");
        } else {
            binding.btnCrearRutinaFinal.setText("CREAR RUTINA");
        }

        //Configuramos el RecyclerView de los ejercicios elegidos
        setupRecyclerView();

        //Observamos los cambios en la lista de ejercicios seleccionados
        viewModel.getSelectedExercises().observe(getViewLifecycleOwner(), exercises -> {
            if (exercises == null || exercises.isEmpty()) {
                // Si no hay ejercicios, mostramos el aviso y ocultamos la lista
                binding.rvEjerciciosSeleccionados.setVisibility(View.GONE);
                binding.layoutEmptyStateEjercicios.setVisibility(View.VISIBLE);
            } else {
                // Si hay ejercicios, mostramos la lista y ocultamos el aviso
                binding.rvEjerciciosSeleccionados.setVisibility(View.VISIBLE);
                binding.layoutEmptyStateEjercicios.setVisibility(View.GONE);
                adapter.setList(exercises);
            }
        });

        //Observamos si la rutina se guardó con éxito en FastAPI
        viewModel.getSaveSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success) {
                Toast.makeText(getContext(), "¡Operación realizada exitosamente!", Toast.LENGTH_SHORT).show();
                //llamamos al metodo del reset que creamos en el RoutineViewModel
                viewModel.ressetSaveStatus();
                // Volvemos atrás o limpiamos la pantalla tras el éxito
                Navigation.findNavController(requireView()).popBackStack();
            }
        });

        //Observamos si hay errores durante el guardado
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });

        //Botón para ir al buscador a elegir más ejercicios
        binding.btnIrABuscador.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_crearRutina_to_buscador);
        });

        //Botón final para guardar la rutina
        binding.btnCrearRutinaFinal.setOnClickListener(v -> {
            guardarRutina();
        });
    }

    private void setupRecyclerView() {
        adapter = new SelectedExerciseAdapter(new ArrayList<>());
        binding.rvEjerciciosSeleccionados.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvEjerciciosSeleccionados.setAdapter(adapter);
    }

    private void guardarRutina() {
        String nombre = binding.etNombreRutina.getText().toString().trim();

        if (nombre.isEmpty()) {
            binding.etNombreRutina.setError("Ponle un nombre a la rutina");
            return;
        }

        // VALIDACIÓN DE NOMBRES RESERVADOS: Evitamos que el usuario use nombres del sistema
        for (String reservado : nombresReservados) {
            if (nombre.toLowerCase().contains(reservado)) {
                binding.etNombreRutina.setError("Nombre reservado por el sistema");
                Toast.makeText(getContext(), "No puedes usar '" + reservado + "' en el nombre", Toast.LENGTH_LONG).show();
                return;
            }
        }

        if (viewModel.getSelectedExercises().getValue() == null ||
                viewModel.getSelectedExercises().getValue().isEmpty()) {
            Toast.makeText(getContext(), "Añade al menos un ejercicio", Toast.LENGTH_SHORT).show();
            return;
        }

        // Guardamos el nombre en el ViewModel
        viewModel.setRoutineName(nombre);

        // --- CAMBIO AQUÍ: Recuperamos los datos usando la "mochila" correcta ---
        SharedPreferences prefs = requireActivity().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);

        // USAMOS TU CLASE TokenManager PARA LEER EL TOKEN
        String token = es.iesagora.proyectointermodular.data.repository.TokenManager.getToken(getContext());

        // Log de seguridad para que veas en el Logcat si ahora sí llega
        android.util.Log.d("DEBUG_TOKEN", "ID: " + userId + " | TOKEN: " + token);

        if (userId != -1 && token != null && !token.isEmpty()) {
            // Llamamos a la API a través del ViewModel
            viewModel.saveAllRoutine(userId, token);
            Toast.makeText(getContext(), "Guardando cambios...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Error: Sesión no válida o expirada.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}