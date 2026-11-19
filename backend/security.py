"""
Aula Virtual - Módulo de seguridad
==================================
Usa solo la librería estándar de Python (no requiere instalar nada).

Incluye:
  1. Hash de contraseñas (PBKDF2-SHA256 con sal aleatoria).
  2. Tokens de sesión firmados (HMAC-SHA256) con fecha de expiración.
  3. Política de acceso por rol (tabla ACCESS_RULES, fácil de editar).
  4. Límite de intentos de inicio de sesión (anti fuerza bruta).

Variables de entorno (todas opcionales):
  AULA_AUTH_MODE      "monitor" (por defecto) o "enforce".
                        monitor : NO bloquea nada, solo avisa en consola de las
                                  peticiones que se bloquearían. Compatible con
                                  la app Android actual.
                        enforce : exige token válido y respeta la tabla de roles.
  AULA_SECRET_KEY     Clave para firmar tokens. Si no existe se genera sola y se
                      guarda en el archivo ".aula_secret" junto a main.py.
  AULA_TOKEN_HOURS    Duración del token en horas (por defecto 72).
  AULA_ADMIN_PASSWORD Contraseña inicial del Admin Maestro SI hay que crearlo.
  AULA_STUDENT_DEFAULT_PASSWORD  Contraseña genérica de estudiantes (por defecto ASD###).
"""

import base64
import hashlib
import hmac
import json
import os
import re
import secrets
import threading
import time
from typing import Optional, Set

# ---------------------------------------------------------------------------
# Configuración
# ---------------------------------------------------------------------------
AUTH_MODE = os.getenv("AULA_AUTH_MODE", "monitor").strip().lower()
if AUTH_MODE not in ("monitor", "enforce"):
    AUTH_MODE = "monitor"

TOKEN_HOURS = float(os.getenv("AULA_TOKEN_HOURS", "72"))
STUDENT_DEFAULT_PASSWORD = os.getenv("AULA_STUDENT_DEFAULT_PASSWORD", "ASD###")
MIN_PASSWORD_LENGTH = 6

_BASE_DIR = os.path.dirname(os.path.abspath(__file__))


