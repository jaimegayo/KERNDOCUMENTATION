# MEMORIA DE PROYECTO INTERMODULAR: KERN (FITNESS MANAGEMENT SYSTEM)

---

## 📑 ÍNDICE DE CONTENIDOS

1.  **SECCIÓN 1: INTRODUCCIÓN GENERAL**
    *   1.1 Origen y Motivación del Proyecto
    *   1.2 Objetivos y Finalidad
    *   1.3 Stack Tecnológico y Justificación
2.  **SECCIÓN 2: DESARROLLO TÉCNICO E IMPLEMENTACIÓN**
    *   2.1 Arquitectura del Sistema
    *   2.2 Diseño de Datos (Modelo Entidad-Relación)
    *   2.3 Implementación del Backend (API REST)
        *   2.3.1 Referencia Detallada de la API (Endpoints)
        *   2.3.2 Seguridad: Hashing y JWT
    *   2.4 Implementación del Frontend (Android Nativo)
        *   2.4.1 Patrón Arquitectónico MVVM
        *   2.4.2 Servicio de Conteo de Pasos (StepCounterService)
    *   2.5 Gestión de APIs Externas y Resolución de Conflictos (Cloudinary)
3.  **MANUAL TÉCNICO: INSTALACIÓN Y PUESTA EN MARCHA**
    *   3.1 Requisitos Previos del Sistema
    *   3.2 Despliegue del Entorno Backend (`kern-api`)
    *   3.3 Despliegue del Entorno Frontend (`ProyectoIntermodular`)
    *   3.4 Verificación de la Instalación y Troubleshooting
4.  **SECCIÓN 3: MANUAL DE USUARIO**
    *   4.1 Registro, Acceso y Onboarding
    *   4.2 Gestión de Rutinas y Seguimiento de Entrenamiento
    *   4.3 Estadísticas y Perfil de Usuario
5.  **SECCIÓN 4: CONCLUSIONES Y MEJORAS FUTURAS**
6.  **SECCIÓN 5: REFERENCIAS Y BIBLIOGRAFÍA**

---

# 1. SECCIÓN 1: INTRODUCCIÓN GENERAL

## 1.1 Origen y Motivación del Proyecto
El Proyecto KERN surge como respuesta a la falta de herramientas integradas que permitan un seguimiento técnico riguroso del entrenamiento de fuerza junto con la actividad física diaria (NEAT). En el contexto académico del I.E.S. Ágora, se ha diseñado un sistema que elimina la subjetividad en la planificación mediante una automatización basada en principios de fisiología deportiva.

## 1.2 Objetivos y Finalidad
La finalidad de KERN es proporcionar al usuario una plataforma de gestión de fitness que actúe como un centro de datos personal. Los objetivos clave son:
*   **Centralización**: Almacenar historial de cargas, repeticiones y volumen total en la nube.
*   **Progresión**: Facilitar la sobrecarga progresiva mediante la comparación visual con sesiones históricas.
*   **Cuantificación**: Integrar el movimiento físico (pasos) dentro de la propia sesión de entrenamiento.

## 1.3 Stack Tecnológico y Justificación
Se ha optado por un ecosistema de tecnologías de vanguardia para asegurar el rendimiento:
*   **Backend**: FastAPI (Python) por su naturaleza asíncrona que reduce los tiempos de respuesta.
*   **Frontend**: Java (Android SDK) para garantizar el acceso directo a los sensores biométricos de bajo nivel.
*   **BBDD**: PostgreSQL por su integridad referencial y soporte avanzado para tipos JSON.
*   **Cloud Multimedia**: Cloudinary para el procesamiento de imágenes off-server.

---

# 2. SECCIÓN 2: DESARROLLO TÉCNICO E IMPLEMENTACIÓN

## 2.1 Arquitectura del Sistema
El sistema sigue una arquitectura distribuida **Cliente-Servidor**. El cliente Android actúa como recolector de datos en tiempo real, mientras que el servidor FastAPI gestiona la persistencia y la lógica de validación.

