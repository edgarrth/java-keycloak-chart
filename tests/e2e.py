#!/usr/bin/env python3
"""Se comprueban los contratos OIDC y la autorización de pagos contra servicios reales."""

import base64
import hashlib
import http.cookiejar
import json
import os
import secrets
import urllib.error
import urllib.parse
import urllib.request
from html.parser import HTMLParser

KC = os.getenv("KEYCLOAK_PUBLIC_URL", "http://localhost:8080")
API = os.getenv("API_URL", "http://localhost:8090")
OIDC = KC + "/realms/axiz/protocol/openid-connect"
checks = []


def request(url, data=None, token=None, expected=200, form=False, headers=None):
    h = dict(headers or {})
    if token:
        h["Authorization"] = "Bearer " + token
    if data is not None:
        h["Content-Type"] = (
            "application/x-www-form-urlencoded" if form else "application/json"
        )
        data = (
            urllib.parse.urlencode(data).encode() if form else json.dumps(data).encode()
        )
    req = urllib.request.Request(url, data=data, headers=h)
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            status, body = response.status, response.read()
    except urllib.error.HTTPError as error:
        status, body = error.code, error.read()
    assert (
        status == expected
    ), f"{url}: se esperaba {expected}, se obtuvo {status}: {body[:300]!r}"
    checks.append({"route": urllib.parse.urlparse(url).path, "status": status})
    return json.loads(body) if body else {}


def password(client, username, password, **extra):
    secret = os.environ[
        (
            "PASSWORD_CLIENT_SECRET"
            if client == "payments-password"
            else "PHONE_CLIENT_SECRET"
        )
    ]
    data = dict(
        grant_type="password",
        client_id=client,
        client_secret=secret,
        username=username,
        password=password,
        scope="openid",
    )
    data.update(extra)
    return data


def claims(token):
    return json.loads(base64.urlsafe_b64decode(token.split(".")[1] + "=="))


class LoginForm(HTMLParser):
    def __init__(self):
        super().__init__()
        self.action = None

    def handle_starttag(self, tag, attrs):
        attributes = dict(attrs)
        if tag == "form" and attributes.get("id") == "kc-form-login":
            self.action = attributes["action"]


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def authorization_code():
    verifier = secrets.token_urlsafe(48)
    challenge = (
        base64.urlsafe_b64encode(hashlib.sha256(verifier.encode()).digest())
        .decode()
        .rstrip("=")
    )
    state, nonce = secrets.token_urlsafe(24), secrets.token_urlsafe(24)
    redirect = "http://localhost:8090/callback-test"
    query = urllib.parse.urlencode(
        dict(
            client_id="payments-web",
            response_type="code",
            scope="openid profile",
            redirect_uri=redirect,
            state=state,
            nonce=nonce,
            code_challenge=challenge,
            code_challenge_method="S256",
            acr_values="5",
        )
    )
    cookies = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(
        urllib.request.HTTPCookieProcessor(cookies), NoRedirect()
    )
    with opener.open(OIDC + "/auth?" + query, timeout=30) as response:
        parser = LoginForm()
        parser.feed(response.read().decode())
    assert parser.action, "No se encontró el formulario de Keycloak"
    # Se reproduce el tratamiento de localhost como contexto confiable de los navegadores.
    if urllib.parse.urlparse(KC).scheme == "http" and urllib.parse.urlparse(
        KC
    ).hostname in ("localhost", "127.0.0.1"):
        for cookie in cookies:
            cookie.secure = False
    req = urllib.request.Request(
        parser.action,
        data=urllib.parse.urlencode(
            {
                "username": "12345678",
                "password": os.environ["DNI_PASSWORD"],
                "credentialId": "",
            }
        ).encode(),
    )
    try:
        opener.open(req, timeout=30)
        raise AssertionError("Se esperaba la redirección OIDC")
    except urllib.error.HTTPError as response:
        assert response.code == 302, response.read().decode()[:500]
        returned = urllib.parse.parse_qs(
            urllib.parse.urlparse(response.headers["Location"]).query
        )
    assert returned["state"] == [state]
    data = dict(
        grant_type="authorization_code",
        client_id="payments-web",
        redirect_uri=redirect,
        code=returned["code"][0],
        code_verifier=verifier,
    )
    result = request(OIDC + "/token", data, form=True)
    assert claims(result["id_token"])["nonce"] == nonce
    request(OIDC + "/token", data, form=True, expected=400)
    assert claims(result["access_token"])["acr"] == "5"
    return result


def portal_login():
    cookies = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookies))
    with opener.open(API + "/oauth2/authorization/axiz", timeout=30) as response:
        parser = LoginForm()
        parser.feed(response.read().decode())
    assert parser.action, "No se encontró el formulario del portal OIDC"
    # Se conserva el mismo tratamiento local de cookies que utiliza el recorrido PKCE.
    if urllib.parse.urlparse(KC).scheme == "http" and urllib.parse.urlparse(
        KC
    ).hostname in ("localhost", "127.0.0.1"):
        for cookie in cookies:
            cookie.secure = False
    data = urllib.parse.urlencode(
        {
            "username": "12345678",
            "password": os.environ["DNI_PASSWORD"],
            "credentialId": "",
        }
    ).encode()
    with opener.open(
        urllib.request.Request(parser.action, data=data), timeout=30
    ) as response:
        result = json.load(response)
    assert result == {"username": "12345678", "acr": "5", "flow": "code"}
    checks.append({"route": "/portal", "status": 200})
    return result


