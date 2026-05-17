# =======================================================================
# KERN API AUTENTICACIÓN Y EJERCICIOS
# Este archivo contiene una API REST completa para autenticación de usuarios
# y gestión de base de datos de ejercicios para el TFG.
# =======================================================================

# IMPORTACIONES
from datetime import datetime, timedelta
from typing import Optional, List # List añadida para los ejercicios
import hashlib
import os
import json # Necesario para leer los archivos .json
import models # Importamos el módulo de modelos para acceder a las clases de SQLAlchemy

# dotenv: Para cargar variables de entorno desde un archivo .env
from dotenv import load_dotenv

# FastAPI: Framework principal
from fastapi import FastAPI, HTTPException, Depends, status
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from fastapi.middleware.cors import CORSMiddleware

# Jose: Librería para manejar JWT
from jose import JWTError, jwt

# Pydantic: Para validación de esquemas
from pydantic import BaseModel, Field

# --- IMPORTACIONES DE BASE DE DATOS Y MODELOS ---
from database import create_db_and_tables_sync, get_db 
# Añadimos Routine, RoutineExercise y RoutineCreate a las importaciones de modelos
# NOTA: Usamos 'as DBExercise' para la tabla de SQLAlchemy para evitar conflicto con el modelo Pydantic 'Exercise'
from models import User as DBUser, Exercise as DBExercise, Routine, RoutineExercise, RoutineCreate
from models import WorkoutSession, WorkoutSessionCreate
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy import select, delete, func
import random

# CONFIGURACIÓN INICIAL
load_dotenv()

# Clave secreta para firmar los JWT
SECRET_KEY = os.getenv("SECRET_KEY")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 30

# =======================================================================
# MODELOS DE DATOS (Pydantic)
# Estos modelos definen la estructura de los datos que entran y salen
# =======================================================================

class Token(BaseModel):
    access_token: str
    token_type: str

class TokenData(BaseModel):
    username: Optional[str] = None

class Exercise(BaseModel):
    """Modelo Pydantic para los ejercicios de los JSON"""
    id: int
    name: str
    primary_muscle: str
    movement_pattern: str
    equipment: str
    instructions: str
    pro_tips: List[str]
    common_mistakes: List[str]
    video_url: Optional[str] = None

    class Config:
        from_attributes = True

class UserData(BaseModel):
    """Modelo para devolver datos de usuario de forma segura"""
    id: Optional[int] = None
    username: str
    email: Optional[str] = None
    full_name: Optional[str] = None
    phone: Optional[str] = None
    avatar_url: Optional[str] = None
    role: Optional[str] = "user"
    bio: Optional[str] = None
    has_completed_quiz: bool = False
    assigned_routine: Optional[str] = None
    created_at: Optional[datetime] = None
    disabled: Optional[bool] = None

    class Config:
        from_attributes = True

class UserCreate(BaseModel):
    """Modelo para el registro de nuevos usuarios"""
    username: str
    email: str
    password: str
    full_name: Optional[str] = None
    phone: Optional[str] = None

class LoginRequest(BaseModel):
    """Modelo para la petición de login personalizada"""
    email: str 
    password: str

class LoginResponse(BaseModel):
    """Modelo de respuesta tras login exitoso"""
    access_token: str = Field(..., alias="accessToken")
    token_type: str
    user: UserData

class QuizCompletion(BaseModel):
    """Modelo para recibir la rutina asignada"""
    assigned_routine: str

class AvatarUpdate(BaseModel):
    """Modelo para actualizar la URL del avatar"""
    avatar_url: str

class UsernameUpdate(BaseModel):
    """Modelo para actualizar el nombre de usuario"""
    username: str

class UserStats(BaseModel):
    """Modelo para devolver estadísticas del usuario"""
    total_workouts: int
    total_steps: int
    training_days: List[str] # Lista de fechas "YYYY-MM-DD"

