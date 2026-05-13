package es.iesagora.proyectointermodular.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import es.iesagora.proyectointermodular.R;

public class WelcomeFragment extends Fragment {

    public WelcomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Referenciamos el botón por su ID
        Button btnEmpezar = view.findViewById(R.id.btn_empezar);

        // Navegación al pulsar
        btnEmpezar.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_welcomeFragment_to_questionnaireFragment);
        });
    }
}