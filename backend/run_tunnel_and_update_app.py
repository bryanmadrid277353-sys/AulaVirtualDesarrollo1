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

def update_local_properties(tunnel_url):
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    local_props_file = os.path.join(project_root, "local.properties")

    if not os.path.exists(local_props_file):
        content = f"backend.url.debug={tunnel_url}\nbackend.url.release=https://api.tudominio.com/\n"
        with open(local_props_file, "w", encoding="utf-8") as f:
            f.write(content)
    else:
        with open(local_props_file, "r", encoding="utf-8") as f:
            content = f.read()

        pattern = r'(?m)^backend\.url\.debug\s*=\s*.*$'
        new_line = f'backend.url.debug={tunnel_url}'

        if re.search(pattern, content):
            new_content = re.sub(pattern, new_line, content)
        else:
            new_content = content.rstrip() + f"\nbackend.url.debug={tunnel_url}\n"

        with open(local_props_file, "w", encoding="utf-8") as f:
            f.write(new_content)

    print("\n===================================================", flush=True)
    print("[SUCCESS] local.properties (backend.url.debug) actualizado automaticamente!", flush=True)
    print(f"[PUBLIC BASE_URL] {tunnel_url}", flush=True)
    print("===================================================\n", flush=True)
    return True

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
                    url_updated = update_local_properties(tunnel_url)
                    print("[i] Túnel activo y operando en segundo plano.", flush=True)

        process.wait()
    except Exception as e:
        print(f"\n[!] Cloudflare Tunnel no se pudo iniciar: {e}", flush=True)
        print("[i] FastAPI sigue funcionando normalmente en http://127.0.0.1:8000 (y http://10.0.2.2:8000 para emulador).\n", flush=True)

if __name__ == "__main__":
    main()