# =======================================================================
# FUNCIONES AUXILIARES Y SEGURIDAD
# Estas funciones manejan la lógica de contraseñas, tokens y carga de datos
# =======================================================================

def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verifica si la contraseña ingresada coincide con el hash almacenado."""
    return hashlib.sha256(plain_password.encode()).hexdigest() == hashed_password

def get_password_hash(password: str) -> str:
    """Genera un hash SHA-256 de una contraseña en texto plano. - algoritmo de hash seguro de 256 bits" y se usa para la seguridad criptográfica"""
    return hashlib.sha256(password.encode()).hexdigest()

def create_access_token(data: dict, expires_delta: Optional[timedelta] = None) -> str:
    """Genera un token JWT firmado para un usuario."""
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(minutes=15)
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt

# --- FUNCIÓN PARA CARGAR EJERCICIOS DESDE JSON ---
def load_exercises_from_json(filename: str) -> List[Exercise]:
    """Lee un archivo JSON de la carpeta data y lo valida con el modelo Exercise."""
    base_path = os.path.dirname(os.path.abspath(__file__))
    file_path = os.path.join(base_path, "data", filename)
    
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            data = json.load(f)
            # Convertimos la lista de diccionarios en lista de objetos Exercise
            return [Exercise(**item) for item in data]
    except Exception as e:
        print(f"ERROR AL CARGAR {filename}: {str(e)}")
        return []

# Cargamos los ejercicios en memoria al arrancar la API para máxima velocidad
gym_exercises_list = load_exercises_from_json("exercises_gym_id.json")
home_exercises_list = load_exercises_from_json("exercises_home_id.json")

# OAuth2PasswordBearer: Define la URL donde se obtienen los tokens
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="login")

async def get_current_user(token: str = Depends(oauth2_scheme), db: AsyncSession = Depends(get_db)) -> DBUser:
    """Dependencia para validar el token en rutas protegidas."""
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="No se pudo validar el token de acceso",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        username: str = payload.get("sub")
        if username is None:
            raise credentials_exception
    except JWTError:
        raise credentials_exception
        
    result = await db.execute(select(DBUser).where(DBUser.username == username))
    user = result.scalars().first()
    
    if user is None:
        raise credentials_exception
    return user

async def get_current_active_user(current_user: DBUser = Depends(get_current_user)):
    """Verifica si el usuario está activo."""
    if getattr(current_user, 'disabled', False):
        raise HTTPException(status_code=400, detail="Usuario inactivo")
    return current_user

# =======================================================================
# CONFIGURACIÓN DE FASTAPI Y CORS
# =======================================================================

app = FastAPI(
    title="Kern API",
    description="API para el TFG de Kern - Sistema de Autenticación y Rutinas",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], 
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# =======================================================================
# EVENTOS DE LA APLICACIÓN
# =======================================================================

@app.on_event("startup")
def on_startup():
    """Se ejecuta al iniciar el servidor en Vercel"""
    create_db_and_tables_sync()
    print(f"- API Kern iniciada. Ejercicios cargados: Gym({len(gym_exercises_list)}), Home({len(home_exercises_list)})")

