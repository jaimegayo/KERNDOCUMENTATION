package es.iesagora.proyectointermodular.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.card.MaterialCardView;
import es.iesagora.proyectointermodular.R;
import es.iesagora.proyectointermodular.data.model.User;
import es.iesagora.proyectointermodular.data.repository.UserRepository;

public class QuestionnaireFragment extends Fragment {

    // 1. Variables para guardar lo que el usuario elija
    private String selectedLevel = "";
    private int selectedDays = 0;
    private boolean isFirstQuestion = true; // Para saber si estamos en "Nivel" o en "Días"

    // 2. Elementos de la pantalla que vamos a cambiar
    private TextView tvQuestionTitle;
    private TextView tvTitle1, tvSubtitle1, tvTitle2, tvSubtitle2, tvTitle3, tvSubtitle3;
    private MaterialCardView card1, card2, card3;

    public QuestionnaireFragment() {
        super(R.layout.fragment_questionnaire);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Vincular los textos con el XML (usando los IDs que te pasé)
        tvQuestionTitle = view.findViewById(R.id.tv_question_title);

        card1 = view.findViewById(R.id.card_option_1);
        tvTitle1 = view.findViewById(R.id.tv_option_title_1);
        tvSubtitle1 = view.findViewById(R.id.tv_option_subtitle_1);

        card2 = view.findViewById(R.id.card_option_2);
        tvTitle2 = view.findViewById(R.id.tv_option_title_2);
        tvSubtitle2 = view.findViewById(R.id.tv_option_subtitle_2);

        card3 = view.findViewById(R.id.card_option_3);
        tvTitle3 = view.findViewById(R.id.tv_option_title_3);
        tvSubtitle3 = view.findViewById(R.id.tv_option_subtitle_3);

        // 3. Configurar qué pasa al tocar cada tarjeta
        card1.setOnClickListener(v -> markSelection(1));
        card2.setOnClickListener(v -> markSelection(2));
        card3.setOnClickListener(v -> markSelection(3));

        // 4. Configurar el botón SIGUIENTE
        view.findViewById(R.id.btn_next).setOnClickListener(v -> {
            if (isFirstQuestion) {
                if (!selectedLevel.isEmpty()) {
                    setupSecondQuestion(); // PASAMOS A LA PREGUNTA DE DÍAS
                } else {
                    Toast.makeText(getContext(), "Selecciona tu nivel primero", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (selectedDays > 0) {
                    finishQuestionnaire(v); // CALCULAMOS Y GUARDAMOS EN API
                } else {
                    Toast.makeText(getContext(), "Dinos cuántos días entrenarás", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void markSelection(int option) {
        // Quitamos el borde a todas
        card1.setStrokeWidth(0);
        card2.setStrokeWidth(0);
        card3.setStrokeWidth(0);

        // Ponemos borde a la elegida y guardamos el dato
        if (option == 1) {
            card1.setStrokeWidth(8);
            if (isFirstQuestion) selectedLevel = "BEGINNER"; else selectedDays = 3;
        } else if (option == 2) {
            card2.setStrokeWidth(8);
            if (isFirstQuestion) selectedLevel = "INTERMEDIATE"; else selectedDays = 4;
        } else if (option == 3) {
            card3.setStrokeWidth(8);
            if (isFirstQuestion) selectedLevel = "ADVANCED"; else selectedDays = 5;
        }
    }

    private void setupSecondQuestion() {
        isFirstQuestion = false; // Ya no es la primera pregunta

        // Limpiar bordes de la selección anterior
        card1.setStrokeWidth(0);
        card2.setStrokeWidth(0);
        card3.setStrokeWidth(0);

        // CAMBIAMOS LOS TEXTOS DINÁMICAMENTE
        tvQuestionTitle.setText("¿CUÁNTOS DÍAS ENTRENARÁS?");

        tvTitle1.setText("2 - 3 DÍAS");
        tvSubtitle1.setText("POCO TIEMPO / MANTENIMIENTO");

        tvTitle2.setText("4 DÍAS");
        tvSubtitle2.setText("IDEAL PARA PROGRESAR");

        tvTitle3.setText("5+ DÍAS");
        tvSubtitle3.setText("MÁXIMO RENDIMIENTO");
    }

    private void finishQuestionnaire(View view) {
        // Lógica científica para asignar la rutina
        String routine;
        if (selectedLevel.equals("BEGINNER") || selectedDays == 3) {
            routine = "Full Body";
        } else if (selectedLevel.equals("INTERMEDIATE") && selectedDays == 4) {
            routine = "Torso-Pierna";
        } else {
            routine = "PPL (Push Pull Leg)";
        }

        // TODO: Aquí llamaremos al repositorio.
        UserRepository userRepository = new UserRepository();
        userRepository.completeQuiz(getContext(), routine, new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                if (isAdded() && getView() != null) {
                    Toast.makeText(getContext(), "¡Rutina " + routine + " guardada Correctamente!", Toast.LENGTH_LONG).show();
                    Navigation.findNavController(getView()).navigate(R.id.homeFragment);
                }
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), "Error al Guardar: " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}