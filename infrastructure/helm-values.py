import json
import os
from pathlib import Path
from urllib.parse import urlparse

names = {
    "keycloak": "KC_DB_PASSWORD",
    "phone": "PHONE_DB_PASSWORD",
    "adminUser": "KC_BOOTSTRAP_ADMIN_USERNAME",
    "adminPassword": "KC_BOOTSTRAP_ADMIN_PASSWORD",
    "passwordClient": "PASSWORD_CLIENT_SECRET",
    "phoneClient": "PHONE_CLIENT_SECRET",
}
replicas = int(os.getenv("KEYCLOAK_REPLICAS", "2"))
if not 2 <= replicas <= 8:
    raise SystemExit("Se requieren entre 2 y 8 réplicas Keycloak")
portal_url = os.getenv("AXIZ_PORTAL_URL", os.getenv("API_URL", "http://localhost:8090"))
portal = urlparse(portal_url)
if (
    portal.scheme not in ("http", "https")
    or not portal.netloc
    or portal.username
    or portal.password
):
    raise SystemExit("Se requiere una URL HTTP(S) absoluta del portal sin credenciales")
values = {
    "portalUrl": portal_url,
    "keycloak": {"replicas": replicas},
    "publicUrl": os.environ["KEYCLOAK_PUBLIC_URL"],
    "credentials": {key: os.environ[value] for key, value in names.items()},
    "images": {"keycloakRevision": os.getenv("KEYCLOAK_IMAGE_REVISION", "")},
    "database": {
        "keycloakUrl": os.environ["K8S_KEYCLOAK_DB_URL"],
        "phoneUrl": os.environ["K8S_PHONE_DB_URL"],
    },
}
Path(".runtime/helm-values.json").write_text(json.dumps(values))
