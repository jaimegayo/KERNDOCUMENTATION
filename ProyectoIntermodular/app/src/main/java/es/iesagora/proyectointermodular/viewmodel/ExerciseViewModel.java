package es.iesagora.proyectointermodular.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import es.iesagora.proyectointermodular.data.model.Exercise;

public class ExerciseViewModel extends AndroidViewModel { //usamos androidviewmodel porque necesitamos el contexto de la app y poder abrir la carpeta assets
    private final MutableLiveData<List<Exercise>> exercises = new MutableLiveData<>();

    public ExerciseViewModel (@NonNull Application application) {
        super(application);
    }

    public LiveData<List<Exercise>> getExercises() {
        return exercises;
    }

    //METODO PARA CARGAR DATOS Y-O EJERCICIOS DESDE ASSETS (GIMNASIO O CASA)
    public void loadExercises(String fileName) {
        String json = null;
        try {
            //vamos hacia la carpeta assets para coger el archivo json
            InputStream is = getApplication().getAssets().open(fileName);
            //medimos cuanto ocupa el archivo para preparar un contenedor del mismo tamano
            int size = is.available();
            byte[] buffer = new byte[size];//esto va a ser la caja donde metamos los datos en "crudo"
            //volcamos nuestros datos al cubo, el buffer
            is.read(buffer);
            is.close();//cerramos porque ya tenemos lo que queremos y hay que liberar memoria
            //pasamos de bytes crudos a String texto
            json = new String(buffer, StandardCharsets.UTF_8);

            //ahora lo analizamos con gson
            Gson gson = new Gson();
            //como vamos a recibir una lista de ejercicios y no uno solo,
            //necesitamos decirle a java exactamente que tipo de lista es (List<Exercise>).
            Type listType = new TypeToken<List<Exercise>>() {}.getType();
            //ahora gson lee el texton json y crea automaticamente los objetos exercise
            List<Exercise> list = gson.fromJson(json, listType);

            //enviamos la lista final al LiveData para que la pantalla se entere y se actualice
            exercises.setValue(list);
        } catch (IOException e) {
            e.printStackTrace();//si no existe el archivo o hay un error de lectura, lo imprimimos
            exercises.setValue(new ArrayList<>());//respondemos con una lista vacia para que no se quede bloqueada la app
        }
    }
}
