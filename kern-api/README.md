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
