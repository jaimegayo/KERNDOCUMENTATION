from sqlalchemy import Column, Integer, Float, String, Boolean, DateTime, ForeignKey, JSON
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship
from database import Base # Importamos la Base de nuestro archivo database.py
from pydantic import BaseModel # Necesario para el esquema de ejercicios
from typing import List, Optional # Para las listas de músculos y tips

# ==========================================================
# MODELO DE BASE DE DATOS (SQLAlchemy) - Tabla "users"
# ==========================================================
class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    username = Column(String(128), unique=True, index=True, nullable=False)
    email = Column(String(255), unique=True, index=True, nullable=False)
    hashed_password = Column(String(256), nullable=False)
    
    full_name = Column(String(100), nullable=True)
    phone = Column(String(50), nullable=True)
    avatar_url = Column(String(255), nullable=True)
    role = Column(String(50), default="user")
    bio = Column(String(500), nullable=True)
    
    # --- NUEVOS CAMPOS PARA EL CUESTIONARIO ---
    has_completed_quiz = Column(Boolean, default=False)
    assigned_routine = Column(String(100), nullable=True)
    # ------------------------------------------

    disabled = Column(Boolean, default=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())

    # Relación con las rutinas: un usuario puede tener muchas rutinas
    routines = relationship("Routine", back_populates="owner")
    
    def __repr__(self):
        return f"<User(username='{self.username}', email='{self.email}', quiz_done={self.has_completed_quiz})>"
    
    # --- NUEVA TABLA PARA LAS RUTINAS ---
class Routine(Base):
    __tablename__ = "routines"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    name = Column(String(255), nullable=False)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    
    # Relación con el usuario: una rutina pertenece a un usuario || MODIFICADO: Añadido el borrado en cascada
    owner = relationship("User", back_populates="routines")
    exercises = relationship("RoutineExercise", back_populates="routine", cascade="all, delete-orphan")


    # --- NUEVA TABLA PARA LOS EJERCICIOS DE LAS RUTINAS ---
class RoutineExercise(Base):
    __tablename__ = "routine_exercises"

    id = Column(Integer, primary_key=True, index=True)
    routine_id = Column(Integer, ForeignKey("routines.id"))
    exercise_name = Column(String(255), nullable=False)
    # Guardamos las series como JSON directamente para facilitar el manejo
    series = Column(JSON, nullable=False) 

    routine = relationship("Routine", back_populates="exercises")


# --- NUEVA TABLA PARA EL HISTORIAL DE ENTRENAMIENTOS ---
class WorkoutSession(Base):
    __tablename__ = "workout_sessions"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    routine_name = Column(String(255), nullable=False)
    duration_seconds = Column(Integer, nullable=False) # Guardamos cuánto duró
    total_volume = Column(Float, default=0.0)
    steps = Column(Integer, default=0) # NUEVO: Campo para pasos
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    # Guardamos todos los ejercicios y series realizados en un JSON gigante
    # Esto es mucho más eficiente para un historial que no vas a editar
    data_json = Column(JSON, nullable=False) 

    owner = relationship("User")

# ==========================================================
# ESQUEMAS DE LA API (Pydantic) - Estructura de Ejercicios
# ==========================================================

#Esquema para las series que vienen en Android
class SerieCreate(BaseModel):
    numSerie: int
    kilos: float
    reps: int

# Esquema para las series (Añadimos "anterior" para la lectura)
class SerieResponse(BaseModel):
    numSerie: int
    kilos: float
    reps: int
    anterior: Optional[str] = "--" # Dato histórico que mostraremos en Android

    class Config:
        from_attributes = True

#Esquema para los ejercicios dentro de una rutina
class RoutineExerciseCreate(BaseModel):
    nombre: str
    series: List[SerieCreate]


#Esquema principal para crear la rutina completa
class RoutineCreate(BaseModel):
    user_id: int
    nombre: str
    ejercicios: List[RoutineExerciseCreate]

# Esquema para los ejercicios dentro de una rutina (Lectura)
class RoutineExerciseResponse(BaseModel):
    nombre: str
    series: List[SerieResponse]

    class Config:
        from_attributes = True

# Esquema principal para el detalle de la rutina (Lo que recibe WorkoutActiveFragment)
class RoutineDetailResponse(BaseModel):
    id: int
    nombre: str
    ejercicios: List[RoutineExerciseResponse]

    class Config:
        from_attributes = True        

class Exercise(BaseModel):
    id: int
    name: str
    primary_muscle: str
    secondary_muscles: List[str]
    equipment: str
    difficulty: str
    exercise_type: str
    movement_pattern: str
    unilateral: bool
    instructions: str
    pro_tips: List[str]
    common_mistakes: List[str]
    image_url: Optional[str] = None


class WorkoutSessionCreate(BaseModel):
    routine_name: str
    duration_seconds: int
    total_volume: float
    steps: int = 0 # NUEVO: Campo para pasos
    data_json: List[dict] # Aquí llegará la lista de ejercicios con sus series

    class Config:
        from_attributes = True