# ==============================================================
# CONFIGURACIÓN CIENTÍFICA DE RUTINAS AUTOMÁTICAS PREDEFINIDAS
# ==============================================================
ROUTINE_CONFIGS = {
    "Full Body": [
        {"muscle": "Pecho", "pattern": "Empuje horizontal"},
        {"muscle": "Espalda", "pattern": "Tracción vertical"},
        {"muscle": "Cuádriceps", "pattern": "Sentadilla"}, 
        {"muscle": "Hombros", "pattern": "Empuje vertical"},
        {"muscle": "Isquiotibiales", "pattern": "Bisagra de cadera"}
    ],
    "PPL (Push Pull Leg)": [
        # Push
        {"muscle": "Pecho", "pattern": "Empuje horizontal"},
        {"muscle": "Pecho superior", "pattern": "Empuje horizontal"},
        {"muscle": "Hombros", "pattern": "Empuje vertical"},
        # Pull
        {"muscle": "Espalda", "pattern": "Tracción vertical"},
        {"muscle": "Espalda media", "pattern": "Tracción horizontal"},
        {"muscle": "Bíceps", "pattern": "Aislamiento"},
        # Leg
        {"muscle": "Cuádriceps", "pattern": "Empuje de piernas"},
        {"muscle": "Isquiotibiales", "pattern": "Bisagra de cadera"}
    ],
    "Torso-Pierna": [
        {"muscle": "Pecho", "pattern": "Empuje horizontal"},
        {"muscle": "Pecho superior", "pattern": "Empuje horizontal"},
        {"muscle": "Espalda", "pattern": "Tracción vertical"},
        {"muscle": "Espalda media", "pattern": "Tracción horizontal"},
        {"muscle": "Cuádriceps", "pattern": "Sentadilla"},
        {"muscle": "Isquiotibiales", "pattern": "Bisagra de cadera"}
    ]
}    

# =======================================================================
# ENDPOINTS PÚBLICOS
# =======================================================================

@app.get("/")
def read_root():
    """Endpoint de cortesía para verificar que la API está online."""
    return {
        "message": "Bienvenido a Kern API",
        "version": "1.0.0",
        "docs": "/docs"
    }

@app.post("/register", response_model=LoginResponse)
async def register(user: UserCreate, db: AsyncSession = Depends(get_db)):
    """Registra un nuevo usuario y devuelve el token inmediatamente."""
    hashed_password = get_password_hash(user.password)
    
    new_user = DBUser(
        username=user.username,
        email=user.email,
        hashed_password=hashed_password,
        full_name=user.full_name,
        phone=user.phone,
        has_completed_quiz=False 
    )
    
    try:
        db.add(new_user)
        await db.commit()
        await db.refresh(new_user)
        access_token_expires = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
        access_token = create_access_token(
            data={"sub": new_user.username}, expires_delta=access_token_expires
        )
        return {
            "accessToken": access_token,
            "token_type": "bearer",
            "user": new_user
        }
    except IntegrityError:
        await db.rollback()
        raise HTTPException(status_code=400, detail="El usuario o email ya existe")

@app.post("/login", response_model=LoginResponse)
async def login(login_request: LoginRequest, db: AsyncSession = Depends(get_db)):
    """Autentica al usuario y devuelve token + datos de usuario."""
    result = await db.execute(select(DBUser).where(DBUser.email == login_request.email))
    user = result.scalars().first()
    
    if not user or not verify_password(login_request.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Email o contraseña incorrectos",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    access_token_expires = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    access_token = create_access_token(
        data={"sub": user.username}, expires_delta=access_token_expires
    )
    
    user_data = {c.name: getattr(user, c.name) for c in user.__table__.columns}
    
    return {
        "accessToken": access_token,
        "token_type": "bearer",
        "user": user_data
    }

# =======================================================================
# ENDPOINTS DE EJERCICIOS (Acceso Público para facilitar la App)
# =======================================================================

@app.get("/exercises/gym", response_model=List[Exercise])
async def get_gym_exercises():
    """Retorna la lista completa de ejercicios para gimnasio."""
    return gym_exercises_list

@app.get("/exercises/home", response_model=List[Exercise])
async def get_home_exercises():
    """Retorna la lista completa de ejercicios para casa."""
    return home_exercises_list

@app.get("/exercises/{exercise_id}", response_model=Exercise)
async def get_exercise_by_id(exercise_id: int):
    """Busca un ejercicio por su ID en ambas bases de datos."""
    all_ex = gym_exercises_list + home_exercises_list
    exercise = next((ex for ex in all_ex if ex.id == exercise_id), None)
    if not exercise:
        raise HTTPException(status_code=404, detail="Ejercicio no encontrado")
    return exercise

