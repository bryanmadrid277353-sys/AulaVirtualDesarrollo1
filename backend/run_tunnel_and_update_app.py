import os
import re
import subprocess
import sys

# Reconfigurar codificación y forzar line buffering en sys.stdout
if hasattr(sys.stdout, 'reconfigure'):
    try:
        sys.stdout.reconfigure(encoding='utf-8', errors='replace', line_buffering=True)
    except Exception:
        pass

def update_retrofit_client(tunnel_url):
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    retrofit_file = os.path.join(
        project_root,
        "app", "src", "main", "java", "com", "aula", "virtual", "data", "RetrofitClient.java"
    )

    if not os.path.exists(retrofit_file):
        print(f"[!] No se encontro RetrofitClient.java en: {retrofit_file}", flush=True)
        return False

    with open(retrofit_file, "r", encoding="utf-8") as f:
        content = f.read()

    pattern = r'(public|private)\s+static\s+final\s+String\s+BASE_URL\s*=\s*"(https?://[^"]+)";'
    new_base_url = f'public static final String BASE_URL = "{tunnel_url}";'

    if re.search(pattern, content):
        new_content = re.sub(pattern, new_base_url, content)
        with open(retrofit_file, "w", encoding="utf-8") as f:
            f.write(new_content)
        print("\n===================================================", flush=True)
        print("[SUCCESS] RetrofitClient.java actualizado automaticamente!", flush=True)
        print(f"[PUBLIC BASE_URL] {tunnel_url}", flush=True)
        print("===================================================\n", flush=True)
        return True
    else:
        print("[!] No se pudo encontrar el patron BASE_URL en RetrofitClient.java", flush=True)
        return False

def main():
    backend_dir = os.path.dirname(os.path.abspath(__file__))
    cloudflared_exe = os.path.join(backend_dir, "cloudflared.exe")

    if not os.path.exists(cloudflared_exe):
        cloudflared_exe = "cloudflared"

    cmd = [
        cloudflared_exe, "tunnel",
        "--protocol", "http2",
        "--edge-ip-version", "4",
        "--no-autoupdate",
        "--url", "http://127.0.0.1:8000"
    ]

    print("===================================================", flush=True)
    print("  AULA VIRTUAL - CLOUDFLARE TUNNEL", flush=True)
    print("===================================================", flush=True)
    print("[i] Iniciando Cloudflare Tunnel...", flush=True)

    try:
        process = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
            universal_newlines=True
        )

        url_updated = False
        url_pattern = re.compile(r'https://[a-zA-Z0-9\-]+\.trycloudflare\.com')

        # Ocultar la salida verbosa (INF, prechecks, etc.) y capturar únicamente la URL pública
        for line in iter(process.stdout.readline, ''):
            if not url_updated:
                match = url_pattern.search(line)
                if match:
                    tunnel_url = match.group(0)
                    if not tunnel_url.endswith("/"):
                        tunnel_url += "/"
                    url_updated = update_retrofit_client(tunnel_url)
                    print("[i] Túnel activo y operando en segundo plano.", flush=True)

        process.wait()
    except Exception as e:
        print(f"\n[!] Cloudflare Tunnel no se pudo iniciar: {e}", flush=True)
        print("[i] FastAPI sigue funcionando normalmente en http://127.0.0.1:8000 (y http://10.0.2.2:8000 para emulador).\n", flush=True)

if __name__ == "__main__":
    main()
