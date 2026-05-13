# api/index.py
# Punto de entrada para Vercel Serverless Functions

import sys
import os

# Añadir el directorio raíz al path para que pueda importar los módulos
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Importar la app de FastAPI desde app.py
from app import app

# Vercel busca una variable 'app' o 'handler'
# FastAPI es compatible con ASGI, así que solo la exportamos
