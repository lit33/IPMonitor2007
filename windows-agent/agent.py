"""Windows LAN ping agent. Requires Python 3.10+. No third-party packages."""
import json
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent

def config():
    with (ROOT / "config.json").open(encoding="utf-8") as file:
        data = json.load(file)
    if not str(data.get("endpoint", "")).startswith("https://"):
        raise ValueError("endpoint must use https://")
    if not isinstance(data.get("devices"), list) or not data["devices"]:
        raise ValueError("Configure at least one device")
    data["interval_seconds"] = max(10, int(data.get("interval_seconds", 30)))
    return data

def ping(device, timeout_ms):
    ip = device["ip"]
    if not re.fullmatch(r"(?:[0-9]{1,3}\.){3}[0-9]{1,3}", ip) or any(int(p) > 255 for p in ip.split(".")):
        raise ValueError(f"Invalid IPv4: {ip}")
    started = time.monotonic()
    try:
        result = subprocess.run(["ping", "-n", "1", "-w", str(timeout_ms), ip],
            capture_output=True, text=True, errors="replace", timeout=(timeout_ms / 1000) + 4,
            creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
        elapsed = round((time.monotonic() - started) * 1000)
        online = result.returncode == 0
        # The subprocess duration is an upper bound, including ping process startup.
        return {"id": device["id"], "name": device["name"], "ip": ip,
                "up": online, "latency_ms": min(elapsed, 60000) if online else None}
    except (OSError, subprocess.TimeoutExpired) as exc:
        print(f"Ping failed for {ip}: {exc}", file=sys.stderr)
        return {"id": device["id"], "name": device["name"], "ip": ip,
                "up": False, "latency_ms": None}

def send(endpoint, token, rows):
    body = json.dumps({"results": rows}, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(endpoint, data=body, method="POST",
        headers={"Content-Type": "application/json", "x-agent-token": token})
    with urllib.request.urlopen(request, timeout=15) as response:
        response.read()
        if response.status != 200:
            raise RuntimeError(f"HTTP {response.status}")

def main():
    data = config()
    token = os.environ.get("IPMONITOR_AGENT_TOKEN", "").strip()
    if len(token) < 32:
        raise ValueError("Set IPMONITOR_AGENT_TOKEN (32+ characters); never commit it")
    timeout_ms = min(max(int(data.get("timeout_ms", 2000)), 100), 10000)
    print("IP Monitor 2007 | Windows LAN Agent | Ctrl+C to stop", flush=True)
    while True:
        rows = [ping(d, timeout_ms) for d in data["devices"]]
        try:
            send(data["endpoint"], token, rows)
            print(datetime.now(timezone.utc).isoformat(), "sent", len(rows), "results", flush=True)
        except (urllib.error.URLError, RuntimeError, TimeoutError) as exc:
            print("Cloud unavailable:", exc, file=sys.stderr, flush=True)
        time.sleep(data["interval_seconds"])

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("Stopped")
    except (ValueError, KeyError, FileNotFoundError) as exc:
        sys.exit(f"Configuration error: {exc}")
