"""Update existing 12.7.6 uploads, using IDs from their publication receipts."""

import argparse
import json
import os
from pathlib import Path
import re
import urllib.error
import urllib.request


REQUIREMENTS = """## Requirements

- [Cleanroom](https://github.com/CleanroomMC/Cleanroom) 0.7.14-alpha or newer
- [Scalar](https://www.curseforge.com/minecraft/mc-mods/scalar) 3.10.0 or newer
- [ModularUI](https://www.curseforge.com/minecraft/mc-mods/modularui) 3.1.6 or newer
"""
FILE_ID = 9624473
VERSION_ID = "LLCbk7sk"
MUI_ID = "Ecvd12QC"


def request(url, token=None, method="GET", data=None, content_type=None):
    headers = {"User-Agent": "Sumire-Labs/AEM-Rescaled release metadata maintenance"}
    if token:
        header, value = token
        headers[header] = value
    if content_type:
        headers["Content-Type"] = content_type
    req = urllib.request.Request(url, headers=headers, method=method, data=data)
    try:
        with urllib.request.urlopen(req, timeout=60) as response:
            body = response.read()
            return response.status, json.loads(body) if body else None
    except urllib.error.HTTPError as error:
        # Do not include request headers or token values in logs.
        raise RuntimeError(f"{method} {url}: HTTP {error.code}") from None


def release_notes():
    text = Path("CHANGELOG.md").read_text(encoding="utf-8-sig")
    match = re.search(r"^## \[12\.7\.6\][^\n]*\n(.*?)(?=^## |\Z)", text, re.M | re.S)
    if not match or not match[1].strip():
        raise RuntimeError("Missing 12.7.6 release notes")
    return REQUIREMENTS + "\n" + match[1].strip() + "\n"


def update_curseforge(dry_run):
    payload = {
        "fileID": FILE_ID,
        "changelog": release_notes(),
        "changelogType": "markdown",
        "relations": {"projects": [
            {"slug": "scalar", "type": "requiredDependency"},
            {"slug": "modularui", "type": "requiredDependency"},
        ]},
    }
    if dry_run:
        return payload
    # Confirm that the existing file still belongs to this release before updating it.
    _, project = request("https://api.cfwidget.com/1736004")
    existing = next((item for item in project["files"] if item["id"] == FILE_ID), None)
    if not existing or existing["name"] != "AdditionalEnchantedMiner-Rescaled-12.7.6.jar":
        raise RuntimeError("CurseForge file identity could not be verified")
    boundary = "AemReleaseMetadata1276"
    body = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"metadata\"\r\n"
            "Content-Type: application/json\r\n\r\n" + json.dumps(payload)
            + f"\r\n--{boundary}--\r\n").encode()
    status, result = request(
        "https://minecraft.curseforge.com/api/projects/1736004/update-file",
        ("X-Api-Token", os.environ["CURSEFORGE_TOKEN"]), "POST", body,
        f"multipart/form-data; boundary={boundary}",
    )
    if result is None or result.get("id") != FILE_ID:
        raise RuntimeError("CurseForge did not confirm the expected file ID")
    return {"status": status, "response": result, "metadata": payload}


def update_modrinth(dry_run):
    payload = {"changelog": release_notes(), "dependencies": [
        {"project_id": MUI_ID, "version_id": None, "dependency_type": "required"},
    ]}
    if dry_run:
        return payload
    token = ("Authorization", os.environ["MODRINTH_TOKEN"])
    url = f"https://api.modrinth.com/v2/version/{VERSION_ID}"
    # Some publication tokens can update versions but cannot read private project metadata.
    # If readable, verify its identity and retain unrelated dependency declarations.
    try:
        _, existing = request(url, token)
    except RuntimeError as error:
        if not str(error).endswith("HTTP 404"):
            raise
        print("::warning::Version metadata is not readable; using the original publication receipt ID.")
        existing = None
    if existing is not None:
        if existing["project_id"] != "jgRO15f7" or existing["version_number"] != "12.7.6":
            raise RuntimeError("Modrinth version identity does not match 12.7.6")
        payload["dependencies"] += [
            item for item in existing["dependencies"] if item.get("project_id") != MUI_ID
        ]
    status, _ = request(url, token, "PATCH", json.dumps(payload).encode(), "application/json")
    if status != 204:
        raise RuntimeError(f"Unexpected Modrinth response: {status}")
    return {"status": status, "version_id": VERSION_ID, "metadata": payload}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("platform", choices=("curseforge", "modrinth"))
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    result = {"curseforge": update_curseforge, "modrinth": update_modrinth}[args.platform](args.dry_run)
    output = Path("build/metadata-update")
    output.mkdir(parents=True, exist_ok=True)
    (output / f"{args.platform}.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result, indent=2))
