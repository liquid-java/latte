#!/usr/bin/env python3
"""Compare unchanged OpenJDK test sources with and without the Java 21 contracts.

Requires a built latte/target/latte-cli.jar and Maven test-compile. No upstream
test code is executed. A source checkout is supplied explicitly, never fetched.
Default selection: @test/@Test Java sources below test/jdk/<type> directories
of stronger catalog targets, plus type-named tests below matching package paths
in test/jdk and test/jaxp. --selection accepts a JSON mapping of relative
source paths to target names, to expand or reproduce a particular corpus.
"""

import argparse
from collections import Counter
from concurrent.futures import ThreadPoolExecutor, as_completed
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import time


REPO = Path(__file__).resolve().parents[1]


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n")


def discover(root):
    catalog = json.loads((REPO / "specifications/jdk21/catalog.json").read_text())
    selected = {}
    tests = [
        file for area in ("test/jdk", "test/jaxp")
        for file in sorted((root / area).rglob("*.java"))
        if re.search(r"@(?:test\b|Test\b)", file.read_text(errors="replace"))
    ]
    for spec in catalog["specifications"]:
        target = spec["target"]
        top = target.split("$")[0]
        package, name = top.rsplit(".", 1)
        directory = root / "test/jdk" / top.replace(".", "/")
        for file in tests:
            named = (len(name) >= 4 and name in file.stem
                     and package.replace(".", "/") in str(file.relative_to(root)))
            if file.is_relative_to(directory) or named:
                selected.setdefault(str(file.relative_to(root)), []).append(target)
    return dict(sorted(selected.items()))


def diagnostic_key(diagnostic):
    return tuple(diagnostic.get(k) for k in ("file", "line", "column", "kind", "message"))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("--revision", required=True, help="Audited OpenJDK commit SHA")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--java", default="java")
    parser.add_argument("--workers", type=int, default=4)
    parser.add_argument("--timeout", type=int, default=60, help="Seconds per file per mode")
    parser.add_argument("--selection", type=Path)
    args = parser.parse_args()
    if args.workers < 1 or args.timeout < 1:
        parser.error("workers and timeout must be positive")
    root, output = args.source.resolve(), args.output.resolve()
    selected = json.loads(args.selection.read_text()) if args.selection else discover(root)
    if not selected:
        parser.error("No upstream tests selected")
    for relative in selected:
        source = (root / relative).resolve()
        if not source.is_relative_to(root) or not source.is_file():
            parser.error("Selected file is not inside the source root: " + relative)
    jar = REPO / "latte/target/latte-cli.jar"
    worker = REPO / "latte/target/test-classes/latte/jdk/Jdk21UpstreamAudit.class"
    manifest = {
        "revision": args.revision,
        "catalogSha256": sha256(REPO / "specifications/jdk21/catalog.json"),
        "jarSha256": sha256(jar),
        "workerSha256": sha256(worker),
        "driverSha256": sha256(Path(__file__)),
        "java": subprocess.run([args.java, "-version"], capture_output=True, text=True, check=True).stderr,
        "timeoutSeconds": args.timeout,
        "mode": "individual unchanged sources, no-classpath; both passes in both modes",
        "files": {
            name: {"sha256": sha256(root / name), "targets": selected[name]}
            for name in sorted(selected)
        },
    }
    previous = output / "manifest.json"
    if previous.exists() and json.loads(previous.read_text()) != manifest:
        parser.error("Output belongs to different inputs; use a fresh output directory")
    write_json(previous, manifest)

    def run(relative, mode):
        result_file = output / "files" / (relative + "." + mode + ".json")
        if result_file.exists():
            return relative, mode, json.loads(result_file.read_text())
        result_file.parent.mkdir(parents=True, exist_ok=True)
        command = [
            args.java, "-Xmx1024m", "-XX:ActiveProcessorCount=2", "-cp",
            str(REPO / "latte/target/test-classes") + os.pathsep + str(jar),
            "latte.jdk.Jdk21UpstreamAudit", str(root), relative, mode, str(result_file),
        ]
        started = time.monotonic()
        try:
            process = subprocess.run(command, capture_output=True, text=True, timeout=args.timeout)
            if process.returncode == 0 and result_file.exists():
                result = json.loads(result_file.read_text())
            else:
                result = {"status": "process-error", "diagnostics": [], "uses": [],
                          "failure": process.stderr[-4000:], "exitCode": process.returncode}
        except subprocess.TimeoutExpired:
            result = {"status": "timeout", "diagnostics": [], "uses": []}
        result["elapsedSeconds"] = round(time.monotonic() - started, 3)
        write_json(result_file, result)
        return relative, mode, result

    results = {name: {} for name in selected}
    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        pending = [pool.submit(run, name, mode) for name in sorted(selected) for mode in ("all", "none")]
        for count, future in enumerate(as_completed(pending), 1):
            name, mode, result = future.result()
            results[name][mode] = result
            if count % 20 == 0 or count == len(pending):
                print(f"{count}/{len(pending)}: {name} ({mode}: {result['status']})", flush=True)

    for name, pair in results.items():
        pair["comparable"] = all(pair[mode]["status"] == "analyzed" for mode in ("all", "none"))
        if pair["comparable"]:
            baseline = {diagnostic_key(d) for d in pair["none"]["diagnostics"]}
            pair["introduced"] = [d for d in pair["all"]["diagnostics"] if diagnostic_key(d) not in baseline]
    summary = {
        "files": len(results),
        "statusWithCatalog": dict(Counter(p["all"]["status"] for p in results.values())),
        "statusWithoutCatalog": dict(Counter(p["none"]["status"] for p in results.values())),
        "comparableFiles": sum(p["comparable"] for p in results.values()),
        "filesWithIntroducedDiagnostics": sum(bool(p.get("introduced")) for p in results.values()),
        "introducedKinds": dict(Counter(d["kind"] for p in results.values() for d in p.get("introduced", []))),
    }
    write_json(output / "results.json", dict(sorted(results.items())))
    write_json(output / "summary.json", summary)
    print(json.dumps(summary, indent=2), flush=True)


if __name__ == "__main__":
    main()
