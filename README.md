# KERN: Fitness Management System

Este repositorio contiene el proyecto completo **KERN**, una solución integral para la gestión de rutinas de entrenamiento y seguimiento de actividad física desarrollada como Proyecto Intermodular para el grado de **DAM**.

---

## 📄 Documentación Breve
La documentación técnica completa, incluyendo el manual de instalación, la arquitectura detallada y el manual de usuario, se encuentra en el siguiente archivo:

👉 **[MEMORIA_PROYECTO_KERN_FINAL.md](./MEMORIA_PROYECTO_KERN_FINAL.md)**

---

## Requisitos

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

### Clonar/Descargar el Proyecto

```bash
cd tu-directorio-de-proyectos
# Si tienes git:
git clone <[url-del-repositorio](https://github.com/jaimegayo/KERNDOCUMENTATION.git)>
cd kern-api
```


## 🚀 Configuración de Ejecución Local

### Backend (`/kern-api`)

```bash
# Crear entorno virtual
python -m venv venv

# Activar entorno virtual
# Windows
venv\Scripts\activate

# Linux/macOS
source venv/bin/activate

# Instalar dependencias
pip install -r requirements.txt

# Ejecutar servidor
```bash
python app.py
```
O con recarga automática (recomendado para desarrollo):

```bash
uvicorn app:app --reload --host 0.0.0.0 --port 8002
```

### Verificar

Abre en tu navegador:
- **API**: http://localhost:8002
- **Documentación Swagger**: http://localhost:8002/docs
- **Documentación ReDoc**: http://localhost:8002/redoc

---

### Frontend (`/ProyectoIntermodular`)

Configurar la URL de conexión en producción:

```java
private static final String BASE_URL =
    "https://kern-blue.vercel.app/";
```

Para pruebas locales con emulador Android:

```java
private static final String BASE_URL =
    "http://10.0.2.2:8002/";
```

### Tecnologías de Despliegue

| Componente | Tecnología | Función |
|------------|------------|----------|
| Frontend | Android (Java) | Aplicación cliente |
| Backend | FastAPI | API REST |
| Hosting | Vercel | Ejecución serverless |
| Base de Datos | Neon PostgreSQL | Persistencia cloud |
| Multimedia | Cloudinary | Gestión de imágenes |
| Comunicación | Retrofit + HTTPS | Cliente-servidor |

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

---

## Despliegue del Sistema

El despliegue de KERN se basa en una arquitectura desacoplada, donde el cliente Android, el backend, la base de datos y el almacenamiento multimedia operan como servicios independientes conectados mediante peticiones HTTPS.

### Arquitectura de Despliegue

```mermaid
graph TD

    A[Aplicación Android<br/>Java + MVVM] -->|HTTPS + JWT<br/>Retrofit| B[Backend FastAPI<br/>Vercel Serverless]

    B -->|SQLAlchemy + asyncpg| C[(Neon PostgreSQL)]

    A -->|Upload Imagen| D[Cloudinary CDN]

    D -->|URL Imagen| B

    B -->|JSON Response| A
```

### Flujo de Puesta en Producción

```mermaid
flowchart TD

    A[Clonar repositorio] --> B[Crear entorno virtual Python]

    B --> C[Instalar dependencias<br/>requirements.txt]

    C --> D[Configurar variables de entorno<br/>DATABASE_URL y SECRET_KEY]

    D --> E[Ejecutar backend local<br/>Uvicorn / FastAPI]

    E --> F[Verificar Swagger UI<br/>localhost:8002/docs]

    F --> G[Configurar BASE_URL en Android]

    G --> H[Compilar APK Android]

    H --> I[Desplegar backend en Vercel]

    I --> J[Conectar con Neon PostgreSQL]

    J --> K[Sistema en Producción]
```

### Proceso de Despliegue del Backend

```mermaid
sequenceDiagram

    participant Dev as Desarrollador
    participant GitHub
    participant Vercel
    participant FastAPI
    participant NeonDB as Neon PostgreSQL

    Dev->>GitHub: Push del código
    GitHub->>Vercel: Detecta cambios
    Vercel->>FastAPI: Build Serverless Function
    FastAPI->>NeonDB: Conexión asyncpg
    NeonDB-->>FastAPI: Base de datos operativa
    FastAPI-->>Vercel: API desplegada
```
---
## Usuarios de Prueba

| Email | Contraseña |
|---------|------------|
| calvo@gmail.com | password123pruebaApp |

---

### 10.1 Mejoras Pendientes

- [ ] **Recuperación de contraseña** - Endpoint para reset password
- [ ] **Logs y monitoreo** - Registrar actividad
- [ ] **Tests automatizados** - Pytest para la API

---

*Desarrollado por Jaime Gayo - I.E.S. Ágora (2026)*
