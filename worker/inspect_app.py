import asyncio
import base64
import binascii
from collections.abc import Callable
from contextlib import asynccontextmanager
import os
from pathlib import Path
import tempfile
from typing import Optional
import xml.etree.ElementTree as ElementTree
import zlib

from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel, ValidationError

from worker.spike.tests.runner_client import PobRunner, PobRunnerError


ROOT = Path(__file__).resolve().parents[1]
SPIKE = ROOT / "worker" / "spike"
UNAVAILABLE_DETAIL = "The PoB inspect worker is unavailable."
INVALID_XML_DETAIL = "Provide a well-formed Path of Building XML export or compressed export code."
MAX_XML_BYTES = 8 * 1024 * 1024


class InspectRequest(BaseModel):
    pobXml: str


def decode_pob_input(pob_input: str) -> str:
    value = pob_input.strip()
    if value.startswith("<"):
        return value

    try:
        padded = value + "=" * (-len(value) % 4)
        compressed = base64.b64decode(padded, altchars=b"-_", validate=True)
        decompressor = zlib.decompressobj()
        xml_bytes = decompressor.decompress(compressed, MAX_XML_BYTES + 1)
        if len(xml_bytes) > MAX_XML_BYTES or decompressor.unconsumed_tail:
            raise ValueError
        xml_bytes += decompressor.flush(MAX_XML_BYTES + 1 - len(xml_bytes))
        if len(xml_bytes) > MAX_XML_BYTES or not decompressor.eof:
            raise ValueError
        return xml_bytes.decode("utf-8")
    except (ValueError, binascii.Error, UnicodeDecodeError, zlib.error) as error:
        raise ValueError(INVALID_XML_DETAIL) from error


class InspectEntry(BaseModel):
    id: int
    title: str


class InspectResult(BaseModel):
    specs: list[InspectEntry]
    skillSets: list[InspectEntry]
    itemSets: list[InspectEntry]
    activeSpec: int
    activeSkillSet: int
    activeItemSet: int


class InspectService:
    def __init__(self, runner: PobRunner):
        self.runner = runner

    def inspect(self, pob_xml: str) -> InspectResult:
        descriptor, path_string = tempfile.mkstemp(suffix=".xml")
        path = Path(path_string)
        try:
            with os.fdopen(descriptor, "w", encoding="utf-8") as file:
                file.write(pob_xml)
            return InspectResult.model_validate(self.runner.request("inspect", path))
        except (OSError, PobRunnerError, ValidationError) as error:
            raise RuntimeError(UNAVAILABLE_DETAIL) from error
        finally:
            path.unlink(missing_ok=True)


def start_runner() -> PobRunner:
    runner = PobRunner(SPIKE)
    return runner.__enter__()


def create_app(runner_factory: Optional[Callable[[], PobRunner]] = None) -> FastAPI:
    @asynccontextmanager
    async def lifespan(app: FastAPI):
        app.state.lock = asyncio.Lock()
        app.state.runner = None
        app.state.service = None
        app.state.startup_error = None
        try:
            runner = (runner_factory or start_runner)()
            app.state.runner = runner
            app.state.service = InspectService(runner)
        except (OSError, PobRunnerError) as error:
            app.state.startup_error = error
        try:
            yield
        finally:
            if app.state.runner is not None:
                app.state.runner.__exit__(None, None, None)

    app = FastAPI(lifespan=lifespan)

    @app.get("/v1/health")
    async def health(http_request: Request) -> dict[str, str]:
        if http_request.app.state.service is None:
            raise HTTPException(status_code=503, detail=UNAVAILABLE_DETAIL)
        return {"status": "ready"}

    @app.post("/v1/builds/inspect", response_model=InspectResult)
    async def inspect_build(request: InspectRequest, http_request: Request) -> InspectResult:
        pob_input = request.pobXml.strip()
        if not pob_input:
            raise HTTPException(status_code=400, detail="PoB export is required.")
        try:
            pob_xml = decode_pob_input(pob_input)
        except ValueError as error:
            raise HTTPException(status_code=400, detail=INVALID_XML_DETAIL) from error
        try:
            root = ElementTree.fromstring(pob_xml)
        except ElementTree.ParseError as error:
            raise HTTPException(status_code=400, detail=INVALID_XML_DETAIL) from error
        if root.tag != "PathOfBuilding":
            raise HTTPException(status_code=400, detail=INVALID_XML_DETAIL)
        if http_request.app.state.service is None:
            raise HTTPException(status_code=503, detail=UNAVAILABLE_DETAIL)
        try:
            async with http_request.app.state.lock:
                return await asyncio.to_thread(http_request.app.state.service.inspect, pob_xml)
        except RuntimeError as error:
            raise HTTPException(status_code=503, detail=UNAVAILABLE_DETAIL) from error

    return app


app = create_app()