# =======================================================================
# ENDPOINTS PROTEGIDOS Y GESTIÓN DE RUTINAS
# =======================================================================

# FUNCIÓN AUXILIAR PARA RELLENAR LAS RUTINAS PREDEFINIDAS
def generate_routine_exercises(routine_type: str, all_exercises: list):
    """
    Busca ejercicios en el JSON que coincidan con la configuración.
    all_exercises es la lista cargada desde exercises_gym_id.json
    """
    config = ROUTINE_CONFIGS.get(routine_type, ROUTINE_CONFIGS["Full Body"])
    selected_exercises = []

    for item in config:
        target_muscle = item["muscle"].lower()
        target_pattern = item["pattern"].lower()

        # 1. Intentamos buscar el match perfecto (Músculo + Patrón)
        candidates = [
            ex for ex in all_exercises 
            if ex["primary_muscle"].lower() == target_muscle 
            and ex["movement_pattern"].lower() == target_pattern
        ]

        # 2. Si no hay match perfecto, buscamos solo por músculo (Fallback)
        if not candidates:
            candidates = [
                ex for ex in all_exercises 
                if ex["primary_muscle"].lower() == target_muscle
            ]

        if candidates:
            chosen = random.choice(candidates)
            selected_exercises.append({
                "nombre": chosen["name"],
                "series": [
                    {"numSerie": 1, "kilos": 0, "reps": 10},
                    {"numSerie": 2, "kilos": 0, "reps": 10},
                    {"numSerie": 3, "kilos": 0, "reps": 10}
                ]
            })
    
    return selected_exercises

@app.post("/users/complete-quiz", response_model=UserData)
async def complete_quiz(
    quiz_data: QuizCompletion, 
    current_user: DBUser = Depends(get_current_active_user), 
    db: AsyncSession = Depends(get_db)
):
    """Guarda la rutina asignada y genera automáticamente los ejercicios iniciales."""
    try:
        # 1. Buscamos al usuario actual
        result = await db.execute(select(DBUser).where(DBUser.id == current_user.id))
        db_user = result.scalars().first()
        
        if not db_user:
            raise HTTPException(status_code=404, detail="Usuario no encontrado")

        # 2. Actualizamos estado del usuario
        db_user.has_completed_quiz = True
        db_user.assigned_routine = quiz_data.assigned_routine

        # --- LÓGICA DE GENERACIÓN AUTOMÁTICA ---
        print(f"DEBUG: El usuario eligió: '{quiz_data.assigned_routine}'") 
        print(f"DEBUG: Claves disponibles en el dict: {list(ROUTINE_CONFIGS.keys())}") 
        
        # 3. Verificamos si la rutina enviada existe en nuestro diccionario
        if quiz_data.assigned_routine in ROUTINE_CONFIGS:
            print("DEBUG: ¡Coincidencia encontrada! Generando ejercicios...")
            # Cargamos los ejercicios del JSON
            base_path = os.path.dirname(os.path.abspath(__file__))
            file_path = os.path.join(base_path, "data", "exercises_gym_id.json")
            
            with open(file_path, "r", encoding="utf-8") as f:
                all_exercises = json.load(f)

            # 4. Creamos la cabecera de la rutina
            new_routine = Routine(
                user_id=db_user.id,
                name=f"Mi Rutina {quiz_data.assigned_routine} Inicial"
            )
            db.add(new_routine)
            await db.flush() # Para obtener el ID de la rutina

            # 5. ¡AQUÍ USAMOS TU FUNCIÓN! 
            ejercicios_generados = generate_routine_exercises(quiz_data.assigned_routine, all_exercises)

            # 6. Guardamos los ejercicios que nos ha devuelto la función
            for ex_data in ejercicios_generados:
                db_exercise = RoutineExercise(
                    routine_id=new_routine.id,
                    exercise_name=ex_data["nombre"],
                    series=ex_data["series"] # Ya vienen con sus 3 series a 0kg
                )
                db.add(db_exercise)
                print(f"--> Generado: {ex_data['nombre']}")
        else:
            print("DEBUG: ¡AVISO! El nombre no coincide con ninguna clave del diccionario.")    

        # Guardamos todo en la base de datos
        await db.commit()
        await db.refresh(db_user)
        return db_user

    except Exception as e:
        await db.rollback()
        print(f"Error en complete_quiz: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Error interno: {str(e)}")

