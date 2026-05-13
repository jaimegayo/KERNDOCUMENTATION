package es.iesagora.proyectointermodular.ui;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeModule;

import es.iesagora.proyectointermodular.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Iconify.with(new FontAwesomeModule());
        setContentView(R.layout.activity_main);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

            // Buscamos el nuevo contenedor que envuelve la barra y el logo
            View navigationWrapper = findViewById(R.id.navigation_wrapper);

            NavigationUI.setupWithNavController(bottomNav, navController);

            //Con esto quitamos el tinte por defecto para que el selector del xml (nav_item_color_selector) mande
            bottomNav.setItemIconTintList(getResources().getColorStateList(R.color.nav_item_color_selector, getTheme()));

            // --- LÓGICA PARA OCULTAR LA NAVBAR ---
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int id = destination.getId();

                if (id == R.id.loginFragment ||
                        id == R.id.registerFragment ||
                        id == R.id.welcomeFragment ||
                        id == R.id.questionnaireFragment) {

                    // Ocultamos el wrapper entero (incluye la barra y el logo central)
                    navigationWrapper.setVisibility(View.GONE);
                } else {
                    // Mostramos el wrapper entero
                    navigationWrapper.setVisibility(View.VISIBLE);
                }

                // --- NUEVO: Sincronización del BottomNav para el entrenamiento activo ---
                // Como workoutActiveFragment no está en el menú, forzamos la iluminación del icono de "Entrenar"
                if (id == R.id.workoutActiveFragment) {
                    bottomNav.getMenu().findItem(R.id.workoutFragment).setChecked(true);
                }
            });
        }
    }
}