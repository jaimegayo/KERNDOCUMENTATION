# Kern API - Backend (FastAPI)

Este repositorio contiene la lógica de servidor para el proyecto Kern, desarrollada con **FastAPI** y **SQLAlchemy**.

## 🚀 Tecnologías
- Python 3.14.2
- FastAPI
- PostgreSQL (Neon.tech)
- JWT para Autenticación

## 🛠️ Instalación y Uso
1. Clonar el repositorio.
2. Crear entorno virtual: `python -m venv venv`.
3. Activar entorno: `.\venv\Scripts\activate` (Windows).
4. Instalar dependencias: `pip install -r requirements.txt`.
5. Ejecutar: `uvicorn app_clean:app --reload --port 8002`.

## 📁 Endpoints Principales
- `POST /login`: Autenticación de usuarios.
- `POST /register`: Registro de nuevos perfiles.
- `POST /users/complete-quiz`: Guardado de rutina asignada.

---

```mermaid
    actor Usuario
    
    package "Sistema KERN" {
        usecase "Registrar Cuenta / Login" as UC1
        usecase "Realizar Cuestionario" as UC2
        usecase "Generar Rutina Automática" as UC2_1
        usecase "Gestionar Rutinas (CRUD)" as UC3
        usecase "Realizar Entrenamiento" as UC4
        usecase "Contar Pasos (Background)" as UC4_1
        usecase "Finalizar y Guardar Sesión" as UC4_2
        usecase "Consultar Estadísticas" as UC5
        usecase "Actualizar Avatar (Cloudinary)" as UC6
    }
    
    Usuario --> UC1
    Usuario --> UC2
    Usuario --> UC3
    Usuario --> UC4
    Usuario --> UC5
    Usuario --> UC6
    
    UC2 ..> UC2_1 : <<include>>
    UC4 ..> UC4_1 : <<include>>
    UC4 ..> UC4_2 : <<include>>
```

