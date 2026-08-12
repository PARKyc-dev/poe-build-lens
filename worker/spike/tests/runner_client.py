import json
from pathlib import Path
import queue
import subprocess
import threading
import time


MARKER = "POE_LENS_JSON\t"


class PobRunnerError(RuntimeError):
    pass


class PobRunner:
    def __init__(self, spike: Path, timeout_seconds: float = 120):
        self.spike = spike
        self.timeout_seconds = timeout_seconds
        self.process = None
        self.lines = queue.Queue()
        self.diagnostics = []
        self.next_id = 1
        self.reader_thread = None
        self.ready = None

    def __enter__(self):
        source = self.spike / ".cache" / "PathOfBuilding" / "src"
        runner = self.spike / "src" / "runner.lua"
        adapter = self.spike / "src" / "adapter.lua"
        self.process = subprocess.Popen(
            ["luajit", str(runner.resolve()), str(adapter.resolve())],
            cwd=source,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
        )
        self.reader_thread = threading.Thread(target=self._read_stdout, daemon=True)
        self.reader_thread.start()
        self.ready = self._next_marker(self.timeout_seconds)
        if self.ready.get("event") != "ready":
            raise PobRunnerError(f"Expected ready event, got {self.ready}")
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        if self.process is None:
            return
        try:
            if self.process.poll() is None and self.process.stdin is not None:
                self.process.stdin.write(json.dumps({"operation": "shutdown"}) + "\n")
                self.process.stdin.flush()
                self.process.stdin.close()
                self.process.wait(timeout=5)
        except (BrokenPipeError, subprocess.TimeoutExpired):
            self.process.terminate()
            self.process.wait(timeout=5)
        finally:
            if self.process.stdout is not None:
                self.process.stdout.close()
            if self.reader_thread is not None:
                self.reader_thread.join(timeout=1)

    def request(self, operation: str, pob_path: Path, selection=None):
        envelope = self.request_envelope(operation, pob_path, selection)
        return envelope["result"]

    def request_envelope(self, operation: str, pob_path: Path, selection=None):
        if self.process is None or self.process.stdin is None:
            raise PobRunnerError("Runner has not started")
        request_id = str(self.next_id)
        self.next_id += 1
        request = {"id": request_id, "operation": operation, "pobPath": str(pob_path)}
        if selection is not None:
            request["selection"] = selection
        self.process.stdin.write(json.dumps(request, separators=(",", ":")) + "\n")
        self.process.stdin.flush()
        response = self._next_marker(self.timeout_seconds)
        if response.get("id") != request_id:
            raise PobRunnerError(f"Unexpected response id: {response}")
        if "error" in response:
            raise PobRunnerError(response["error"]["code"])
        return response

    @property
    def pid(self):
        if self.process is None:
            raise PobRunnerError("Runner has not started")
        return self.process.pid

    def _read_stdout(self):
        assert self.process is not None and self.process.stdout is not None
        for line in self.process.stdout:
            self.lines.put(line.rstrip("\n"))

    def _next_marker(self, timeout_seconds: float):
        deadline = time.monotonic() + timeout_seconds
        while time.monotonic() < deadline:
            remaining = deadline - time.monotonic()
            try:
                line = self.lines.get(timeout=max(remaining, 0.01))
            except queue.Empty:
                break
            if line.startswith(MARKER):
                return json.loads(line[len(MARKER) :])
            self.diagnostics.append(line)
        diagnostic_text = "\n".join(self.diagnostics[-20:])
        raise PobRunnerError(f"Timed out waiting for PoB response. Output:\n{diagnostic_text}")