def main():
    request(KC + "/realms/axiz/.well-known/openid-configuration")
    request(API + "/api/payments", expected=401)
    for client, username, credential in [
        ("payments-password", "22334455", os.environ["DNI_PASSWORD"]),
        (
            "payments-phone",
            "+51911111111",
            os.environ.get("PHONE_DEMO_PASSWORD", "Telefono2026!"),
        ),
        ("payments-phone", "+51987654321", "ClaveIncorrecta2026!"),
    ]:
        request(
            OIDC + "/token",
            password(client, username, credential),
            form=True,
            expected=400,
        )
    additional = request(
        OIDC + "/token",
        password("payments-password", "87654321", os.environ["DNI_PASSWORD"]),
        form=True,
    )
    assert claims(additional["access_token"])["acr"] == "4"

    request(
        OIDC + "/token",
        password("payments-password", "12345678", "ClaveIncorrecta2026!"),
        form=True,
        expected=400,
    )
    request(
        OIDC + "/token",
        password(
            "payments-password",
            "+51987654321",
            os.environ.get("PHONE_DEMO_PASSWORD", "Telefono2026!"),
        ),
        form=True,
        expected=400,
    )
    request(
        OIDC + "/token",
        password("payments-phone", "12345678", os.environ["DNI_PASSWORD"]),
        form=True,
        expected=400,
    )
    internal = request(
        OIDC + "/token",
        password(
            "payments-password", "12345678", os.environ["DNI_PASSWORD"], acr_values="5"
        ),
        form=True,
    )
    phone = request(
        OIDC + "/token",
        password(
            "payments-phone",
            "+51987654321",
            os.environ.get("PHONE_DEMO_PASSWORD", "Telefono2026!"),
            acr_values="5",
        ),
        form=True,
    )
    code = authorization_code()
    portal_login()
    assert claims(internal["access_token"])["acr"] == "4"
    assert claims(phone["access_token"])["acr"] == "3"
    for token, acr in [(internal, "4"), (phone, "3"), (code, "5")]:
        assert "merchant" in claims(token["access_token"])["realm_access"]["roles"]
        identity = request(API + "/api/identity", token=token["access_token"])
        assert identity["acr"] == acr
        request(API + "/api/payments", token=token["access_token"])
    request(
        API + "/api/payments",
        {"amount": 10, "currency": "PEN"},
        token=phone["access_token"],
        expected=403,
        headers={"Idempotency-Key": "phone-denied-2026"},
    )
    key = secrets.token_hex(12)
    payload = {"amount": 100.50, "currency": "PEN"}
    payment = request(
        API + "/api/payments",
        payload,
        token=internal["access_token"],
        headers={"Idempotency-Key": key},
    )
    repeated = request(
        API + "/api/payments",
        payload,
        token=internal["access_token"],
        headers={"Idempotency-Key": key},
    )
    assert payment["id"] == repeated["id"]
    request(
        API + "/api/payments",
        {"amount": 101, "currency": "PEN"},
        token=internal["access_token"],
        expected=409,
        headers={"Idempotency-Key": key},
    )
    request(
        API + "/api/payments/" + payment["id"] + "/approve",
        {},
        token=internal["access_token"],
        expected=403,
    )
    request(
        API + "/api/payments/" + payment["id"] + "/approve",
        {},
        token=code["access_token"],
    )
    assert not request(API + "/api/payments", token=phone["access_token"])
    tampered = internal["access_token"].split(".")
    body = claims(internal["access_token"])
    body["acr"] = "5"
    tampered[1] = (
        base64.urlsafe_b64encode(json.dumps(body).encode()).decode().rstrip("=")
    )
    request(API + "/api/identity", token=".".join(tampered), expected=401)
    refreshed = request(
        OIDC + "/token",
        dict(
            grant_type="refresh_token",
            client_id="payments-password",
            client_secret=os.environ["PASSWORD_CLIENT_SECRET"],
            refresh_token=internal["refresh_token"],
        ),
        form=True,
    )
    assert claims(refreshed["access_token"])["acr"] == "4"
    request(
        OIDC + "/token",
        dict(
            grant_type="refresh_token",
            client_id="payments-password",
            client_secret=os.environ["PASSWORD_CLIENT_SECRET"],
            refresh_token=internal["refresh_token"],
        ),
        form=True,
        expected=400,
    )
    os.makedirs(".runtime", exist_ok=True)
    with open(".runtime/e2e-results.json", "w") as file:
        json.dump({"passed": True, "checks": checks}, file, indent=2)
    print(
        f"Se completaron {len(checks)} comprobaciones HTTP y las aserciones OIDC, ACR e idempotencia."
    )


if __name__ == "__main__":
    main()
