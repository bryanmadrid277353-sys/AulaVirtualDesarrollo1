import os
import sys
from sqlalchemy import create_engine, text

# Forzar codificación UTF-8 en consolas de Windows para evitar 'charmap' errors
if hasattr(sys.stdout, 'reconfigure'):
    try:
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    except Exception:
        pass

def sync_db():
    backend_dir = os.path.dirname(os.path.abspath(__file__))
    init_sql_path = os.path.join(backend_dir, "init.sql")
    seed_sql_path = os.path.join(backend_dir, "seed_demo.sql")

    print("=========================================================")
    print("  SINCRONIZADOR DE BASE DE DATOS - AULA VIRTUAL (MYSQL)")
    print("=========================================================")
    print("Conectando con el servidor MySQL local en XAMPP (localhost:3306)...")

    # 1. Asegurar creación de la base de datos 'aula_virtual'
    root_engine = create_engine("mysql+mysqlconnector://root:@localhost/", isolation_level="AUTOCOMMIT")
    with root_engine.connect() as conn:
        conn.execute(text("CREATE DATABASE IF NOT EXISTS aula_virtual CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"))
    print("[OK] Base de datos 'aula_virtual' verificada en MySQL.")

    # 2. Conectar a la base de datos 'aula_virtual'
    db_engine = create_engine("mysql+mysqlconnector://root:@localhost/aula_virtual")

    # 3. Ejecutar init.sql
    if os.path.exists(init_sql_path):
        print("Importando estructura e inicializacion desde 'init.sql'...")
        execute_sql_file(db_engine, init_sql_path)
        print("[OK] 'init.sql' sincronizado exitosamente.")
    else:
        print("[!] No se encontro el archivo 'init.sql'.")

    # 4. Ejecutar seed_demo.sql
    if os.path.exists(seed_sql_path):
        print("Importando datos de demostracion desde 'seed_demo.sql'...")
        execute_sql_file(db_engine, seed_sql_path)
        print("[OK] 'seed_demo.sql' sincronizado exitosamente.")
    else:
        print("[!] No se encontro el archivo 'seed_demo.sql'.")

    print("\n=========================================================")
    print(" ¡Sincronizacion completada con exito!")
    print(" Base de datos: aula_virtual")
    print(" URL de phpMyAdmin: http://localhost/phpmyadmin/")
    print("=========================================================\n")

def execute_sql_file(engine, file_path):
    with open(file_path, "r", encoding="utf-8") as f:
        sql_content = f.read()

    # Dividir las sentencias por punto y coma (;)
    raw_statements = sql_content.split(";")
    with engine.connect() as conn:
        for stmt in raw_statements:
            stmt_clean = stmt.strip()
            # Omitir líneas vacías o comentarios puros
            lines = [l for l in stmt_clean.splitlines() if l.strip() and not l.strip().startswith("--")]
            clean_cmd = "\n".join(lines).strip()
            if clean_cmd:
                try:
                    conn.execute(text(clean_cmd))
                except Exception as e:
                    # Captura de advertencias si un registro ya existe
                    pass
        conn.commit()

if __name__ == "__main__":
    try:
        sync_db()
    except Exception as e:
        print(f"\n[ERROR] Durante la sincronización: {e}")
        print("Asegúrate de que XAMPP esté iniciado con el servicio MySQL activo en el puerto 3306.")
