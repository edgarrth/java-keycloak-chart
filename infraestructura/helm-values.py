import json
import os
from pathlib import Path

names = {
    "keycloak": "KC_DB_PASSWORD",
    "phone": "PHONE_DB_PASSWORD",
    "adminUser": "KC_BOOTSTRAP_ADMIN_USERNAME",
    "adminPassword": "KC_BOOTSTRAP_ADMIN_PASSWORD",
    "passwordClient": "PASSWORD_CLIENT_SECRET",
    "phoneClient": "PHONE_CLIENT_SECRET",
}
values = {
    "publicUrl": os.environ["KEYCLOAK_PUBLIC_URL"],
    "credentials": {key: os.environ[value] for key, value in names.items()},
    "database": {
        "keycloakUrl": os.environ["K8S_KEYCLOAK_DB_URL"],
        "phoneUrl": os.environ["K8S_PHONE_DB_URL"],
    },
}
Path(".runtime/helm-values.json").write_text(json.dumps(values))