@app.get("/users/me", response_model=UserData)
async def read_users_me(current_user: DBUser = Depends(get_current_active_user)):
    """Retorna la información del usuario autenticado."""
    return current_user

@app.put("/users/avatar", response_model=UserData)
async def update_avatar(
    avatar_data: AvatarUpdate,
    current_user: DBUser = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db)
):
    """Actualiza la URL del avatar del usuario."""
    try:
        current_user.avatar_url = avatar_data.avatar_url
        await db.commit()
        await db.refresh(current_user)
        return current_user
    except Exception as e:
        await db.rollback()
        raise HTTPException(status_code=500, detail=str(e))

@app.put("/users/update_name", response_model=LoginResponse)
async def update_username(
    username_data: UsernameUpdate,
    current_user: DBUser = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db)
):
    """Actualiza el nombre de usuario."""
    try:
        # Verificar si ya existe otro usuario con este nombre
        result = await db.execute(select(DBUser).where(DBUser.username == username_data.username))
        existing_user = result.scalars().first()
        if existing_user and existing_user.id != current_user.id:
            raise HTTPException(status_code=400, detail="El nombre de usuario ya está en uso")
            
        current_user.username = username_data.username
        await db.commit()
        await db.refresh(current_user)
        
        # --- NUEVO: Generar nuevo token con el nombre actualizado ---
        access_token_expires = timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
        access_token = create_access_token(
            data={"sub": current_user.username}, expires_delta=access_token_expires
        )
        user_data = {c.name: getattr(current_user, c.name) for c in current_user.__table__.columns}
        
        return {
            "accessToken": access_token,
            "token_type": "bearer",
            "user": user_data
        }
    except IntegrityError:
        await db.rollback()
        raise HTTPException(status_code=400, detail="El nombre de usuario ya está en uso")
    except Exception as e:
        await db.rollback()
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/users/stats", response_model=UserStats)
async def get_user_stats(
    current_user: DBUser = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db)
):
    """Calcula las estadísticas acumuladas del usuario (entrenamientos y pasos)."""
    # 1. Contar total de sesiones
    count_query = select(func.count(WorkoutSession.id)).where(WorkoutSession.user_id == current_user.id)
    count_result = await db.execute(count_query)
    total_workouts = count_result.scalar() or 0

    # 2. Sumar total de pasos
    steps_query = select(func.sum(WorkoutSession.steps)).where(WorkoutSession.user_id == current_user.id)
    steps_result = await db.execute(steps_query)
    total_steps = steps_result.scalar() or 0

    # 3. Obtener los días de entrenamiento únicos (YYYY-MM-DD)
    # Extraemos solo la fecha de la columna created_at
    days_query = select(func.distinct(func.date(WorkoutSession.created_at))).where(WorkoutSession.user_id == current_user.id)
    days_result = await db.execute(days_query)
    # Convertimos cada objeto date a string
    training_days = [d.strftime("%Y-%m-%d") if hasattr(d, "strftime") else str(d) for d in days_result.scalars().all() if d]

    return {
        "total_workouts": total_workouts,
        "total_steps": total_steps,
        "training_days": training_days
    }

