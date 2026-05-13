package es.iesagora.proyectointermodular.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import es.iesagora.proyectointermodular.data.model.User;
import es.iesagora.proyectointermodular.data.remote.ApiService;
import es.iesagora.proyectointermodular.data.repository.UserRepository;

//Aqui va la logica para manejar el HomeFragment con los datos
public class HomeViewModel extends AndroidViewModel {
    private final UserRepository repository;
    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<ApiService.UserStats> statsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public HomeViewModel(@NonNull Application application){
        super(application);
        //inicializamos el repository
        this.repository = new UserRepository();
    }

    //exponer los datos para que el fragment los observe
    public LiveData<User> getUserLiveData(){
        return userLiveData;
    }
    public LiveData<ApiService.UserStats> getStatsLiveData(){
        return statsLiveData;
    }
    public LiveData<String> getErrorLiveData(){
        return errorLiveData;
    }

    public void fetchUserStats() {
        repository.getUserStats(getApplication().getApplicationContext(), new UserRepository.StatsCallback() {
            @Override
            public void onSuccess(ApiService.UserStats stats) {
                statsLiveData.postValue(stats);
            }

            @Override
            public void onFailure(String message) {
                errorLiveData.postValue(message);
            }
        });
    }

    //Metodo para pedir/buscar los datos al repository
    public void fetchUserProfile(){
        repository.getUserProfile(getApplication().getApplicationContext(), new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                userLiveData.postValue(user);
            }

            @Override
            public void onFailure(String message) {
                errorLiveData.postValue(message);
            }
        });
    }

    public void updateAvatar(String avatarUrl) {
        repository.updateUserAvatar(getApplication().getApplicationContext(), avatarUrl, new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                userLiveData.postValue(user);
            }

            @Override
            public void onFailure(String message) {
                errorLiveData.postValue(message);
            }
        });
    }

    public void updateUsername(String newUsername) {
        repository.updateUsername(getApplication().getApplicationContext(), newUsername, new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                userLiveData.postValue(user);
            }

            @Override
            public void onFailure(String message) {
                errorLiveData.postValue(message);
            }
        });
    }

    //MutableLiveData<User>: Es como una caja transparente. El ViewModel mete el usuario ahí dentro.
    // El Fragmento estará mirando esa caja, y en cuanto vea que aparece algo,
    // actualizará los textos de la pantalla.

    //fetchUserProfile(): Esta función es la que "aprieta el botón".
    // Llama al metodo que creamos antes en el UserRepository.

    //AndroidViewModel: Usamos esta versión (en lugar de ViewModel a secas)
    // porque necesitamos el Application Context para que el Repositorio pueda leer
    // las SharedPreferences (donde tienes guardado el Token).
}
