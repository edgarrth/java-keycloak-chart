#!/usr/bin/env python3
"""Se comprueban sesiones entre instancias identificadas mediante Gateway API."""

import base64
import hashlib
import http.cookiejar
import json
import os
from pathlib import Path
import re
import secrets
import time
import urllib.error
import urllib.parse
import urllib.request

from e2e import LoginForm, NoRedirect

KC = os.environ.get("KEYCLOAK_PUBLIC_URL", "http://localhost:8080").rstrip("/")
API = os.environ.get("API_URL", "http://localhost:8090").rstrip("/")
OIDC = KC + "/realms/axiz/protocol/openid-connect"
CHECKS = []


def claims(token):
    payload = token.split(".")[1]
    return json.loads(base64.urlsafe_b64decode(payload + "=" * (-len(payload) % 4)))


def call(instance, url, data=None, token=None, method=None, expected=200, opener=None):
    headers = {"X-Axiz-Instance": instance}
    if token:
        headers["Authorization"] = "Bearer " + token
    if data is not None:
        headers["Content-Type"] = "application/x-www-form-urlencoded"
        data = urllib.parse.urlencode(data).encode()
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    opener = opener or urllib.request.build_opener(NoRedirect())
    try:
        with opener.open(request, timeout=30) as response:
            status, response_headers, body = (
                response.status,
                response.headers,
                response.read(),
            )
    except urllib.error.HTTPError as response:
        status, response_headers, body = (
            response.code,
            response.headers,
            response.read(),
        )
    assert (
        response_headers.get("X-Axiz-Instance") == instance
    ), "No se confirmó la instancia elegida"
    assert (
        status == expected
    ), f"Se esperaba HTTP {expected} en {instance}; se obtuvo {status}"
    route = urllib.parse.urlparse(url).path
    route = re.sub(r"(/users/)[^/]+", r"\1{user}", route)
    route = re.sub(r"(/sessions/)[^/]+", r"\1{session}", route)
    CHECKS.append(
        {
            "instance": instance,
            "path": route,
            "status": status,
        }
    )
    return body, response_headers


def json_call(*args, **kwargs):
    body, _ = call(*args, **kwargs)
    return json.loads(body) if body else None


def client_credentials(flow):
    if flow == "code":
        return {"client_id": "payments-web"}
    if flow == "password":
        return {
            "client_id": "payments-password",
            "client_secret": os.environ["PASSWORD_CLIENT_SECRET"],
        }
    return {
        "client_id": "payments-phone",
        "client_secret": os.environ["PHONE_CLIENT_SECRET"],
    }


def code_login(source, target):
    verifier = secrets.token_urlsafe(32)
    challenge = (
        base64.urlsafe_b64encode(hashlib.sha256(verifier.encode()).digest())
        .decode()
        .rstrip("=")
    )
    state, nonce = secrets.token_urlsafe(24), secrets.token_urlsafe(24)
    redirect = API + "/callback-test"
    query = urllib.parse.urlencode(
        {
            "client_id": "payments-web",
            "response_type": "code",
            "scope": "openid profile",
            "redirect_uri": redirect,
            "state": state,
            "nonce": nonce,
            "code_challenge": challenge,
            "code_challenge_method": "S256",
            "acr_values": "5",
        }
    )
    cookies = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(
        urllib.request.HTTPCookieProcessor(cookies), NoRedirect()
    )
    body, _ = call(source, OIDC + "/auth?" + query, opener=opener)
    form = LoginForm()
    form.feed(body.decode())
    assert form.action, "Se requiere el formulario Keycloak"
    form_url, public_url = urllib.parse.urlparse(form.action), urllib.parse.urlparse(KC)
    assert (form_url.scheme, form_url.netloc) == (public_url.scheme, public_url.netloc)
    # Se aplica únicamente al laboratorio HTTP localhost el tratamiento de cookies del navegador.
    if urllib.parse.urlparse(KC).scheme == "http" and urllib.parse.urlparse(
        KC
    ).hostname in ("localhost", "127.0.0.1"):
        for cookie in cookies:
            cookie.secure = False
    _, headers = call(
        target,
        form.action,
        {
            "username": "12345678",
            "password": os.environ["DNI_PASSWORD"],
            "credentialId": "",
        },
        expected=302,
        opener=opener,
    )
    callback = urllib.parse.urlparse(headers["Location"])
    expected_callback = urllib.parse.urlparse(redirect)
    assert (callback.scheme, callback.netloc, callback.path) == (
        expected_callback.scheme,
        expected_callback.netloc,
        expected_callback.path,
    )
    returned = urllib.parse.parse_qs(callback.query)
    assert returned["state"] == [state]
    tokens = json_call(
        source,
        OIDC + "/token",
        {
            "grant_type": "authorization_code",
            "client_id": "payments-web",
            "redirect_uri": redirect,
            "code": returned["code"][0],
            "code_verifier": verifier,
        },
    )
    assert claims(tokens["id_token"])["nonce"] == nonce
    return tokens