@app.get("/users/my-routines")
async def get_my_routines(
    current_user: DBUser = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db)
):
    """Obtiene todas las rutinas del usuario actual con sus ejercicios."""
    result = await db.execute(
        select(Routine).where(Routine.user_id == current_user.id)
    )
    routines = result.scalars().all()
    
    response = []
    for r in routines:
        ex_result = await db.execute(
            select(RoutineExercise).where(RoutineExercise.routine_id == r.id)
        )
        exercises = ex_result.scalars().all()
        
        response.append({
            "id": r.id,
            "name": r.name,
            "created_at": r.created_at,
            "exercises": [
                {
                    "id": ex.id,
                    "name": ex.exercise_name,
                    "series": ex.series
                } for ex in exercises
            ]
        })
    
    return response

@app.post("/routines/create")
async def create_routine(
    routine_data: RoutineCreate, 
    db: AsyncSession = Depends(get_db),
    current_user: DBUser = Depends(get_current_active_user)
):
    """Registra una rutina completa con sus ejercicios y series en una sola transacción."""
    try:
        new_routine = Routine(
            user_id=current_user.id, 
            name=routine_data.nombre
        )
        db.add(new_routine)
        await db.flush()

        for ex_data in routine_data.ejercicios:
            series_list = [s.model_dump() if hasattr(s, 'model_dump') else s.dict() for s in ex_data.series]
            
            new_exercise = RoutineExercise(
                routine_id=new_routine.id,
                exercise_name=ex_data.nombre,
                series=series_list
            )
            db.add(new_exercise)
        
        await db.commit()
        
        return {
            "status": "success", 
            "message": "Rutina y ejercicios guardados correctamente", 
            "routine_id": new_routine.id
        }

    except Exception as e:
        await db.rollback()
        print(f"[DEBUG ERROR] Fallo al crear rutina: {str(e)}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, 
            detail=f"Error en el servidor al procesar la rutina: {str(e)}"
        )

@app.put("/routines/{routine_id}")
async def update_routine(
    routine_id: int,
    routine_data: RoutineCreate, 
    db: AsyncSession = Depends(get_db),
    current_user: DBUser = Depends(get_current_active_user)
):
    """Actualiza una rutina existente: cambia el nombre y reemplaza los ejercicios."""
    try:
        result = await db.execute(
            select(Routine).where(Routine.id == routine_id, Routine.user_id == current_user.id)
        )
        db_routine = result.scalars().first()
        
        if not db_routine:
            raise HTTPException(status_code=404, detail="Rutina no encontrada")

        old_name = db_routine.name
        db_routine.name = routine_data.nombre

        # Actualizar historial para no perder el tracking al renombrar
        if old_name != routine_data.nombre:
            sessions_result = await db.execute(
                select(WorkoutSession).where(
                    WorkoutSession.user_id == current_user.id,
                    WorkoutSession.routine_name == old_name
                )
            )
            for s in sessions_result.scalars().all():
                s.routine_name = routine_data.nombre

        # Verificar si el payload contiene ejercicios con datos reales
        has_real_data = False
        for ex in routine_data.ejercicios:
            for s in ex.series:
                if s.kilos > 0 or s.reps > 0:
                    has_real_data = True
                    break
            if has_real_data:
                break

        # Si el payload envía ejercicios vacíos (todo a 0), asumimos que es solo un renombrado desde la app
        is_empty_rename = not has_real_data and len(routine_data.ejercicios) > 0

        if not is_empty_rename:
            await db.execute(
                delete(RoutineExercise).where(RoutineExercise.routine_id == routine_id)
            )
    
            for ex_data in routine_data.ejercicios:
                series_list = [s.model_dump() if hasattr(s, 'model_dump') else s.dict() for s in ex_data.series]
                
                new_exercise = RoutineExercise(
                    routine_id=routine_id,
                    exercise_name=ex_data.nombre,
                    series=series_list
                )
                db.add(new_exercise)
        
        await db.commit()
        
        return {"status": "success", "message": "Rutina actualizada correctamente"}

    except Exception as e:
        await db.rollback()
        print(f"[DEBUG ERROR] Fallo al editar rutina: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/routines/{routine_id}")
