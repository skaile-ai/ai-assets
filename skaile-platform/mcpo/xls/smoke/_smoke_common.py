"""Shared helpers for smoke-script embedded Python heredocs.

Each scenario script bootstraps the import via:

    import os, sys
    sys.path.insert(0, os.environ["SMOKE_DIR"])
    from _smoke_common import start, send, recv, handshake, call, call_expect_error

The helpers all take an explicit subprocess.Popen `p` so scripts that run multiple
server instances in sequence can keep their state straight.
"""

import json
import subprocess


def start(jar):
    return subprocess.Popen(
        ["java", "-jar", jar],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )


def send(p, obj):
    p.stdin.write((json.dumps(obj) + "\n").encode())
    p.stdin.flush()


def recv(p):
    line = p.stdout.readline()
    if not line:
        err = p.stderr.read(2048).decode("utf-8", "replace")
        raise EOFError("server closed stdout; stderr=" + err)
    return json.loads(line.decode())


def handshake(p, client_name="smoke"):
    send(
        p,
        {
            "jsonrpc": "2.0",
            "id": 1,
            "method": "initialize",
            "params": {
                "protocolVersion": "2024-11-05",
                "capabilities": {},
                "clientInfo": {"name": client_name, "version": "0.0.1"},
            },
        },
    )
    init = recv(p)
    assert init.get("id") == 1, init
    send(p, {"jsonrpc": "2.0", "method": "notifications/initialized"})


def call(p, i, name, args):
    send(p, {"jsonrpc": "2.0", "id": i, "method": "tools/call",
             "params": {"name": name, "arguments": args}})
    r = recv(p)
    assert r.get("id") == i, r
    body = json.loads(r["result"]["content"][0]["text"])
    if r["result"].get("isError"):
        raise AssertionError(f"tool error id={i}: {body}")
    return body


def call_expect_error(p, i, name, args, expected_code):
    send(p, {"jsonrpc": "2.0", "id": i, "method": "tools/call",
             "params": {"name": name, "arguments": args}})
    r = recv(p)
    assert r.get("id") == i, r
    body = json.loads(r["result"]["content"][0]["text"])
    assert r["result"].get("isError"), f"expected error but got success: {body}"
    assert body.get("code") == expected_code, f"expected {expected_code}, got {body}"
    return body
