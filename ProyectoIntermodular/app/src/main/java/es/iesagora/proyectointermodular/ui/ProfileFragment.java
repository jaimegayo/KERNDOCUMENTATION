package es.iesagora.proyectointermodular.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.ForegroundColorSpan;
import android.text.style.LineBackgroundSpan;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.net.Uri;

import es.iesagora.proyectointermodular.BuildConfig;
import es.iesagora.proyectointermodular.R;
import es.iesagora.proyectointermodular.databinding.FragmentProfileBinding;
import es.iesagora.proyectointermodular.viewmodel.HomeViewModel;
import es.iesagora.proyectointermodular.viewmodel.RoutineViewModel;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private HomeViewModel homeViewModel;
    private RoutineViewModel routineViewModel; // Añadimos este para limpiar las rutinas

    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);

        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // IMPORTANTE: Accedemos al ViewModel de rutinas usando requireActivity()
        // para limpiar el que comparten los demás fragmentos.
        routineViewModel = new ViewModelProvider(requireActivity()).get(RoutineViewModel.class);

        // Configuración de Cloudinary usando BuildConfig
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", BuildConfig.CLOUDINARY_CLOUD_NAME);
            MediaManager.init(requireContext().getApplicationContext(), config);
        } catch (Exception e) {
            // Ya está inicializado, ignoramos
        }

        // Configuración del Launcher para la galería
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            uploadImageToCloudinary(imageUri);
                        }
                    }
                }
        );

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // CONTROL DE LA SESSION_EXPIRED (401)
        homeViewModel.getErrorLiveData().observe(getViewLifecycleOwner(), error -> {
            if (error != null && error.equals("SESSION_EXPIRED")){
                realizarLogOutLimpiandoTodo();
                Toast.makeText(getContext(), "Sesión caducada. Inicia sesión de nuevo.", Toast.LENGTH_LONG).show();
            }
        });

        // LOGICA DEL BOTON DEL LOG-OUT
        binding.btnLogout.setOnClickListener(v -> {
            realizarLogOutLimpiandoTodo();
            Toast.makeText(getContext(), "Has cerrado sesión", Toast.LENGTH_SHORT).show();
        });

        // --- NUEVO: CARGAR DATOS DEL USUARIO ---
        homeViewModel.fetchUserProfile();
        homeViewModel.fetchUserStats();

        // Cargar historial para que esté listo al hacer click en el calendario
        String token = es.iesagora.proyectointermodular.data.repository.TokenManager.getToken(getContext());
        if (token != null) {
            routineViewModel.fetchWorkoutHistory(token);
        }

        // Observar los datos del usuario para el Avatar
        homeViewModel.getUserLiveData().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                String avatarUrl = user.getAvatarUrl();
                if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
                    // Fallback a DiceBear
                    avatarUrl = "https://api.dicebear.com/7.x/avataaars/png?seed=" + user.getUsername();
                }

                if (binding != null && binding.ivProfileImage != null) {
                    Glide.with(this)
                            .load(avatarUrl)
                            .apply(RequestOptions.circleCropTransform())
                            .into(binding.ivProfileImage);
                }

                if (binding != null && binding.tvProfileUsername != null) {
                    binding.tvProfileUsername.setText(user.getUsername());
                }
            }
        });

        if (binding.btnEditUsername != null) {
            binding.btnEditUsername.setOnClickListener(v -> showEditUsernameDialog());
        }

        // Configurar clic en el avatar para abrir la galería
        binding.cvAvatarContainer.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        // Configuración estética del calendario por código (más seguro que XML)
        setupCalendarUI();

        // Observar las estadísticas y actualizar la interfaz
        homeViewModel.getStatsLiveData().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) {
                // Seteamos el total de entrenamientos
                binding.tvStatEntrenos.setText(String.valueOf(stats.getTotalWorkouts()));

                // Seteamos el total de pasos formateado (ej: 12.5k)
                binding.tvStatPasos.setText(formatSteps(stats.getTotalSteps()));

                // --- NUEVO: PINTAR DÍAS DE ENTRENAMIENTO EN EL CALENDARIO ---
                updateCalendar(stats.getTrainingDays());
            }
        });

        // --- NUEVO: ACCIÓN AL PULSAR UN DÍA EN EL CALENDARIO ---
        if (binding.calendarView != null) {
            binding.calendarView.setOnDateChangedListener((widget, date, selected) -> {
                if (selected) {
                    // Formateamos la fecha a yyyy-MM-dd
                    String clickedDate = String.format(java.util.Locale.getDefault(), "%04d-%02d-%02d", date.getYear(), date.getMonth(), date.getDay());

                    List<es.iesagora.proyectointermodular.data.remote.ApiService.WorkoutHistoryResponse> historyList = routineViewModel.getWorkoutHistory().getValue();
                    if (historyList != null) {
                        for (es.iesagora.proyectointermodular.data.remote.ApiService.WorkoutHistoryResponse session : historyList) {
                            if (session.getCreatedAt() != null && session.getCreatedAt().startsWith(clickedDate)) {
                                WorkoutDetailBottomSheet bottomSheet = WorkoutDetailBottomSheet.newInstance(session);
                                bottomSheet.show(getChildFragmentManager(), "WorkoutDetailBottomSheet");
                                break; // Mostrar solo la primera sesión de ese día (o podríamos listarlas)
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * Sube la imagen a Cloudinary asíncronamente y actualiza el backend.
     */
    private void uploadImageToCloudinary(Uri imageUri) {
        Toast.makeText(getContext(), "Subiendo imagen...", Toast.LENGTH_SHORT).show();

        MediaManager.get().upload(imageUri)
                .unsigned("preset_pfg")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) { }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) { }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String secureUrl = (String) resultData.get("secure_url");
                        if (secureUrl != null && isAdded()) {
                            // Actualizar el perfil en el Backend
                            homeViewModel.updateAvatar(secureUrl);
                            Toast.makeText(getContext(), "¡Foto de perfil actualizada!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        if (isAdded()) {
                            android.util.Log.e("CLOUDINARY", "Error: " + error.getDescription());
                            Toast.makeText(getContext(), "Error al subir imagen", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) { }
                }).dispatch();
    }

    private void showEditUsernameDialog() {
        android.widget.EditText input = new android.widget.EditText(getContext());
        input.setHint("Nuevo nombre de usuario");
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Editar Usuario")
                .setView(input)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        homeViewModel.updateUsername(newName);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Configura los colores y estilos del calendario para que encajen con el modo oscuro.
     * Hacemos esto por código para evitar errores de vinculación de recursos (Attribute not found).
     */
    private void setupCalendarUI() {
        if (binding.calendarView == null) return;

        // 1. Estilos de texto (Seguros: vienen de themes.xml)
        binding.calendarView.setHeaderTextAppearance(R.style.CalendarHeaderStyle);
        binding.calendarView.setDateTextAppearance(R.style.CalendarDateStyle);

        // 2. Configuración básica (Permitimos selección simple para el BottomSheet)
        binding.calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_SINGLE);
        binding.calendarView.setShowOtherDates(MaterialCalendarView.SHOW_ALL);
    }

    /**
     * Procesa la lista de fechas del servidor y las marca en el calendario.
     */
    private void updateCalendar(List<String> trainingDays) {
        if (trainingDays == null || binding.calendarView == null) return;

        HashSet<CalendarDay> dates = new HashSet<>();
        for (String dateStr : trainingDays) {
            try {
                // La API envía "YYYY-MM-DD"
                String[] parts = dateStr.split("-");
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);

                // CalendarDay.from usa meses de 1 a 12
                dates.add(CalendarDay.from(year, month, day));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Limpiamos marcas previas para evitar duplicados al recargar
        binding.calendarView.removeDecorators();

        // Aplicamos el decorador naranja (#FF5722)
        binding.calendarView.addDecorator(new EventDecorator(0xFFFF5722, dates));
    }

    /**
     * Decorador personalizado para añadir un punto debajo del número del día.
     */
    private class EventDecorator implements DayViewDecorator {
        private final int color;
        private final HashSet<CalendarDay> dates;

        public EventDecorator(int color, HashSet<CalendarDay> dates) {
            this.color = color;
            this.dates = dates;
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return dates.contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            // Aplicamos el círculo de fondo usando el color configurado
            view.addSpan(new CircleBackgroundSpan(color));
            // Cambiamos el color del texto a blanco para legibilidad
            view.addSpan(new ForegroundColorSpan(Color.WHITE));
        }
    }

    /**
     * Clase personalizada para dibujar un círculo sólido detrás del número del día.
     */
    public static class CircleBackgroundSpan implements LineBackgroundSpan {
        private final int color;

        public CircleBackgroundSpan(int color) {
            this.color = color;
        }

        @Override
        public void drawBackground(Canvas canvas, Paint paint, int left, int right, int top, int baseline, int bottom,
                                   CharSequence text, int start, int end, int lnum) {
            int oldColor = paint.getColor();
            paint.setColor(color);

            // Calculamos el centro
            float centerX = (left + right) / 2f;
            float centerY = (top + bottom) / 2f;

            // El radio al 90% para que no choque con los bordes de la celda
            float radius = (Math.min(Math.abs(right - left), Math.abs(bottom - top)) / 2f) * 0.9f;

            canvas.drawCircle(centerX, centerY, radius, paint);

            paint.setColor(oldColor);
        }
    }

    /**
     * Formatea los pasos para que se vean bien en el diseño (ej: 10.5k si es > 1000)
     */
    private String formatSteps(int steps) {
        if (steps >= 1000) {
            // Dividimos por 1000 y sacamos un decimal
            double kSteps = steps / 1000.0;
            return String.format(java.util.Locale.US, "%.1fk", kSteps);
        }
        return String.valueOf(steps);
    }

    /**
     * Método centralizado para no dejar rastro del usuario anterior
     */
    private void realizarLogOutLimpiandoTodo() {
        // 1. Limpiamos solo los datos en memoria para el siguiente usuario
        if (routineViewModel != null) {
            routineViewModel.clearAllData();
        }

        // 2. Detener el servicio de pasos para que no siga contando para el siguiente usuario
        try {
            Intent intent = new Intent(getContext(), es.iesagora.proyectointermodular.services.StepCounterService.class);
            requireContext().stopService(intent);
        } catch (Exception e) {
            // El servicio podría no estar corriendo
        }

        // 3. Borrar Token y SharedPreferences de forma selectiva (SIN usar clear())
        es.iesagora.proyectointermodular.data.repository.TokenManager.clearToken(requireContext());
        SharedPreferences prefs = requireActivity().getSharedPreferences("prefs", Context.MODE_PRIVATE);
        prefs.edit()
                .remove("userId")
                .remove("accessToken")
                .apply();

        // 4. Navegar al Login limpiando la pila
        NavOptions navOptions = new NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build();

        Navigation.findNavController(requireView()).navigate(R.id.loginFragment, null, navOptions);
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        binding = null;
    }
}