async def get_routine_detail(
    routine_id: int,
    current_user: DBUser = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db)
):
    """Obtiene el detalle profundo de una rutina incluyendo cálculos de volumen y datos históricos."""
    
    result = await db.execute(
        select(Routine).where(Routine.id == routine_id, Routine.user_id == current_user.id)
    )
    routine = result.scalars().first()
    
    if not routine:
        raise HTTPException(status_code=404, detail="Rutina no encontrada")

    print(f"\n>>> DEBUG: Buscando historial para rutina: '{routine.name}' (User ID: {current_user.id})")
    # --- CAMBIO CRÍTICO: Filtramos por routine_name para asegurar la progresión correcta ---
    # Usamos .ilike() y .strip() para evitar errores por espacios extra o mayúsculas
    session_result = await db.execute(
        select(WorkoutSession)
        .where(
            WorkoutSession.user_id == current_user.id,
            WorkoutSession.routine_name.ilike(routine.name.strip()) # Comparación robusta de nombre
        )
        .order_by(WorkoutSession.created_at.desc())
    )
    last_session = session_result.scalars().first()

    if last_session:
        print(f">>> DEBUG: Sesión encontrada! ID: {last_session.id} del {last_session.created_at}")
    else:
        print(f">>> DEBUG: No se encontró ninguna sesión previa para esta rutina.")

    ex_result = await db.execute(
        select(RoutineExercise).where(RoutineExercise.routine_id == routine.id)
    )
    exercises = ex_result.scalars().all()

    total_series = 0
    total_volume = 0.0
    formatted_exercises = []

    #Unificamos las listas de JSON en una sola para buscar más fácil
    all_json_exercises = gym_exercises_list + home_exercises_list

    for ex in exercises:
        # En lugar de hacer select a la DB (que daba error), buscamos en la lista del JSON
        # Normalizamos con .strip() por seguridad
        master_ex = next((e for e in all_json_exercises if e.name.lower().strip() == ex.exercise_name.lower().strip()), None)

        # Buscamos las series de este ejercicio en la última sesión para comparar por índice
        h_series_encontradas = []
        if last_session and last_session.data_json:
            nombre_e = ex.exercise_name.lower().strip()
            for h_ex in last_session.data_json:
                # Buscamos el nombre en el JSON, normalizando para evitar fallos de coincidencia
                nombre_h = (h_ex.get("name") or h_ex.get("exercise_name") or h_ex.get("nombre") or "").lower().strip()
                
                # Usamos una comparación que cubra si un nombre contiene al otro (por si hay truncado visual)
                if nombre_h == nombre_e or (nombre_h != "" and nombre_h in nombre_e) or (nombre_e in nombre_h):
                    h_series_encontradas = h_ex.get("series", [])
                    print(f">>> DEBUG: Ejercicio '{ex.exercise_name}' coincide con historial. Series halladas: {len(h_series_encontradas)}")
                    break

        # Corregido: Usamos ex.series que es donde están los datos de la DB
        num_series_ex = len(ex.series)
        total_series += num_series_ex
        
        series_con_historial = []
        
        # --- NUEVA LÓGICA DE EMPAREJAMIENTO ROBUSTA PARA PROGRESIÓN ---
        # Construimos mapa de historial para búsqueda O(1) con normalización de tipos
        historial_map = {}
        for h_s in h_series_encontradas:
            try:
                ns_raw = h_s.get("numSerie")
                if ns_raw is not None:
                    ns_int = int(ns_raw)
                    if ns_int > 0:
                        historial_map[ns_int] = h_s
            except (ValueError, TypeError):
                continue # Ignoramos entradas con numSerie corrupto

        # Usamos enumerate para identificar cada serie por su posición (i)
        for i, s in enumerate(ex.series):
            # Para el cálculo del volumen inicial de la respuesta, pero mandaremos 0 a Android
            kilos_base = float(s.get("kilos", 0) or 0)
            reps_base = int(s.get("reps", 0) or 0)
            
            historia_texto = "--"
            pk, pr = 0.0, 0
            s_hist = None
            
            # PRIORIDAD 1: Match por numSerie (Normalizado y Validado)
            try:
                current_ns_raw = s.get("numSerie")
                if current_ns_raw is not None:
                    ns_int = int(current_ns_raw)
                    if ns_int > 0:
                        s_hist = historial_map.get(ns_int)
            except (ValueError, TypeError):
                s_hist = None

            # PRIORIDAD 2: Fallback por índice (Compatibilidad total con sesiones antiguas)
            if not s_hist and i < len(h_series_encontradas):
                s_hist = h_series_encontradas[i]
            
            if s_hist:
                pk = float(s_hist.get('kilos', 0) or 0)
                pr = int(s_hist.get('reps', 0) or 0)
                historia_texto = f"{pk}kg x {pr}"
            
            serie_data = s.copy()
            # CAMBIO CRÍTICO PARA PROGRESIÓN: Enviamos 0 para que Android use el historial como sugerencia
            serie_data["kilos"] = 0 
            serie_data["reps"] = 0
            serie_data["anterior"] = historia_texto 
            serie_data["prev_kilos"] = pk # Dato numérico para los EditText de Android
            serie_data["prev_reps"] = pr   # Dato numérico para los EditText de Android
            series_con_historial.append(serie_data)

        formatted_exercises.append({
            "id": ex.id,
            "name": ex.exercise_name,
            "instructions": master_ex.instructions if master_ex else "No hay instrucciones disponibles",
            "pro_tips": master_ex.pro_tips if master_ex else [],
            "common_mistakes": master_ex.common_mistakes if master_ex else [],
            "series": series_con_historial
        })

    return {
        "id": routine.id,
        "name": routine.name,
        "created_at": routine.created_at,
        "total_exercises": len(exercises),
        "total_series": total_series,
        "total_volume": 0.0, # Empezamos sesión con volumen 0
        "exercises": formatted_exercises
    }

