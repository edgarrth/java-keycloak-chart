import json
import os
import time
import urllib.error
import urllib.parse
import urllib.request

BASE = (
    os.environ.get("KEYCLOAK_INTERNAL_URL") or os.environ["KEYCLOAK_PUBLIC_URL"]
).rstrip("/")
TOKEN = None


def request(method, path, body=None, form=False):
    data = (
        (urllib.parse.urlencode(body).encode() if form else json.dumps(body).encode())
        if body is not None
        else None
    )
    headers = {
        "Content-Type": (
            "application/x-www-form-urlencoded" if form else "application/json"
        )
    }
    if TOKEN:
        headers["Authorization"] = "Bearer " + TOKEN
    req = urllib.request.Request(BASE + path, data=data, method=method, headers=headers)
    with urllib.request.urlopen(req, timeout=15) as response:
        raw = response.read()
        return json.loads(raw) if raw else None


for attempt in range(120):
    try:
        TOKEN = request(
            "POST",
            "/realms/master/protocol/openid-connect/token",
            {
                "client_id": "admin-cli",
                "grant_type": "password",
                "username": os.environ["KC_BOOTSTRAP_ADMIN_USERNAME"],
                "password": os.environ["KC_BOOTSTRAP_ADMIN_PASSWORD"],
            },
            form=True,
        )["access_token"]
        request("GET", "/admin/realms/axiz")
        break
    except (urllib.error.URLError, TimeoutError):
        if attempt == 119:
            raise RuntimeError(
                "No se pudo acceder al realm para cargar usuarios"
            ) from None
        time.sleep(2)

info = request("GET", "/admin/serverinfo")
selected_theme = os.getenv("KEYCLOAK_LOGIN_THEME", "axiz")
login_themes = info.get("themes", {}).get("login", [])
available_themes = {
    item["name"] if isinstance(item, dict) else item for item in login_themes
}
if available_themes and selected_theme not in available_themes:
    raise RuntimeError(
        "El tema no está instalado: "
        + selected_theme
        + ". Se reconstruye la imagen Keycloak antes de cargar el realm."
    )

realm = request("GET", "/admin/realms/axiz")
realm.update(
    loginTheme=selected_theme,
    internationalizationEnabled=True,
    supportedLocales=["es"],
    defaultLocale="es",
)
request("PUT", "/admin/realms/axiz", realm)
print("Se configuró el tema de login " + realm["loginTheme"] + " en el realm axiz")

role = request("GET", "/admin/realms/axiz/roles/merchant")
for dni, name, enabled in [
    ("12345678", "Operador", True),
    ("87654321", "Supervisor", True),
    ("11223344", "Comerciante", True),
    ("22334455", "Deshabilitado", False),
]:
    query = urllib.parse.urlencode({"username": dni, "exact": "true"})
    users = request("GET", "/admin/realms/axiz/users?" + query)
    profile = {
        "username": dni,
        "enabled": enabled,
        "emailVerified": True,
        "firstName": name,
        "lastName": "Axiz",
        "email": "dni-" + dni + "@example.test",
        "requiredActions": [],
    }
    if not users:
        request("POST", "/admin/realms/axiz/users", profile)
        users = request("GET", "/admin/realms/axiz/users?" + query)
    user_id = users[0]["id"]
    path = "/admin/realms/axiz/users/" + user_id
    request("PUT", path, profile)
    request(
        "PUT",
        path + "/reset-password",
        {"type": "password", "value": os.environ["DNI_PASSWORD"], "temporary": False},
    )
    request("POST", path + "/role-mappings/realm", [role])
    print(
        "Se aprovisionó DNI " + dni + (" habilitado" if enabled else " deshabilitado")
    )

for spi, detail in info.get("providers", {}).items():
    for provider, properties in detail.get("providers", {}).items():
        version = properties.get("operationalInfo", {}).get("version")
        if version:
            print("Proveedor " + spi + "/" + provider + ": versión " + version)
