package es.iesagora.proyectointermodular.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.app.Application;
import es.iesagora.proyectointermodular.data.repository.UserRepository;
import es.iesagora.proyectointermodular.data.model.User;

public class AuthViewModel extends AndroidViewModel {//cambiamos viewmodel por Androidviewmodel

    // --- 1. PROPIEDADES DE ENTRADA ---
    public MutableLiveData<String> email = new MutableLiveData<>("");
    public MutableLiveData<String> password = new MutableLiveData<>("");
    public MutableLiveData<String> name = new MutableLiveData<>("");
    public MutableLiveData<String> surname = new MutableLiveData<>("");
    public MutableLiveData<String> phone = new MutableLiveData<>("");
    public MutableLiveData<String> confirmPassword = new MutableLiveData<>("");

    // --- 2. PROPIEDADES DE SALIDA ---
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<Boolean> registerSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> needsOnboarding = new MutableLiveData<>();

    // NUEVO: Estado de carga para feedback visual
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // NUEVO: Guardamos el objeto User para que el Fragment pueda obtener el Token
    private final MutableLiveData<User> authenticatedUser = new MutableLiveData<>();

    // --- 3. REPOSITORIO ---
    private final UserRepository repository;

    public AuthViewModel(@NonNull android.app.Application application) {
        super(application);
        this.repository = new UserRepository();
    }

    // --- 4. GETTERS ---
    public MutableLiveData<Boolean> getLoginSuccess() { return loginSuccess; }
    public MutableLiveData<Boolean> getRegisterSuccess() { return registerSuccess; }
    public MutableLiveData<String> getErrorMessage() { return errorMessage; }
    public MutableLiveData<Boolean> getNeedsOnboarding() { return needsOnboarding; }
    public MutableLiveData<User> getAuthenticatedUser() { return authenticatedUser; }
    public MutableLiveData<Boolean> getIsLoading() { return isLoading; }

    public void clearErrorMessage() { errorMessage.setValue(null); }

    // --- 5. LOGIN ---
    public void login() {
        String emailValue = email.getValue();
        String passwordValue = password.getValue();

        if (emailValue == null || emailValue.trim().isEmpty() || passwordValue == null || passwordValue.isEmpty()) {
            errorMessage.setValue("El usuario y la contraseña son obligatorios.");
            return;
        }

        isLoading.setValue(true);
        repository.performLogin(getApplication(), emailValue, passwordValue, new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                isLoading.postValue(false);
                if (user != null) {
                    // PASO CLAVE: Guardamos el usuario (con su token) antes de avisar del éxito
                    authenticatedUser.postValue(user);

                    if (!user.isHasCompletedQuiz()) {
                        needsOnboarding.postValue(true);
                    } else {
                        loginSuccess.postValue(true);
                    }
                }
            }

            @Override
            public void onFailure(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
                loginSuccess.postValue(false);
            }
        });
    }

    // --- 6. REGISTRO ---
    public void register() {
        String nameValue = name.getValue();
        String surnameValue = surname.getValue();
        String emailValue = email.getValue();
        String phoneValue = phone.getValue();
        String passwordValue = password.getValue();
        String confirmPasswordValue = confirmPassword.getValue();

        if (nameValue == null || nameValue.trim().isEmpty() || emailValue == null || emailValue.trim().isEmpty() || passwordValue == null || passwordValue.isEmpty()) {
            errorMessage.setValue("Todos los campos son obligatorios.");
            return;
        }

        if (!passwordValue.equals(confirmPasswordValue)) {
            errorMessage.setValue("Las contraseñas no coinciden.");
            return;
        }

        isLoading.setValue(true);
        repository.performRegister(getApplication(), emailValue, passwordValue, nameValue, surnameValue, phoneValue, new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                isLoading.postValue(false);
                if (user != null) {
                    // PASO CLAVE: Guardamos el usuario aquí también
                    authenticatedUser.postValue(user);
                    needsOnboarding.postValue(true);
                }
            }

            @Override
            public void onFailure(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
                registerSuccess.postValue(false);
            }
        });
    }
}