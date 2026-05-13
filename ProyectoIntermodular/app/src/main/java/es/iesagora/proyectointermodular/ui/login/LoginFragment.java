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
import es.iesagora.proyectointermodular.data.model.User;
import es.iesagora.proyectointermodular.data.repository.TokenManager; // Asegúrate de que la ruta sea correcta
import es.iesagora.proyectointermodular.databinding.FragmentLoginBinding;
import es.iesagora.proyectointermodular.viewmodel.AuthViewModel;

public class LoginFragment extends Fragment {

    private AuthViewModel authViewModel;
    private FragmentLoginBinding binding;

    public LoginFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- 1. NUEVO: AUTO-LOGIN (Persistencia de Sesión) ---
        String savedToken = TokenManager.getToken(getContext());
        if (savedToken != null) {
            // Si el token ya existe en SharedPreferences, redirigimos al Home directamente
            navigateToDestination(view, R.id.homeFragment);
            return;
        }

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.setViewModel(authViewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        // 5. OBSERVAR RESULTADOS DE LOGIN (Para usuarios antiguos -> Van a Home)
        authViewModel.getLoginSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                // --- NUEVO: GUARDAR TOKEN ANTES DE NAVEGAR ---
                saveTokenToMochila();

                Toast.makeText(getContext(), "¡Bienvenido de nuevo!", Toast.LENGTH_SHORT).show();
                navigateToDestination(view, R.id.homeFragment);
            }
        });

        // 6. NUEVO: OBSERVAR SI NECESITA CUESTIONARIO (Para usuarios nuevos -> Van a Welcome)
        authViewModel.getNeedsOnboarding().observe(getViewLifecycleOwner(), needsOnboarding -> {
            if (needsOnboarding != null && needsOnboarding) {
                // --- NUEVO: GUARDAR TOKEN ANTES DE NAVEGAR ---
                saveTokenToMochila();

                Toast.makeText(getContext(), "¡Hola! Vamos a configurar tu perfil", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_welcomeFragment);
            }
        });

        // 7. OBSERVAR MENSAJES DE ERROR
        authViewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                authViewModel.clearErrorMessage();
            }
        });

        // 8. Configurar la Navegación a Registro
        binding.tvGoRegister.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_loginFragment_to_registerFragment);
        });
    }

    /**
     * METODO AUXILIAR: Coge el usuario del ViewModel y guarda el token en TokenManager
     */
    private void saveTokenToMochila() {
        User user = authViewModel.getAuthenticatedUser().getValue();
        if (user != null && user.getToken() != null) {
            TokenManager.saveToken(getContext(), user.getToken());
        }
    }

    private void navigateToDestination(View view, int destinationId) {
        NavController navController = Navigation.findNavController(view);
        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build();
        navController.navigate(destinationId, null, navOptions);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}