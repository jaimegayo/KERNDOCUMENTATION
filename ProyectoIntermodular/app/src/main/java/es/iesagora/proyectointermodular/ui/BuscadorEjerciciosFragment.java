package es.iesagora.proyectointermodular.ui;

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
import java.util.List;
import androidx.appcompat.widget.SearchView;
import es.iesagora.proyectointermodular.data.model.Exercise;
import es.iesagora.proyectointermodular.databinding.FragmentBuscadorEjerciciosBinding;
import es.iesagora.proyectointermodular.ui.adapter.ExerciseAdapter;
import es.iesagora.proyectointermodular.viewmodel.ExerciseViewModel;
import es.iesagora.proyectointermodular.viewmodel.RoutineViewModel;

public class BuscadorEjerciciosFragment extends Fragment implements ExerciseAdapter.OnExerciseClickListener {
    private RoutineViewModel routineViewModel;
    private FragmentBuscadorEjerciciosBinding binding;
    private ExerciseViewModel viewModel;
    private ExerciseAdapter adapter;
    private List<Exercise> allExercises = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle bundle) {
        binding = FragmentBuscadorEjerciciosBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // primero inicializamos el viewmodel
        viewModel = new ViewModelProvider(this).get(ExerciseViewModel.class);

        //inicializamos el SharedViewModel usando requireActivity()
        routineViewModel = new ViewModelProvider(requireActivity()).get(RoutineViewModel.class);

        //luego configuraremos el recyclerview
        setupRecyclerView();

        //observamos los datos del viewmodel, livedata
        viewModel.getExercises().observe(getViewLifecycleOwner(), exercises -> {
            allExercises = exercises; //guardamos la lista completa para filtrar luego
            adapter.setList(exercises); //pasamos la lista al adaptador para que la pinte
            generateMuscleChips(exercises);
        });

        //aqui cargaremos los ejercicios por defecto, es decir, los del gym
        viewModel.loadExercises("exercises_gym_id.json");

        //y por ultimo, configuraremos los eventos de los chips, cambiar entre gimnasio y casa
        setupFilters();
        //configurar el buscador por teclado
        setupSearchView();
    }
    private void setupRecyclerView() {
        adapter = new ExerciseAdapter(new ArrayList<>(), this);
        binding.rvResultadosBusqueda.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvResultadosBusqueda.setAdapter(adapter);
    }

    private void setupFilters() {
        //al pulsar el chip del gimnasio
        binding.chipGym.setOnClickListener(v -> {
            viewModel.loadExercises("exercises_gym_id.json");
        });

        //al pulsar el chip de casa
        binding.chipCasa.setOnClickListener(v -> {
            viewModel.loadExercises("exercises_home_id.json");
        });

        // AQUÍ MÁS ADELANTE: Lógica del SearchView y filtros por músculo
    }

    private void generateMuscleChips(List<Exercise> exercises) {
        //limpiamos los chips de musculos anteriores (para que no se dupliquen al cambiar Gym/Casa)
        //peero, no borramos los dos primeros (Gym y Casa)
        int childCount = binding.chipGroupFiltros.getChildCount();
        if (childCount > 2) {
            binding.chipGroupFiltros.removeViews(2, childCount - 2);
        }

        //sacamos una lista de musculos sin repetirlos
        List<String> muscles = new ArrayList<>();
        for (Exercise res : exercises) {
            if (!muscles.contains(res.getPrimaryMuscle())) {
                muscles.add(res.getPrimaryMuscle());
            }
        }

        //por cada musculo que hayamos encontrado, creamos un chip visualmente
        for (String muscle : muscles) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(getContext());
            chip.setText(muscle);
            chip.setCheckable(true);
            chip.setClickable(true);

            // Estilo -> para que se vea igual que los otros (podriamos omitir el estilo si diera error)
            chip.setChipBackgroundColorResource(android.R.color.darker_gray);

            //logica: que pasa si pulso el chip de un musculo?
            chip.setOnClickListener(v -> {
                filterListByMuscle(muscle);
            });

            binding.chipGroupFiltros.addView(chip);
        }
    }

    private void filterListByMuscle(String muscle) {
        List<Exercise> filteredList = new ArrayList<>();
        for (Exercise ex : allExercises) {
            if (ex.getPrimaryMuscle().equalsIgnoreCase(muscle)) {
                filteredList.add(ex);
            }
        }
        adapter.setList(filteredList);
    }

    //-- METODOS PARA EL BUSCADOR
    private void setupSearchView() {
        binding.searchViewEjercicios.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Cada vez que escribes una letra, filtramos la lista 'allExercises'
                filterList(newText);
                return true;
            }
        });
    }
    private void filterList(String text) {
        List<Exercise> filteredList = new ArrayList<>();
        for (Exercise exercise : allExercises) {
            // comparamos el nombre en minusculas para que no falle por las mayusculas
            if (exercise.getName().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(exercise);
            }
        }
        // actualizamos el recyclerview con los resultados
        adapter.setList(filteredList);
    }

    @Override
    public void onAddClick(Exercise exercise) {
        //añadiremos el ejercicio al "buzón" compartido, el sharedVM
        routineViewModel.addExercise(exercise);

        //mostramos la confirmación
        Toast.makeText(getContext(), exercise.getName() + " añadido a la rutina", Toast.LENGTH_SHORT).show();
        //volvemos de manera automarica a la pantalla de crear rutina
        Navigation.findNavController(requireView()).navigateUp();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // con esto evitamos fugas de memoria (comun de ViewBinding en Fragments)
    }
}
