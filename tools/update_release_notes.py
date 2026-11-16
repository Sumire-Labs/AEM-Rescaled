"""Replace published release notes with the player-facing CHANGELOG section."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import urllib.request

from update_12_7_6_metadata import request


def notes(tag, requirements=False):
    text = Path("CHANGELOG.md").read_text(encoding="utf-8-sig")
    match = re.search(r"^## \[" + re.escape(tag) + r"\][^\n]*\n(.*?)(?=^## |\Z)", text, re.M | re.S)
    if not match or not match[1].strip():
        raise RuntimeError(f"Missing notes for {tag}")
    body = match[1].strip() + "\n"
    if requirements:
        scalar = "3.10.0" if tag == "12.7.6" else "3.9.0"
        body = ("## Requirements\n\n"
                "- [Cleanroom](https://github.com/CleanroomMC/Cleanroom) 0.7.14-alpha or newer\n"
                f"- [Scalar](https://www.curseforge.com/minecraft/mc-mods/scalar) {scalar} or newer\n"
                "- [ModularUI](https://www.curseforge.com/minecraft/mc-mods/modularui) 3.1.6 or newer\n\n" + body)
    return body


def update(platform, tag, dry_run):
    body = notes(tag, requirements=True)
    if dry_run:
        return {"platform": platform, "tag": tag, "changelog": body}
    if platform == "curseforge":
        _, project = request("https://api.cfwidget.com/1736004")
        files = [f for f in project["files"] if f["name"] == f"AdditionalEnchantedMiner-Rescaled-{tag}.jar"]
        if len(files) != 1:
            raise RuntimeError(f"Could not identify the CurseForge file for {tag}")
        file_id = files[0]["id"]
        payload = {"fileID": file_id, "changelog": body, "changelogType": "markdown"}
        boundary = "AemReleaseNotes"
        data = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"metadata\"\r\n"
                "Content-Type: application/json\r\n\r\n" + json.dumps(payload)
                + f"\r\n--{boundary}--\r\n").encode()
        status, result = request(
            "https://minecraft.curseforge.com/api/projects/1736004/update-file",
            ("X-Api-Token", os.environ["CURSEFORGE_TOKEN"]), "POST", data,
            f"multipart/form-data; boundary={boundary}",
        )
        if not result or result.get("id") != file_id:
            raise RuntimeError("Unexpected CurseForge update response")
        return {"status": status, "file_id": file_id, "changelog": body}
    token = ("Authorization", os.environ["MODRINTH_TOKEN"])
    # Version read scopes may not permit project-level listing. Resolve this exact
    # version, or identify it from the hash of its published GitHub release JAR.
    if tag == "12.7.6":
        _, version = request("https://api.modrinth.com/v2/version/LLCbk7sk", token)
    else:
        try:
            _, version = request(f"https://api.modrinth.com/v2/project/jgRO15f7/version/{tag}", token)
        except RuntimeError as error:
            if not str(error).endswith("HTTP 404"):
                raise
            url = (f"https://github.com/Sumire-Labs/AEM-Rescaled/releases/download/{tag}/"
                   f"AdditionalEnchantedMiner-Rescaled-{tag}.jar")
            with urllib.request.urlopen(url, timeout=60) as response:
                digest = hashlib.file_digest(response, "sha512").hexdigest()
            _, version = request(f"https://api.modrinth.com/v2/version_file/{digest}?algorithm=sha512", token)
    if version["project_id"] != "jgRO15f7" or version["version_number"] != tag:
        raise RuntimeError(f"Modrinth identity does not match {tag}")
    url = f"https://api.modrinth.com/v2/version/{version['id']}"
    status, _ = request(url, token, "PATCH", json.dumps({"changelog": body}).encode(), "application/json")
    if status != 204:
        raise RuntimeError("Unexpected Modrinth update response")
    _, updated = request(url, token)
    if updated["changelog"] != body or updated["dependencies"] != version["dependencies"]:
        raise RuntimeError("Modrinth readback did not match the requested notes or existing dependencies")
    return {"status": status, "version_id": version["id"], "verified": True, "changelog": body}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("platform", choices=("curseforge", "modrinth", "github"))
    parser.add_argument("tag", choices=("12.7.4", "12.7.5", "12.7.6"))
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    if args.platform == "github":
        print(notes(args.tag), end="")
    else:
        result = update(args.platform, args.tag, args.dry_run)
        output = Path("build/release-notes-update")
        output.mkdir(parents=True, exist_ok=True)
        (output / f"{args.platform}-{args.tag}.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
        print(json.dumps(result, indent=2))
