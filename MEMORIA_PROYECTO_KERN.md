# MEMORIA TÉCNICA Y DE USUARIO - PROYECTO INTERMODULAR KERN

---

## ÍNDICE
1. [Sección 1: Introducción General](#sección-1-introducción-general)
2. [Sección 2: Desarrollo Técnico](#sección-2-desarrollo-técnico)
3. [Manual Técnico: Guía de Instalación](#🛠️-manual-técnico-guía-de-instalación-y-puesta-en-marcha)
4. [Sección 3: Manual de Usuario](#sección-3-manual-de-usuario)
5. [Sección 4: Conclusiones y Mejoras](#sección-4-conclusiones-y-mejoras)
6. [Sección 5: Referencias](#sección-5-referencias)

---

## Sección 1: Introducción General

### 1.1 Identificación del Proyecto
*   **Nombre**: KERN (Fitness Management System)
*   **Ámbito**: Salud, Deporte y Bienestar.
*   **Tipo**: Aplicación multiplataforma con arquitectura Cliente-Servidor.

### 1.2 Justificación y Motivación
El sedentarismo y la falta de planificación son los principales obstáculos para el progreso físico. **KERN** nace para democratizar el entrenamiento de fuerza y la gestión de la actividad física. La motivación principal es construir un sistema que elimine la fricción de "no saber qué hacer" al entrar al gimnasio, proporcionando una estructura científica desde el primer minuto.

### 1.3 Objetivos del Proyecto
*   **Automatización**: Generar rutinas coherentes mediante un cuestionario algorítmico.
*   **Seguimiento Biométrico**: Integrar el conteo de pasos en tiempo real durante el entrenamiento.
*   **Persistencia en la Nube**: Garantizar que el usuario nunca pierda su historial, independientemente del dispositivo.
*   **Experiencia de Usuario (UX) Premium**: Interfaz fluida, moderna y orientada a la eficiencia en entornos de entrenamiento.

### 1.4 Stack Tecnológico (Deep Dive)

#### Backend (Microservicio de API)
*   **Lenguaje**: Python 3.10+
*   **Framework**: FastAPI (Asíncrono).
*   **ORM**: SQLAlchemy 1.4+ con `AsyncSession` y SQLModel.
*   **Seguridad**: `python-jose` para JWT y `hashlib` para encriptación de datos.
*   **Infraestructura**: Despliegue en **Vercel** con base de datos **PostgreSQL** (Neon).

#### Frontend (Aplicación Nativa)
*   **Lenguaje**: Java (Android SDK).
*   **Arquitectura**: MVVM (Model-View-ViewModel) con arquitectura limpia por capas.
*   **Networking**: Retrofit 2 + OkHttp 4.
*   **Gráficos**: MPAndroidChart para la representación de volumen de entrenamiento.
*   **Imágenes**: Glide para optimización de bitmaps y carga asíncrona.
*   **Cloud Multimedia**: Cloudinary SDK para la gestión distribuida de avatares.

---

## Sección 2: Desarrollo Técnico

### 2.1 Arquitectura y Diseño de Datos

#### 2.1.1 Diagrama de Base de Datos (Estructura Relacional)
La base de datos PostgreSQL gestiona la relación entre usuarios, sus rutinas personalizadas y el historial de sesiones.

```mermaid
erDiagram
    USER ||--o{ ROUTINE : "crea"
    USER ||--o{ WORKOUT_SESSION : "realiza"
    ROUTINE ||--o{ ROUTINE_EXERCISE : "contiene"
    
    USER {
        int id PK
        string username
        string email
        string hashed_password
        string avatar_url
        string assigned_routine
        boolean has_completed_quiz
        datetime created_at
    }
    
    ROUTINE {
        int id PK
        int user_id FK
        string name
        datetime created_at
    }
    
    ROUTINE_EXERCISE {
        int id PK
        int routine_id FK
        string exercise_name
        json series "Contiene peso y reps en formato JSON"
    }
    
    WORKOUT_SESSION {
        int id PK
        int user_id FK
        string routine_name
        int duration_seconds
        float total_volume
        int steps
        json data_json "Snapshot completo de la sesión"
        datetime created_at
    }
```

#### 2.1.2 Lógica de Seguridad y Validación de Datos
*   **Pydantic Schemas**: La API utiliza modelos de Pydantic (`UserCreate`, `LoginRequest`, `WorkoutSessionCreate`) para validar estrictamente los datos de entrada. Esto evita ataques de inyección de datos y asegura que el frontend siempre envíe la información en el formato correcto (ej: validación de tipos de datos en la duración del entrenamiento).
*   **Hashing de Contraseñas**: Se aplica el algoritmo **SHA-256** mediante la librería `hashlib`. El proceso es irreversible, garantizando que incluso en caso de una brecha de seguridad en la base de datos, las credenciales de los usuarios permanezcan protegidas.
*   **JWT (JSON Web Tokens)**: El sistema utiliza el algoritmo `HS256` para firmar los tokens. Cada token contiene el `username` del usuario y un timestamp de expiración (`exp`), lo que permite una comunicación *stateless* (sin estado) y segura.

#### 2.1.3 Inicialización de la Base de Datos
El archivo `database.py` incluye una función crítica `create_db_and_tables_sync()` que se ejecuta en el evento `startup` de FastAPI. Esta función detecta si las tablas (`users`, `routines`, `routine_exercises`, `workout_sessions`) existen en el servidor PostgreSQL (Neon/Vercel) y las crea automáticamente si es necesario, facilitando el despliegue continuo sin intervención manual.


### 2.2 Desarrollo del Servidor (FastAPI)

#### Generación de Rutinas por Cuestionario
El endpoint `/users/complete-quiz` utiliza una lógica de selección basada en el catálogo maestro de ejercicios. Si un usuario selecciona "Gimnasio", el sistema accede a `exercises_gym_id.json`, filtra por los grupos musculares necesarios para la rutina elegida (ej: Full Body) y selecciona ejercicios que cumplan con los patrones de movimiento de empuje, tracción y pierna.

### 2.3 Desarrollo del Cliente (Android Studio)

#### 2.3.1 Estructura del Proyecto
El código se organiza en paquetes funcionales para maximizar la mantenibilidad:
*   `ui.adapter`: Adaptadores de RecyclerView (ej: `ActiveExerciseAdapter` para la sesión en vivo).
*   `ui.login`: Gestión de acceso y registro.
*   `services`: Servicios en segundo plano como `StepCounterService`.
*   `viewmodel`: Centraliza la lógica de los fragmentos, evitando que la UI maneje datos crudos.

#### 2.3.2 El Motor de Progresión (Workout Active)
El fragmento `WorkoutActiveFragment` es el núcleo de la aplicación. Realiza dos tareas críticas:
1.  **Consumo de Historial**: Al cargar un ejercicio, la API busca en `workout_sessions` el valor más reciente de `kilos` y `reps` para ese ejercicio específico del usuario, mostrándolo como sugerencia visual ("Anterior: 60kg x 10").
2.  **Cálculo de Volumen**: En tiempo real, calcula la suma de `kilos * repeticiones` de todas las series completadas para generar el dato de **Volumen Total** de la sesión.

#### 2.3.3 Sensor de Pasos (Integración Biométrica)
El `StepCounterService` se registra como un `Foreground Service` con una notificación persistente. Esto asegura que Android no mate el proceso durante el entrenamiento. Utiliza el sensor `TYPE_STEP_COUNTER` para contar los pasos realizados estrictamente durante la sesión, permitiendo al usuario saber cuánta actividad extra-entrenamiento ha generado.

### 2.4 Gestión de APIs Externas: Cloudinary
Se ha implementado una integración asíncrona con Cloudinary. El flujo es:
1.  Usuario selecciona foto en el móvil.
2.  Android sube el binario directamente a Cloudinary usando el `MediaManager`.
3.  Tras el éxito, Cloudinary devuelve una URL pública segura (`https://res.cloudinary.com/...`).
4.  Android envía esa URL al endpoint `PUT /users/avatar` de KERN API.

---

## 🛠️ Manual Técnico: Guía de Instalación y Puesta en Marcha

Esta sección detalla los pasos necesarios para desplegar el entorno de desarrollo de KERN desde cero, cubriendo tanto el ecosistema backend como la aplicación móvil.

### 1. Requisitos Previos

Antes de comenzar, asegúrese de tener instaladas las siguientes herramientas en sus versiones mínimas recomendadas:

*   **Python 3.10+**: Necesario para ejecutar la API asíncrona.
*   **Java JDK 17**: Versión estándar para el desarrollo actual de Android.
*   **Git**: Para el control de versiones y clonación del proyecto.
*   **PostgreSQL 14+**: Motor de base de datos relacional (o acceso a una instancia en la nube como Neon.tech).
*   **Android Studio (Flamingo o superior)**: Entorno de desarrollo para la App móvil.

### 2. Puesta en marcha del Backend (`kern-api`)

El backend de KERN es el corazón del sistema, encargado de la lógica de negocio y la persistencia de datos.

#### 2.2.1 Clonación y Entorno Virtual
Abra una terminal en su directorio de trabajo y ejecute los siguientes comandos:

```bash
# 1. Clonar el repositorio
git clone <url-del-repositorio-kern-api>
cd kern-api

# 2. Crear un entorno virtual para aislar las dependencias
python -m venv venv

# 3. Activar el entorno virtual
# En Windows:
.\venv\Scripts\activate
# En Linux/macOS:
source venv/bin/activate
```

#### 2.2.2 Instalación de Dependencias
Con el entorno virtual activo, instale los paquetes necesarios:

```bash
# Instalar dependencias desde el archivo requirements.txt
pip install -r requirements.txt
```

#### 2.2.3 Configuración de Variables de Env (Archivo .env)
Cree un archivo llamado `.env` en la raíz de la carpeta `kern-api`. Este archivo contendrá las credenciales sensibles:

```env
# Clave secreta para firmar los tokens JWT
SECRET_KEY=su_clave_secreta_super_segura_32_caracteres

# URL de conexión a la base de datos PostgreSQL (Formato asyncpg)
DATABASE_URL=postgresql+asyncpg://usuario:password@host:puerto/nombre_db
```

*   **Nota**: Si utiliza una base de datos con SSL (como Neon), añada `?ssl=require` al final de la URL.

#### 2.2.4 Ejecución y Creación de Tablas
La API está configurada para crear las tablas automáticamente al arrancar. Para iniciar el servidor:

```bash
# Levantar el servidor con Uvicorn y recarga automática
uvicorn app:app --reload --port 8000
```

### 3. Puesta en marcha del Frontend (`ProyectoIntermodular`)

#### 3.1 Configuración de Claves Locales
Abra el proyecto en **Android Studio**. Localice el archivo `local.properties` (en la raíz del proyecto) y añada las claves de Cloudinary y la URL del backend:

```properties
# Credenciales de Cloudinary
CLOUDINARY_CLOUD_NAME=nombre_de_tu_cloud
CLOUDINARY_API_KEY=tu_api_key_numerica
CLOUDINARY_UPLOAD_PRESET=tu_preset_sin_firmar

# URL de la API de KERN
# 10.0.2.2 es el alias del localhost para el emulador de Android
API_URL=http://10.0.2.2:8000/
```

#### 3.2 Sincronización y Compilación
1.  Pulse en **Sync Project with Gradle Files**.
2.  Espere a que finalice la descarga de librerías.
3.  Seleccione un dispositivo y pulse **Run 'app'**.

### 4. Verificación de la Instalación

#### 4.1 FastAPI Interactive Docs
Acceda a `http://127.0.0.1:8000/docs` para visualizar la documentación automática de Swagger y probar los endpoints.

#### 4.2 Generación de APK
Para generar un instalable de depuración: `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

### 5. Solución de Problemas Comunes (Troubleshooting)

| Problema | Causa | Solución |
| :--- | :--- | :--- |
| **CLEARTEXT_COMMUNICATION** | Bloqueo de tráfico HTTP. | Verifique que `network_security_config.xml` permite el dominio `10.0.2.2`. |
| **DB Connection Refused** | PostgreSQL no escucha o puerto bloqueado. | Verifique que el servicio de Postgres está iniciado y el firewall permite el puerto 5432. |
| **Auth Error 401** | Token JWT expirado o SECRET_KEY desajustada. | Reinicie sesión en la App para generar un nuevo token válido. |

---


## Sección 3: Manual de Usuario

### 3.1 Flujo de Registro y Bienvenida
Al abrir KERN por primera vez, el usuario es recibido por una pantalla de bienvenida.
*   **Paso 1**: Click en "Empezar" o "Ya tengo cuenta".
*   **Paso 2**: En el registro, introducir nombre, email y contraseña.
*   **Paso 3**: Se redirige al **Cuestionario Inicial**.

[INSERTAR CAPTURA DE: pantalla_bienvenida_y_registro]

### 3.2 El Cuestionario de Perfil
Es fundamental completar este paso para que KERN pueda "pensar" por el usuario.
*   Se pregunta el objetivo (Gimnasio o Casa).
*   Se selecciona la frecuencia semanal.
*   Al finalizar, el sistema muestra: "Hemos generado tu rutina X".

[INSERTAR CAPTURA DE: flujo_cuestionario_perfil]

### 3.3 Dashboard Principal (Home)
El centro neurálgico del usuario.
*   **Estadísticas**: Visualización rápida de entrenamientos totales y pasos acumulados.
*   **Gráfico**: Línea de tiempo con el volumen levantado.
*   **Rendimiento**: Botón para iniciar el entrenamiento del día.

[INSERTAR CAPTURA DE: dashboard_principal_home]

### 3.4 Ejecución del Entrenamiento
Cuando el usuario está en el gimnasio:
*   **Ver Ejercicios**: Lista de movimientos asignados.
*   **Registro de Series**: Por cada serie, introducir peso y reps. Click en el "check" para marcar como completado.
*   **Descanso**: Al marcar una serie, se activa un temporizador ajustable.
*   **Finalización**: Al acabar todos los ejercicios, click en "Finalizar". Se mostrará un resumen con el volumen total y los pasos registrados.

[INSERTAR CAPTURA DE: interfaz_entrenamiento_activo]

### 3.5 Gestión de Perfil y Ajustes
El usuario puede cambiar su avatar haciendo click en la imagen de perfil, actualizar su nombre o cerrar sesión de forma segura.

[INSERTAR CAPTURA DE: gestion_perfil_usuario]

---

## Sección 4: Conclusiones y Mejoras

### 4.1 Conclusiones
El Proyecto KERN ha cumplido satisfactoriamente con los requisitos intermodulares, integrando un backend escalable con un cliente móvil de alto rendimiento. Se ha logrado una sincronización perfecta de datos y una experiencia de usuario que aporta valor real al proceso de entrenamiento.

### 4.2 Fortalezas y Debilidades
*   **Fortalezas**:
    *   Arquitectura asíncrona en el servidor.
    *   Gestión inteligente de la progresión de cargas (historial).
    *   Integración exitosa de sensores biométricos nativos.
*   **Debilidades**:
    *   Dependencia de conexión a internet para la persistencia inicial.
    *   Lógica de "Monolito" en el archivo principal de la API.

### 4.3 Roadmap de Mejoras (Futuro)
1.  **Modo Offline con Room**: Implementar caché local en el dispositivo para entrenar en zonas sin cobertura.
2.  **Modularización del Backend**: Migrar la API a una estructura de micro-servicios o paquetes por dominio.
3.  **Compartición Social**: Permitir exportar resúmenes de entrenamiento en formato imagen para redes sociales.
4.  **IA de Entrenamiento**: Integrar modelos básicos de ML para sugerir incrementos de peso basados en la velocidad percibida (RPE).

---

## Sección 5: Referencias

### 5.1 Librerías de Terceros (Detallado)

#### Backend (`requirements.txt`)
*   `fastapi==0.95.0`: Core de la API.
*   `sqlalchemy==1.4.41`: Acceso a datos.
*   `python-jose[cryptography]==3.3.0`: Seguridad JWT.
*   `psycopg2-binary==2.9.5`: Driver de PostgreSQL.

#### Frontend (`build.gradle.kts`)
*   `retrofit:2.9.0`: Cliente REST.
*   `cloudinary-android:2.2.0`: Almacenamiento multimedia.
*   `glide:4.16.0`: Renderizado de imágenes.
*   `MPAndroidChart:v3.1.0`: Visualización de datos.
*   `navigation-fragment:2.7.7`: Gestión de rutas entre pantallas.

### 5.2 Control de Versiones y Gestión
El proyecto se ha desarrollado siguiendo una metodología ágil, con Git como sistema de control de versiones centralizado. Se han realizado commits iterativos divididos por sprints de funcionalidad (Auth, Routines, Stats, Health Sync).

---
**Documentación Final del Proyecto KERN**
*Autor: Jaime Gayo*
*Grado Superior DAM/DAW - 2026*