## 2.2 Diseño de Datos (Modelo Entidad-Relación)
La base de datos se estructura para optimizar las consultas de historial:
*   **Users**: Perfiles, credenciales hasheadas y estado del cuestionario.
*   **Routines**: Cabeceras de planes de entrenamiento.
*   **RoutineExercises**: Unión de ejercicios y series (almacenadas en formato JSON para flexibilidad).
*   **WorkoutSessions**: Histórico inmutable de cada entrenamiento finalizado, incluyendo volumen y pasos.

## 2.3 Implementación del Backend (API REST)

### 2.3.1 Referencia Detallada de la API (Endpoints)
A continuación se detalla el contrato de comunicación entre el frontend y el servidor:

| Método | Ruta | Descripción | Protegido (JWT) |
| :--- | :--- | :--- | :---: |
| `POST` | `/register` | Crea un nuevo usuario y devuelve el primer token. | No |
| `POST` | `/login` | Valida credenciales y otorga acceso. | No |
| `GET` | `/exercises/gym` | Obtiene el catálogo de ejercicios de gimnasio. | No |
| `GET` | `/exercises/home` | Obtiene el catálogo de ejercicios de casa. | No |
| `GET` | `/users/me` | Recupera el perfil completo del usuario activo. | Sí |
| `POST` | `/users/complete-quiz` | Asigna una rutina inicial científica. | Sí |
| `GET` | `/users/stats` | Agregado de volumen total y pasos históricos. | Sí |
| `GET` | `/users/my-routines` | Listado de todas las rutinas del usuario. | Sí |
| `POST` | `/routines/create` | Crea una rutina personalizada con ejercicios. | Sí |
| `GET` | `/routines/{id}` | **Detalle de Rutina + Historial de Cargas**. | Sí |
| `PUT` | `/routines/{id}` | Actualiza ejercicios o nombre de rutina. | Sí |
| `DELETE` | `/routines/{id}` | Elimina una rutina y sus ejercicios en cascada. | Sí |
| `POST` | `/workouts/finish` | Finaliza sesión y guarda snapshot de datos. | Sí |

### 2.3.2 Seguridad: Hashing y JWT
La seguridad se implementa en dos niveles:
1.  **Persistencia**: Uso de `hashlib.sha256` para el almacenamiento de contraseñas.
2.  **Transporte**: Generación de tokens JWT (JSON Web Tokens) firmados con el algoritmo `HS256`, garantizando la integridad de cada petición.

## 2.4 Implementación del Frontend (Android Nativo)

### 2.4.1 Patrón Arquitectónico MVVM
Se ha utilizado el patrón **Model-View-ViewModel** para separar la responsabilidad de la UI de la lógica de datos.
*   **Repository**: Encargado de decidir si los datos vienen de la red (Retrofit) o de la caché.
*   **ViewModel**: Mantiene el estado de la sesión de entrenamiento activo (temporizadores y contadores).
*   **View**: Fragmentos que observan `LiveData` para actualizar la interfaz dinámicamente.

### 2.4.2 Servicio de Conteo de Pasos (StepCounterService)
El sistema utiliza un **Foreground Service** que registra un listener en el sensor `TYPE_STEP_COUNTER`. Este servicio garantiza que el conteo no se detenga si el usuario minimiza la App para cambiar de música, integrando los pasos directamente en la estadística de la sesión de entrenamiento.

## 2.5 Gestión de APIs Externas y Resolución de Conflictos (Cloudinary)
Durante el desarrollo se detectó un **error 403 (Forbidden)** al intentar subir imágenes de perfil desde Android.

**Resolución del Problema:**
*   **Causa**: El SDK de Cloudinary intentaba realizar una subida firmada (`Signed Upload`) sin incluir una firma de servidor, lo cual es la configuración por defecto de seguridad.
*   **Solución**: Se reconfiguró el entorno de Cloudinary para permitir **Unsigned Uploads** mediante la creación de un `Upload Preset` específico. Se actualizó el código de inicialización en Android para utilizar este preset, permitiendo subidas directas y seguras desde el cliente móvil.

---

# 3. MANUAL TÉCNICO: INSTALACIÓN Y PUESTA EN MARCHA

