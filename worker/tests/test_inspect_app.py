import base64
from pathlib import Path
import unittest
import zlib

from fastapi.testclient import TestClient

from worker.inspect_app import create_app
from worker.spike.tests.runner_client import PobRunnerError


ROOT = Path(__file__).resolve().parents[2]
FIXTURE = ROOT / "worker" / "spike" / ".cache" / "fixtures" / "multiple.xml"


class InspectAppTest(unittest.TestCase):
    def test_reports_ready_when_the_pob_runner_has_started(self):
        with TestClient(create_app()) as client:
            response = client.get("/v1/health")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {"status": "ready"})

    def test_reports_unavailable_when_the_pob_runner_cannot_start(self):
        def unavailable_runner():
            raise PobRunnerError("RUNTIME_ERROR")

        with TestClient(create_app(runner_factory=unavailable_runner)) as client:
            response = client.get("/v1/health")

        self.assertEqual(response.status_code, 503)

    def test_decodes_a_compressed_pob_export_before_inspecting(self):
        compressed_export = base64.urlsafe_b64encode(
            zlib.compress(FIXTURE.read_bytes())
        ).decode().rstrip("=")

        with TestClient(create_app()) as client:
            response = client.post(
                "/v1/builds/inspect", json={"pobXml": compressed_export}
            )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["activeSpec"], 3)

    def test_returns_pob_configuration_lists_for_a_real_fixture(self):
        with TestClient(create_app()) as client:
            response = client.post("/v1/builds/inspect", json={"pobXml": FIXTURE.read_text()})

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual([entry["id"] for entry in body["specs"]], [1, 2, 3])
        self.assertEqual(body["activeSpec"], 3)
        self.assertEqual(body["activeSkillSet"], 2)
        self.assertEqual(body["activeItemSet"], 2)

    def test_rejects_blank_or_malformed_xml_without_calling_the_runner(self):
        class FakeRunner:
            def __init__(self):
                self.request_calls = []

            def request(self, operation, path):
                self.request_calls.append((operation, path))

            def __exit__(self, *_):
                pass

        runner = FakeRunner()
        with TestClient(create_app(runner_factory=lambda: runner)) as client:
            blank = client.post("/v1/builds/inspect", json={"pobXml": "  "})
            malformed = client.post("/v1/builds/inspect", json={"pobXml": "<PathOfBuilding>"})

        self.assertEqual(blank.status_code, 400)
        self.assertEqual(malformed.status_code, 400)
        self.assertEqual(runner.request_calls, [])

    def test_returns_a_stable_error_when_the_runner_fails(self):
        class FailingRunner:
            def request(self, operation, path):
                raise PobRunnerError("RUNTIME_ERROR")

            def __exit__(self, *_):
                pass

        with TestClient(create_app(runner_factory=FailingRunner)) as client:
            response = client.post("/v1/builds/inspect", json={"pobXml": "<PathOfBuilding />"})

        self.assertEqual(response.status_code, 503)
        self.assertEqual(response.json(), {"detail": "The PoB inspect worker is unavailable."})


if __name__ == "__main__":
    unittest.main()
