# database.py

from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine
from typing import AsyncGenerator
from sqlalchemy.orm import declarative_base, sessionmaker
from sqlalchemy import create_engine 
import os

# Cargar variables de entorno desde archivo local (si existe)
from dotenv import load_dotenv
load_dotenv("database.env", override=False)

# --- ZONA DE CONFIGURACIÓN ---

DATABASE_URL = os.getenv("DATABASE_URL")

if not DATABASE_URL:
    print("[ERROR] DATABASE_URL no encontrada. Usando URL de desarrollo.")
    DATABASE_URL = "postgresql+asyncpg://user:pass@localhost/db"

# 1. Creación del motor asíncrono
import ssl

ssl_context = ssl.create_default_context()
ssl_context.check_hostname = False
ssl_context.verify_mode = ssl.CERT_NONE

# Limpiar la URL de parámetros SSL incompatibles con asyncpg
clean_url = DATABASE_URL.replace("?sslmode=require", "").replace("?ssl=require", "").replace("&sslmode=require", "").replace("&ssl=require", "")

engine = create_async_engine(
    clean_url, 
    echo=True,
    pool_size=5,
    max_overflow=10,
    pool_pre_ping=True,
    pool_recycle=300,
    connect_args={"ssl": ssl_context}
) 

# 2. Base para todos nuestros modelos de tabla
Base = declarative_base()

# 3. Sesión asíncrona
AsyncSessionLocal = sessionmaker(
    engine, class_=AsyncSession, expire_on_commit=False
)

# 4. Dependencia para inyectar la sesión DB en las rutas de FastAPI
async def get_db() -> AsyncGenerator[AsyncSession, None]:
    async with AsyncSessionLocal() as session:
        yield session

# 5. Función Síncrona para crear las tablas (Corregida para evitar error SSL)
def create_db_and_tables_sync(): 
    """Crea todas las tablas de forma síncrona limpiando la URL de Neon."""
    from models import User, Routine, RoutineExercise  # Importamos los modelos para que se registren en la metadata
    
    # 1. Cambiamos el driver de asyncpg a postgresql estándar
    sync_url = DATABASE_URL.replace("postgresql+asyncpg", "postgresql")
    
    # 2. Limpiamos cualquier parámetro de SSL que venga en la URL (como sslmode)
    # Esto evita el error "invalid connection option ssl"
    if "?" in sync_url:
        sync_url = sync_url.split("?")[0]
    
    # 3. Creamos el motor síncrono pasando el SSL de forma que psycopg2 lo entienda
    sync_engine = create_engine(
        sync_url, 
        echo=False,
        connect_args={"sslmode": "require"} # Este es el formato que acepta psycopg2
    )

    try:
        Base.metadata.create_all(sync_engine, checkfirst=True)
        print("[INFO] Tablas de base de datos listas y verificadas.")
    except Exception as e:
        print(f"[ERROR] No se pudieron crear las tablas: {e}")