## 3.1 Requisitos Previos del Sistema
*   **Python 3.10+**: Intérprete para la API.
*   **JDK 17**: Para la compilación de la App Android.
*   **PostgreSQL**: Instancia de base de datos local o remota.
*   **Git**: Herramienta de control de versiones.

## 3.2 Despliegue del Entorno Backend (`kern-api`)

1. **Clonación e Instalación**:
```bash
git clone <repositorio_backend>
cd kern-api
python -m venv venv
# Activar entorno (Ejemplo Windows)
.\venv\Scripts\activate
pip install -r requirements.txt
```

2. **Configuración de Variables (`.env`)**:
Cree un archivo `.env` en la raíz con el siguiente formato:
```python
# Configuración sensible del servidor
DATABASE_URL="postgresql+asyncpg://user:pass@host:5432/db"
SECRET_KEY="tu_clave_secreta_para_jwt"
```

3. **Ejecución**:
```bash
uvicorn app:app --reload --port 8000
```

## 3.3 Despliegue del Entorno Frontend (`ProyectoIntermodular`)

1. **Configuración de Claves (`local.properties`)**:
En la raíz del proyecto Android, edite el archivo para incluir sus credenciales:
```properties
CLOUDINARY_CLOUD_NAME=nombre_cloud
CLOUDINARY_API_KEY=tu_api_key
CLOUDINARY_UPLOAD_PRESET=tu_preset_unsigned
API_URL=http://10.0.2.2:8000/
```

2. **Sincronización y Run**:
Abra el proyecto en Android Studio y ejecute la sincronización de Gradle antes de lanzar la App en el emulador.

## 3.4 Verificación de la Instalación y Troubleshooting
*   **API**: Acceda a `http://localhost:8000/docs` para verificar la documentación Swagger.
*   **Android**: Si la App no conecta al backend local, verifique que `network_security_config.xml` permite tráfico de texto plano para el dominio `10.0.2.2`.

---

# 4. SECCIÓN 3: MANUAL DE USUARIO

## 4.1 Registro, Acceso y Onboarding
El usuario inicia creando una cuenta. Tras el registro, es obligatorio completar el **Cuestionario de Perfil** (Objetivo y Frecuencia). Al finalizar, el sistema genera automáticamente una rutina base basada en su elección (Gimnasio o Casa).

[INSERTAR CAPTURA DE: pantalla_onboarding]

## 4.2 Gestión de Rutinas y Seguimiento de Entrenamiento
Desde la pestaña de Rutinas, el usuario puede iniciar una sesión.
*   **Registro de Series**: Por cada ejercicio, el usuario introduce peso y repeticiones.
*   **Historial Visual**: El sistema muestra en gris lo que el usuario levantó en la sesión anterior para fomentar la mejora.
*   **Temporizador**: Cronómetro integrado para medir los tiempos de descanso entre series.

[INSERTAR CAPTURA DE: entrenamiento_activo]

## 4.3 Estadísticas y Perfil de Usuario
En el perfil, el usuario puede visualizar su **Gráfico de Volumen Total** y el acumulado de pasos. También puede actualizar su fotografía de perfil, la cual se procesa y almacena en Cloudinary.

---

# 5. SECCIÓN 4: CONCLUSIONES Y MEJORAS FUTURAS

KERN ha demostrado ser una herramienta sólida para la gestión del entrenamiento. La principal conclusión es que la integración de sensores biométricos (pasos) con el registro de cargas de fuerza aporta un valor diferencial al usuario. 

Como mejoras futuras se plantean:
*   Implementación de **Modo Offline** mediante Room.
*   Refactorización del backend hacia una arquitectura de **APIRouters**.

---

# 6. SECCIÓN 5: REFERENCIAS Y BIBLIOGRAFÍA
*   FastAPI Documentation: https://fastapi.tiangolo.com/
*   Android Developer Guide (MVVM): https://developer.android.com/topic/libraries/architecture/viewmodel
*   Cloudinary Android SDK: https://cloudinary.com/documentation/android_integration
*   MPAndroidChart Repository: https://github.com/PhilJay/MPAndroidChart
