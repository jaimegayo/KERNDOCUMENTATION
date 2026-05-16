# KERN: Fitness Management System

Este repositorio contiene el proyecto completo **KERN**, una solución integral para la gestión de rutinas de entrenamiento y seguimiento de actividad física desarrollada como Proyecto Intermodular para el grado de **DAM/DAW**.

---

## 📄 Documentación Definitiva
La documentación técnica completa, incluyendo el manual de instalación, la arquitectura detallada y el manual de usuario, se encuentra en el siguiente archivo:

👉 **[MEMORIA_PROYECTO_KERN_FINAL.md](./MEMORIA_PROYECTO_KERN_FINAL.md)**

---

## 🚀 Inicio Rápido

### Backend (`/kern-api`)
```bash
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
uvicorn app:app --reload
```

### Frontend (`/ProyectoIntermodular`)
1. Abrir con Android Studio.
2. Configurar `local.properties` con las claves de Cloudinary y la URL de la API (`http://10.0.2.2:8000/`).
3. Sincronizar Gradle y ejecutar en el emulador.

---

## 🛠️ Tecnologías Principales
*   **Servidor**: FastAPI, SQLAlchemy, PostgreSQL.
*   **Móvil**: Android Nativo (Java), MVVM, Retrofit.
*   **Servicios**: Cloudinary (Imágenes), Step Counter (Sensores biométricos).

---
## 2.X Despliegue del Sistema

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

### Configuración de Ejecución Local

#### Backend (FastAPI)

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
uvicorn app:app --reload --host 0.0.0.0 --port 8002
```

#### Frontend Android

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

### Capturas recomendadas
- Dashboard de **Vercel** mostrando el despliegue correcto.  
- Consola de **Neon PostgreSQL** con las tablas del sistema.  
- Swagger UI (`/docs`) funcionando en producción.  
- Panel de **Cloudinary** con avatares subidos.

*Desarrollado por Jaime Gayo - I.E.S. Ágora (2026)*
