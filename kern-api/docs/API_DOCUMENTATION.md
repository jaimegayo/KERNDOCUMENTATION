# 📚 Documentación Completa - Kern API

## Índice

1. [Introducción](#1-introducción)
2. [Requisitos](#2-requisitos)
3. [Instalación y Ejecución](#3-instalación-y-ejecución)
4. [Conceptos Clave](#4-conceptos-clave)
5. [Endpoints de la API](#5-endpoints-de-la-api)
6. [Integración con Android](#6-integración-con-android)
7. [Pruebas de la API](#7-pruebas-de-la-api)
8. [Manejo de Errores](#8-manejo-de-errores)
9. [Seguridad](#9-seguridad)
10. [Próximos Pasos](#10-próximos-pasos)

---

## 1. Introducción

### ¿Qué es Kern API?

Kern API es una API REST de autenticación desarrollada con **FastAPI** (Python). Proporciona un sistema completo de login, registro y protección de rutas usando **JWT (JSON Web Tokens)**.

### Características

- ✅ Login con usuario y contraseña
- ✅ Registro de nuevos usuarios
- ✅ Tokens JWT para autenticación
- ✅ Rutas protegidas
- ✅ CORS configurado para Android
- ✅ Documentación automática (Swagger)
- ✅ Validación automática de datos

### Stack Tecnológico

| Tecnología | Propósito |
|------------|-----------|
| **FastAPI** | Framework web moderno y rápido |
| **Pydantic** | Validación de datos |
| **python-jose** | Manejo de JWT |
| **uvicorn** | Servidor ASGI |

---

## 2. Requisitos

### Software Necesario

- **Python 3.8+** instalado
- **pip** (gestor de paquetes de Python)
- **Git** (opcional, para control de versiones)

### Verificar Python

```bash
python --version
# Debería mostrar: Python 3.8.x o superior
```

---

## 3. Instalación y Ejecución

### Paso 1: Clonar/Descargar el Proyecto

```bash
cd tu-directorio-de-proyectos
# Si tienes git:
git clone <url-del-repositorio>
cd kern-api
```

### Paso 2: Crear Entorno Virtual

```bash
# Crear entorno virtual
python -m venv venv

# Activar entorno virtual
# Windows:
venv\Scripts\activate
# Linux/Mac:
source venv/bin/activate
```

### Paso 3: Instalar Dependencias

```bash
pip install -r requirements.txt
```

### Paso 4: Ejecutar el Servidor

```bash
python app.py
```

O con recarga automática (recomendado para desarrollo):

```bash
uvicorn app:app --reload --host 0.0.0.0 --port 8002
```

### Paso 5: Verificar

Abre en tu navegador:
- **API**: http://localhost:8002
- **Documentación Swagger**: http://localhost:8002/docs
- **Documentación ReDoc**: http://localhost:8002/redoc

---

## 4. Conceptos Clave

### 4.1 ¿Qué es JWT?

JWT (JSON Web Token) es un estándar para transmitir información de forma segura entre partes. Un JWT tiene 3 partes:

```
eyJhbGciOiJIUzI1NiIs...  (Header - algoritmo)
.
eyJzdWIiOiJhZG1pbiIs...  (Payload - datos del usuario)
.
pT8jy9wUMBUF7jvcDvFF...  (Signature - firma de seguridad)
```

**Ventajas de JWT:**
- ✅ Sin estado (stateless) - El servidor no guarda sesiones
- ✅ Escalable - Funciona con múltiples servidores
- ✅ Móvil-friendly - Fácil de almacenar en apps

### 4.2 Flujo de Autenticación

```
┌─────────────┐                              ┌─────────────┐
│   ANDROID   │                              │   SERVIDOR  │
│     APP     │                              │   (API)     │
└──────┬──────┘                              └──────┬──────┘
       │                                            │
       │  1. POST /login                            │
       │  { username, password }                    │
       │ ──────────────────────────────────────────>│
       │                                            │
       │  2. Verificar credenciales                 │
       │                                            │
       │  3. Generar JWT                            │
       │                                            │
       │  4. { access_token: "eyJ..." }             │
       │ <──────────────────────────────────────────│
       │                                            │
       │  5. Guardar token en SharedPreferences     │
       │                                            │
       │  6. GET /users/me                          │
       │  Header: Authorization: Bearer eyJ...      │
       │ ──────────────────────────────────────────>│
       │                                            │
       │  7. Validar token                          │
       │                                            │
       │  8. { username, email, ... }               │
       │ <──────────────────────────────────────────│
       │                                            │
```

### 4.3 Estructura del Proyecto

```
kern-api/
├── app.py              # Código principal de la API
├── requirements.txt    # Dependencias Python
├── README.md           # Documentación básica
├── docs/
│   └── API_DOCUMENTATION.md  # Este archivo
└── venv/               # Entorno virtual (no commitear)
```

---

## 5. Endpoints de la API

### 5.1 Resumen de Endpoints

| Método | Endpoint | Autenticación | Descripción |
|--------|----------|---------------|-------------|
| GET | `/` | ❌ No | Verificar estado de la API |
| POST | `/login` | ❌ No | Login con JSON |
| POST | `/token` | ❌ No | Login OAuth2 (form-data) |
| POST | `/register` | ❌ No | Registrar usuario |
| GET | `/users/me` | ✅ Sí | Obtener usuario actual |
| GET | `/protected` | ✅ Sí | Ejemplo ruta protegida |

---

### 5.2 Detalle de Endpoints

#### GET / - Estado de la API

Verifica que la API está funcionando.

**Request:**
```http
GET http://localhost:8002/
```

**Response (200 OK):**
```json
{
    "message": "Bienvenido a Kern API",
    "status": "online"
}
```

---

#### POST /login - Iniciar Sesión

Autentica un usuario y devuelve un token JWT.

**Request:**
```http
POST http://localhost:8002/login
Content-Type: application/json

{
    "username": "admin",
    "password": "password123"
}
```

**Response (200 OK):**
```json
{
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsImV4cCI6MTczNDE2OTYwMH0.xxxxx",
    "token_type": "bearer"
}
```

**Response (401 Unauthorized):**
```json
{
    "detail": "Usuario o contraseña incorrectos"
}
```

---

#### POST /token - Login OAuth2

Alternativa al /login usando form-data (estándar OAuth2).

**Request:**
```http
POST http://localhost:8002/token
Content-Type: application/x-www-form-urlencoded

username=admin&password=password123
```

**Response:** Igual que /login

---

#### POST /register - Registrar Usuario

Crea una nueva cuenta de usuario.

**Request:**
```http
POST http://localhost:8002/register
Content-Type: application/json

{
    "username": "nuevo_usuario",
    "email": "nuevo@example.com",
    "password": "mi_password_seguro",
    "full_name": "Nombre Completo"
}
```

**Response (200 OK):**
```json
{
    "username": "nuevo_usuario",
    "email": "nuevo@example.com",
    "full_name": "Nombre Completo",
    "disabled": false
}
```

**Response (400 Bad Request):**
```json
{
    "detail": "El nombre de usuario ya existe"
}
```

---

#### GET /users/me - Obtener Usuario Actual 🔒

Devuelve la información del usuario autenticado.

**Request:**
```http
GET http://localhost:8002/users/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**Response (200 OK):**
```json
{
    "username": "admin",
    "email": "admin@example.com",
    "full_name": "Administrador",
    "disabled": false
}
```

**Response (401 Unauthorized):**
```json
{
    "detail": "No se pudieron validar las credenciales"
}
```

---

#### GET /protected - Ruta Protegida de Ejemplo 🔒

Ejemplo de cómo funcionan las rutas protegidas.

**Request:**
```http
GET http://localhost:8002/protected
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

**Response (200 OK):**
```json
{
    "message": "Hola Administrador!",
    "user": "admin"
}
```

---

## 6. Integración con Android

### 6.1 Dependencias (build.gradle)

```gradle
dependencies {
    // Retrofit para peticiones HTTP
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    
    // OkHttp para logging (opcional pero recomendado)
    implementation 'com.squareup.okhttp3:logging-interceptor:4.11.0'
    
    // Coroutines para operaciones asíncronas
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    
    // ViewModel y LiveData
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.6.2'
}
```

### 6.2 Modelos de Datos

```kotlin
// models/LoginRequest.kt
data class LoginRequest(
    val username: String,
    val password: String
)

// models/RegisterRequest.kt
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val full_name: String? = null
)

// models/TokenResponse.kt
data class TokenResponse(
    val access_token: String,
    val token_type: String
)

// models/User.kt
data class User(
    val username: String,
    val email: String?,
    val full_name: String?,
    val disabled: Boolean?
)

// models/ErrorResponse.kt
data class ErrorResponse(
    val detail: String
)
```

### 6.3 Interface de la API

```kotlin
// api/ApiService.kt
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // Verificar estado de la API
    @GET("/")
    suspend fun healthCheck(): Response<Map<String, String>>
    
    // Login
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>
    
    // Registro
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<User>
    
    // Obtener usuario actual (requiere token)
    @GET("users/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): Response<User>
    
    // Ruta protegida de ejemplo
    @GET("protected")
    suspend fun getProtectedData(
        @Header("Authorization") token: String
    ): Response<Map<String, String>>
}
```

### 6.4 Configuración de Retrofit

```kotlin
// api/RetrofitClient.kt
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    
    // ⚠️ Cambiar por la IP de tu computadora para pruebas en dispositivo físico
    // Para emulador: "http://10.0.2.2:8002/"
    // Para dispositivo físico: "http://192.168.x.x:8002/"
    private const val BASE_URL = "http://10.0.2.2:8002/"
    
    // Interceptor para logging (ver peticiones en Logcat)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    // Cliente HTTP con configuración
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // Instancia de Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    // Servicio de la API
    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
```

### 6.5 Gestión del Token

```kotlin
// utils/TokenManager.kt
import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "auth_prefs",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
    }
    
    // Guardar token
    fun saveToken(token: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }
    
    // Obtener token
    fun getToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }
    
    // Obtener token con formato Bearer
    fun getBearerToken(): String? {
        val token = getToken()
        return if (token != null) "Bearer $token" else null
    }
    
    // Eliminar token (logout)
    fun clearToken() {
        prefs.edit().remove(KEY_ACCESS_TOKEN).apply()
    }
    
    // Verificar si hay sesión activa
    fun isLoggedIn(): Boolean {
        return getToken() != null
    }
}
```

### 6.6 Repository para Autenticación

```kotlin
// repository/AuthRepository.kt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val tokenManager: TokenManager) {
    
    private val apiService = RetrofitClient.apiService
    
    // Login
    suspend fun login(username: String, password: String): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.login(LoginRequest(username, password))
                
                if (response.isSuccessful) {
                    val tokenResponse = response.body()!!
                    
                    // Guardar el token
                    tokenManager.saveToken(tokenResponse.access_token)
                    
                    // Obtener datos del usuario
                    val userResponse = apiService.getCurrentUser(
                        tokenManager.getBearerToken()!!
                    )
                    
                    if (userResponse.isSuccessful) {
                        Result.success(userResponse.body()!!)
                    } else {
                        Result.failure(Exception("Error al obtener usuario"))
                    }
                } else {
                    Result.failure(Exception("Usuario o contraseña incorrectos"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // Registro
    suspend fun register(
        username: String,
        email: String,
        password: String,
        fullName: String?
    ): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val request = RegisterRequest(username, email, password, fullName)
                val response = apiService.register(request)
                
                if (response.isSuccessful) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Error en el registro"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // Obtener usuario actual
    suspend fun getCurrentUser(): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val token = tokenManager.getBearerToken()
                    ?: return@withContext Result.failure(Exception("No hay sesión activa"))
                
                val response = apiService.getCurrentUser(token)
                
                if (response.isSuccessful) {
                    Result.success(response.body()!!)
                } else {
                    // Token inválido o expirado
                    tokenManager.clearToken()
                    Result.failure(Exception("Sesión expirada"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // Logout
    fun logout() {
        tokenManager.clearToken()
    }
}
```

### 6.7 ViewModel de Login

```kotlin
// viewmodel/LoginViewModel.kt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
    
    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState
    
    fun login(username: String, password: String) {
        _loginState.value = LoginState.Loading
        
        viewModelScope.launch {
            val result = authRepository.login(username, password)
            
            result.fold(
                onSuccess = { user ->
                    _loginState.value = LoginState.Success(user)
                },
                onFailure = { error ->
                    _loginState.value = LoginState.Error(error.message ?: "Error desconocido")
                }
            )
        }
    }
}

sealed class LoginState {
    object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}
```

### 6.8 Ejemplo de Activity

```kotlin
// ui/LoginActivity.kt
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels {
        // Crear ViewModel con dependencias
        val tokenManager = TokenManager(applicationContext)
        val repository = AuthRepository(tokenManager)
        LoginViewModelFactory(repository)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupObservers()
        setupListeners()
    }
    
    private fun setupObservers() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnLogin.isEnabled = false
                }
                is LoginState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Bienvenido ${state.user.full_name}", Toast.LENGTH_SHORT).show()
                    // Navegar a la pantalla principal
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is LoginState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val password = binding.etPassword.text.toString()
            
            if (username.isBlank() || password.isBlank()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            viewModel.login(username, password)
        }
    }
}
```

### 6.9 Permisos de Internet (AndroidManifest.xml)

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- Permiso de Internet -->
    <uses-permission android:name="android.permission.INTERNET" />
    
    <application
        android:usesCleartextTraffic="true"  <!-- Permitir HTTP para desarrollo -->
        ... >
        
    </application>
</manifest>
```

### 6.10 Notas Importantes para Android

#### Conexión con Emulador
El emulador de Android usa una red virtual. Para conectar con localhost:
- Usar `10.0.2.2` en lugar de `localhost` o `127.0.0.1`

```kotlin
private const val BASE_URL = "http://10.0.2.2:8002/"
```

#### Conexión con Dispositivo Físico
1. Asegúrate de que el dispositivo y la PC estén en la misma red WiFi
2. Busca la IP de tu PC (ipconfig en Windows, ifconfig en Linux/Mac)
3. Usa esa IP en BASE_URL

```kotlin
private const val BASE_URL = "http://192.168.1.100:8002/"
```

---

## 7. Pruebas de la API

### 7.1 Usando Swagger UI (Navegador)

1. Abre http://localhost:8002/docs
2. Haz clic en "Authorize" para probar con autenticación
3. Expande cada endpoint y haz clic en "Try it out"

### 7.2 Usando PowerShell

```powershell
# Health Check
Invoke-RestMethod -Uri "http://localhost:8002/"

# Login
$body = @{username="admin@kern.com"; password="password123"} | ConvertTo-Json
$response = Invoke-RestMethod -Uri "http://localhost:8002/login" -Method POST -ContentType "application/json" -Body $body
$token = $response.access_token
Write-Host "Token: $token"

# Obtener usuario actual
$headers = @{Authorization = "Bearer $token"}
Invoke-RestMethod -Uri "http://localhost:8002/users/me" -Method GET -Headers $headers
```

### 7.3 Usando cURL (Git Bash / Linux / Mac)

```bash
# Health Check
curl http://localhost:8002/

# Login
curl -X POST "http://localhost:8002/login" \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "password123"}'

# Guardar token en variable
TOKEN=$(curl -s -X POST "http://localhost:8002/login" \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "password123"}' | jq -r '.access_token')

# Obtener usuario actual
curl -X GET "http://localhost:8002/users/me" \
     -H "Authorization: Bearer $TOKEN"
```

### 7.4 Usando Postman

1. **Importar colección:**
   - Crear nueva colección "Kern API"
   
2. **Configurar variable de entorno:**
   - Crear variable `base_url` = `http://localhost:8002`
   - Crear variable `token` (vacía inicialmente)

3. **Crear peticiones:**
   - Login: POST `{{base_url}}/login`
   - En Tests del login, añadir:
     ```javascript
     var jsonData = pm.response.json();
     pm.environment.set("token", jsonData.access_token);
     ```
   - Para rutas protegidas: Header `Authorization: Bearer {{token}}`

---

## 8. Manejo de Errores

### Códigos de Estado HTTP

| Código | Significado | Cuándo ocurre |
|--------|-------------|---------------|
| **200** | OK | Petición exitosa |
| **400** | Bad Request | Datos inválidos, usuario ya existe |
| **401** | Unauthorized | Credenciales incorrectas, token inválido/expirado |
| **404** | Not Found | Endpoint no existe |
| **422** | Unprocessable Entity | Datos no pasan validación de Pydantic |
| **500** | Internal Server Error | Error del servidor |

### Estructura de Errores

```json
{
    "detail": "Mensaje descriptivo del error"
}
```

### Manejo en Android

```kotlin
suspend fun handleApiCall(): Result<Data> {
    return try {
        val response = apiService.someCall()
        
        when (response.code()) {
            200 -> Result.success(response.body()!!)
            401 -> {
                tokenManager.clearToken()
                Result.failure(AuthException("Sesión expirada"))
            }
            400 -> {
                val error = Gson().fromJson(
                    response.errorBody()?.string(),
                    ErrorResponse::class.java
                )
                Result.failure(Exception(error.detail))
            }
            else -> Result.failure(Exception("Error desconocido"))
        }
    } catch (e: IOException) {
        Result.failure(NetworkException("Sin conexión a internet"))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

---

## 9. Seguridad

### 9.1 Recomendaciones para Producción

| Aspecto | Desarrollo | Producción |
|---------|------------|------------|
| SECRET_KEY | Hardcodeada | Variable de entorno |
| Hash de contraseñas | SHA256 | bcrypt |
| CORS origins | "*" | Dominios específicos |
| HTTPS | No | Obligatorio |
| Logs | Verbose | Limitados |

### 9.2 Ejemplo de Configuración de Producción

```python
import os

# Cargar de variables de entorno
SECRET_KEY = os.environ.get("SECRET_KEY")
if not SECRET_KEY:
    raise ValueError("SECRET_KEY no está configurada")

# bcrypt para contraseñas
from passlib.context import CryptContext
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# CORS restrictivo
app.add_middleware(
    CORSMiddleware,
    allow_origins=["https://mi-app.com"],
    allow_credentials=True,
    allow_methods=["GET", "POST"],
    allow_headers=["Authorization", "Content-Type"],
)
```

### 9.3 Almacenamiento Seguro del Token en Android

```kotlin
// Usar EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureTokenManager(context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun saveToken(token: String) {
        prefs.edit().putString("access_token", token).apply()
    }
    
    // ... resto igual
}
```

---

## 10. Próximos Pasos

### 10.1 Mejoras Pendientes

- [ ] **Base de datos real** - Conectar SQLite, PostgreSQL o MongoDB
- [ ] **Refresh tokens** - Renovar tokens sin re-login
- [ ] **Verificación de email** - Confirmar email al registrarse
- [ ] **Recuperación de contraseña** - Endpoint para reset password
- [ ] **Rate limiting** - Limitar intentos de login
- [ ] **Logs y monitoreo** - Registrar actividad
- [ ] **Tests automatizados** - Pytest para la API
- [ ] **Docker** - Containerizar la aplicación

### 10.2 Estructura Sugerida para Producción

```
kern-api/
├── app/
│   ├── __init__.py
│   ├── main.py           # Aplicación FastAPI
│   ├── config.py         # Configuración
│   ├── models/           # Modelos Pydantic
│   │   ├── __init__.py
│   │   ├── user.py
│   │   └── token.py
│   ├── routes/           # Endpoints
│   │   ├── __init__.py
│   │   ├── auth.py
│   │   └── users.py
│   ├── services/         # Lógica de negocio
│   │   ├── __init__.py
│   │   └── auth_service.py
│   ├── db/               # Base de datos
│   │   ├── __init__.py
│   │   ├── database.py
│   │   └── models.py
│   └── utils/            # Utilidades
│       ├── __init__.py
│       └── security.py
├── tests/
├── .env
├── .gitignore
├── Dockerfile
├── docker-compose.yml
├── requirements.txt
└── README.md
```

---

## Usuarios de Prueba

| Usuario | Contraseña | Email |
|---------|------------|-------|
| admin | password123 | admin@example.com |
| usuario1 | password123 | usuario1@example.com |

---

## Soporte

Si tienes dudas o problemas:
1. Revisa la documentación Swagger: http://localhost:8002/docs
2. Verifica los logs del servidor en la terminal
3. Usa el logging interceptor en Android para ver las peticiones

---

*Documentación generada para Kern API v1.0.0*

