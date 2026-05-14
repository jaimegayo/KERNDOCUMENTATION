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
*Desarrollado por Jaime Gayo - I.E.S. Ágora (2026)*