@app.delete("/routines/{routine_id}")
async def delete_routine(
    routine_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: DBUser = Depends(get_current_active_user)
):
    """Elimina una rutina y todos sus ejercicios asociados."""
    try:
        result = await db.execute(
            select(Routine).where(Routine.id == routine_id, Routine.user_id == current_user.id)
        )
        db_routine = result.scalars().first()

        if not db_routine:
            raise HTTPException(
                status_code=404, 
                detail="Rutina no encontrada o no tienes permiso para eliminarla"
            )

        await db.delete(db_routine)
        await db.commit()

        return {"status": "success", "message": "Rutina eliminada correctamente"}

    except Exception as e:
        await db.rollback()
        print(f"[DEBUG ERROR] Fallo al eliminar rutina: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Error interno: {str(e)}")

@app.post("/workouts/finish")
async def finish_workout(
    workout_data: WorkoutSessionCreate,
    db: AsyncSession = Depends(get_db),
    current_user: DBUser = Depends(get_current_active_user)
):
    """Registra una sesión de entrenamiento terminada en el historial."""
    try:
        new_session = WorkoutSession(
            user_id=current_user.id,
            routine_name=workout_data.routine_name,
            duration_seconds=workout_data.duration_seconds,
            total_volume=workout_data.total_volume,
            steps=workout_data.steps,
            data_json=workout_data.data_json
        )
        
        db.add(new_session)
        await db.commit()
        
        return {"status": "success", "message": "Entrenamiento guardado en el historial"}
    except Exception as e:
        await db.rollback()
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/users/my-history")
async def get_history(
    current_user: DBUser = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db)
):
    result = await db.execute(
        select(WorkoutSession)
        .where(WorkoutSession.user_id == current_user.id)
        .order_by(WorkoutSession.created_at.desc())
    )
    return result.scalars().all()

# =======================================================================
# EJECUCIÓN DEL SERVIDOR
# =======================================================================

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8002)