def _b64e(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def _b64d(text: str) -> bytes:
    return base64.urlsafe_b64decode(text + "=" * (-len(text) % 4))


def _load_secret() -> bytes:
    env_key = os.getenv("AULA_SECRET_KEY")
    if env_key:
        return env_key.encode("utf-8")
    key_file = os.path.join(_BASE_DIR, ".aula_secret")
    try:
        with open(key_file, "r", encoding="utf-8") as f:
            stored = f.read().strip()
            if stored:
                return stored.encode("utf-8")
    except FileNotFoundError:
        pass
    new_key = secrets.token_hex(32)
    try:
        with open(key_file, "w", encoding="utf-8") as f:
            f.write(new_key)
    except OSError:
        pass  # Sin permisos de escritura: la clave vive solo en memoria (los tokens caducan al reiniciar)
    return new_key.encode("utf-8")


_SECRET = _load_secret()

# ---------------------------------------------------------------------------
# 1. Contraseñas
# ---------------------------------------------------------------------------
_HASH_PREFIX = "pbkdf2_sha256"
_ITERATIONS = 240_000


def is_hashed(stored: Optional[str]) -> bool:
    return bool(stored) and stored.startswith(_HASH_PREFIX + "$")


def hash_password(plain: str) -> str:
    salt = secrets.token_bytes(16)
    digest = hashlib.pbkdf2_hmac("sha256", plain.encode("utf-8"), salt, _ITERATIONS)
    return f"{_HASH_PREFIX}${_ITERATIONS}${_b64e(salt)}${_b64e(digest)}"


def verify_password(stored: Optional[str], plain: str) -> bool:
    """Acepta hashes nuevos y contraseñas antiguas en texto plano (migración transparente).
    La comparación distingue mayúsculas/minúsculas."""
    if not stored or plain is None:
        return False
    if is_hashed(stored):
        try:
            _, iters, salt_b64, hash_b64 = stored.split("$")
            digest = hashlib.pbkdf2_hmac("sha256", plain.encode("utf-8"), _b64d(salt_b64), int(iters))
            return hmac.compare_digest(digest, _b64d(hash_b64))
        except Exception:
            return False
    return hmac.compare_digest(stored.strip().encode("utf-8"), plain.strip().encode("utf-8"))


# Hash falso para igualar el tiempo de respuesta cuando el usuario no existe
_DUMMY_HASH = hash_password(secrets.token_hex(8))


def burn_time(plain: str) -> None:
    verify_password(_DUMMY_HASH, plain)


def password_problem(plain: Optional[str]) -> Optional[str]:
    if not plain or len(plain.strip()) < MIN_PASSWORD_LENGTH:
        return f"La contraseña debe tener al menos {MIN_PASSWORD_LENGTH} caracteres."
    return None


# ---------------------------------------------------------------------------
# 2. Tokens firmados
# ---------------------------------------------------------------------------
def create_token(uid: int, role: str, carnet: str, src: str = "user") -> str:
    """src = 'user' (tabla users) o 'student' (tabla students): sus ids son independientes."""
    payload = {
        "uid": uid,
        "role": (role or "").upper(),
        "carnet": carnet,
        "src": src,
        "exp": int(time.time() + TOKEN_HOURS * 3600),
    }
    body = _b64e(json.dumps(payload, separators=(",", ":")).encode("utf-8"))
    sig = _b64e(hmac.new(_SECRET, body.encode("ascii"), hashlib.sha256).digest())
    return f"{body}.{sig}"


def verify_token(token: Optional[str]) -> Optional[dict]:
    if not token or "." not in token:
        return None
    try:
        body, sig = token.split(".", 1)
        expected = _b64e(hmac.new(_SECRET, body.encode("ascii"), hashlib.sha256).digest())
        if not hmac.compare_digest(sig, expected):
            return None
        payload = json.loads(_b64d(body))
        if payload.get("exp", 0) < time.time():
            return None
        return payload
    except Exception:
        return None


def token_from_header(authorization: Optional[str]) -> Optional[str]:
    if authorization and authorization.lower().startswith("bearer "):
        return authorization[7:].strip()
    return None


# ---------------------------------------------------------------------------
# 3. Política de acceso por rol
# ---------------------------------------------------------------------------
ADMIN = {"ADMIN"}
STAFF = {"ADMIN", "PROFESSOR"}
ADMIN_ENCARGADO = {"ADMIN", "ENCARGADO"}

# Rutas que no necesitan token
PUBLIC_EXACT = {"/", "/health", "/api-status", "/login", "/docs", "/redoc", "/openapi.json", "/favicon.ico"}
PUBLIC_PREFIXES = ("/static/",)

# (métodos, patrón de ruta, roles permitidos)
# Todo lo que NO aparece aquí exige únicamente estar autenticado (cualquier rol).
# EDITA ESTA TABLA para ajustar permisos sin tocar el resto del código.
ACCESS_RULES = [
    ({"GET"},                     r"^/users/(admins|professors|encargados|students)$", STAFF),
    ({"GET"},                     r"^/students$",                             STAFF),
    ({"POST"},                    r"^/users$",                                ADMIN),
    ({"DELETE"},                  r"^/users/\d+$",                            ADMIN),
    # PUT /users/{id}: cualquiera autenticado; el endpoint limita a "solo su propio perfil" salvo ADMIN
    ({"POST", "DELETE"},          r"^/students(/\d+)?$",                      ADMIN),
    ({"PUT"},                     r"^/students/\d+$",                         ADMIN_ENCARGADO),
    ({"POST", "PUT", "DELETE"},   r"^/subjects(/\d+)?$",                      ADMIN),
    ({"POST", "DELETE"},          r"^/enrollments(/\d+)?$",                   ADMIN),
    ({"PUT"},                     r"^/enrollments/\d+$",                      STAFF),
    ({"POST", "PUT", "DELETE"},   r"^/facilities(/\d+)?$",                    ADMIN),
    ({"POST", "PUT", "DELETE"},   r"^/facilities/schedules(/\d+)?$",          STAFF),
    ({"POST"},                    r"^/(academic-periods|non-school-days)$",   ADMIN),
    ({"DELETE"},                  r"^/non-school-days/\d+$",                  ADMIN),
    ({"PUT", "DELETE"},           r"^/notifications(/\d+)?$",                 ADMIN),
    ({"POST"},                    r"^/(attendance-records|conduct-records)$", STAFF),
    ({"POST"},                    r"^/(grade-import-logs|modification-logs)$", STAFF),
]
_COMPILED_RULES = [(m, re.compile(p), roles) for m, p, roles in ACCESS_RULES]


def is_public(path: str) -> bool:
    return path in PUBLIC_EXACT or path.startswith(PUBLIC_PREFIXES)


def required_roles(method: str, path: str) -> Optional[Set[str]]:
    """None = basta con estar autenticado. Un conjunto = solo esos roles."""
    for methods, pattern, roles in _COMPILED_RULES:
        if method in methods and pattern.match(path):
            return roles
    return None


# ---------------------------------------------------------------------------
# 4. Límite de intentos de inicio de sesión
# ---------------------------------------------------------------------------
class LoginLimiter:
    """Bloquea temporalmente tras demasiados fallos (por cuenta y por IP)."""

    def __init__(self, max_per_account=5, max_per_ip=25, window_seconds=300):
        self.max_per_account = max_per_account
        self.max_per_ip = max_per_ip
        self.window = window_seconds
        self._fails = {}
        self._lock = threading.Lock()

    def _prune(self, key, now):
        self._fails[key] = [t for t in self._fails.get(key, []) if now - t < self.window]
        if not self._fails[key]:
            self._fails.pop(key, None)
            return []
        return self._fails[key]

    def seconds_blocked(self, ip: str, account: str) -> int:
        now = time.time()
        with self._lock:
            for key, limit in ((f"acc:{account}", self.max_per_account), (f"ip:{ip}", self.max_per_ip)):
                hits = self._prune(key, now)
                if len(hits) >= limit:
                    return int(self.window - (now - hits[0])) + 1
        return 0

    def register_failure(self, ip: str, account: str) -> None:
        now = time.time()
        with self._lock:
            for key in (f"acc:{account}", f"ip:{ip}"):
                self._fails.setdefault(key, []).append(now)

    def reset(self, account: str) -> None:
        with self._lock:
            self._fails.pop(f"acc:{account}", None)


login_limiter = LoginLimiter()