def login(flow, source, target):
    if flow == "code":
        return code_login(source, target)
    data = client_credentials(flow) | {
        "grant_type": "password",
        "scope": "openid profile",
    }
    if flow == "password":
        data.update(username="12345678", password=os.environ["DNI_PASSWORD"])
    else:
        data.update(
            username="+51987654321",
            password=os.environ.get("PHONE_DEMO_PASSWORD", "Telefono2026!"),
        )
    return json_call(source, OIDC + "/token", data)


def check_tokens(tokens, flow, subject=None, session=None):
    access = claims(tokens["access_token"])
    assert access["iss"] == KC + "/realms/axiz"
    assert access["acr"] == {"code": "5", "password": "4", "phone": "3"}[flow]
    assert access["auth_flow"] == flow
    identity = claims(tokens["id_token"])
    sid = identity.get("sid") or access.get("sid") or tokens.get("session_state")
    assert sid, "No se obtuvo el identificador de sesión"
    if subject:
        assert access["sub"] == subject
    if session:
        assert sid == session, "Se creó otra sesión en lugar de renovar la original"
    return access["sub"], sid


def sessions(instance, admin, subject):
    return json_call(
        instance,
        KC
        + "/admin/realms/axiz/users/"
        + urllib.parse.quote(subject, safe="")
        + "/sessions",
        token=admin,
    )


def scenario(flow, source, target, admin):
    tokens = login(flow, source, target)
    subject, session = check_tokens(tokens, flow)
    # Se comprueba estado del servidor; validar un JWT en pagos no demostraría sesión compartida.
    for instance in (source, target):
        assert session in {s["id"] for s in sessions(instance, admin, subject)}
    profile = json_call(target, OIDC + "/userinfo", token=tokens["access_token"])
    assert profile["sub"] == subject
    for instance in (target, source):
        tokens = json_call(
            instance,
            OIDC + "/token",
            client_credentials(flow)
            | {
                "grant_type": "refresh_token",
                "refresh_token": tokens["refresh_token"],
            },
        )
        check_tokens(tokens, flow, subject, session)
    call(
        target,
        KC + "/admin/realms/axiz/sessions/" + urllib.parse.quote(session, safe=""),
        token=admin,
        method="DELETE",
        expected=204,
    )
    for _ in range(30):
        if session not in {s["id"] for s in sessions(source, admin, subject)}:
            break
        time.sleep(0.2)
    else:
        raise AssertionError("No se propagó el cierre de sesión entre instancias")
    rejected = json_call(
        source,
        OIDC + "/token",
        client_credentials(flow)
        | {
            "grant_type": "refresh_token",
            "refresh_token": tokens["refresh_token"],
        },
        expected=400,
    )
    assert rejected["error"] == "invalid_grant"
    return {
        "flow": flow,
        "source": source,
        "target": target,
        "same_session": True,
        "refresh_both_instances": True,
        "logout_propagated": True,
    }


def main():
    for instance in ("keycloak-0", "keycloak-1"):
        discovery = json_call(
            instance, KC + "/realms/axiz/.well-known/openid-configuration"
        )
        assert discovery["issuer"] == KC + "/realms/axiz"
    admin = json_call(
        "keycloak-0",
        KC + "/realms/master/protocol/openid-connect/token",
        {
            "grant_type": "password",
            "client_id": "admin-cli",
            "username": os.environ["KC_BOOTSTRAP_ADMIN_USERNAME"],
            "password": os.environ["KC_BOOTSTRAP_ADMIN_PASSWORD"],
        },
    )["access_token"]
    results = []
    for source, target in (("keycloak-0", "keycloak-1"), ("keycloak-1", "keycloak-0")):
        for flow in ("code", "password", "phone"):
            results.append(scenario(flow, source, target, admin))
    path = Path(".runtime/ha-session-results.json")
    path.parent.mkdir(exist_ok=True)
    path.write_text(
        json.dumps(
            {
                "status": "PASS",
                "scenarios": results,
                "http_checks": len(CHECKS),
                "checks": CHECKS,
            },
            ensure_ascii=False,
            indent=2,
        )
        + "\n"
    )
    print(
        f"Se validaron {len(results)} escenarios de sesión compartida y {len(CHECKS)} comprobaciones HTTP entre keycloak-0 y keycloak-1."
    )


if __name__ == "__main__":
    main()
