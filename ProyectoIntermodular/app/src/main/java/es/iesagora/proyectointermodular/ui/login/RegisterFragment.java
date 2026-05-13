package es.iesagora.proyectointermodular.ui.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import es.iesagora.proyectointermodular.R;
import es.iesagora.proyectointermodular.databinding.FragmentRegisterBinding;
import es.iesagora.proyectointermodular.viewmodel.AuthViewModel;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private AuthViewModel authViewModel;

    public RegisterFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // 1. Inflar la vista usando Data Binding
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 2. Inicializar el ViewModel ligado a este Fragment
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // 3. Vincular el ViewModel al layout
        binding.setViewModel(authViewModel);

        // 4. Vincular el propietario del ciclo de vida para LiveData
        binding.setLifecycleOwner(getViewLifecycleOwner());

        // 5. NUEVO: OBSERVAR SI DEBE IR AL CUESTIONARIO (Tras registro exitoso)
        authViewModel.getNeedsOnboarding().observe(getViewLifecycleOwner(), needsOnboarding -> {
            if (needsOnboarding != null && needsOnboarding) {
                Toast.makeText(getContext(), "¡Cuenta creada! Empecemos con tu perfil", Toast.LENGTH_LONG).show();

                // Navegamos a la pantalla de Bienvenida/Cuestionario
                // Usamos popUpTo para que no pueda volver atrás al formulario de registro
                NavController navController = Navigation.findNavController(view);
                navController.navigate(R.id.action_registerFragment_to_welcomeFragment, null,
                        new NavOptions.Builder()
                                .setPopUpTo(R.id.nav_graph, true)
                                .build());
            }
        });

        // 6. OBSERVAR MENSAJES DE ERROR
        authViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                authViewModel.clearErrorMessage();
            }
        });

        // 7. Configurar la Navegación hacia Login
        binding.tvGoLogin.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_registerFragment_to_loginFragment);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpiar la referencia al binding para evitar memory leaks
        binding = null;
    }
}