#!/usr/bin/env bash
set -euo pipefail
printf '%s\n' \
 'Portal: http://localhost:8090/' \
 'Inicio OIDC: http://localhost:8090/oauth2/authorization/axiz' \
 'Callback: http://localhost:8090/login/oauth2/code/axiz' \
 'Sesión web: http://localhost:8090/portal' \
 'Disponibilidad: http://localhost:8090/ready' \
 'Identidad JWT: http://localhost:8090/api/identity' \
 'Pagos (GET y POST): http://localhost:8090/api/payments' \
 'Aprobación (POST): http://localhost:8090/api/payments/{id}/approve' \
 'Consola: http://localhost:8080/admin/' \
 'Discovery: http://localhost:8080/realms/axiz/.well-known/openid-configuration' \
 'Authorization: http://localhost:8080/realms/axiz/protocol/openid-connect/auth' \
 'Token: http://localhost:8080/realms/axiz/protocol/openid-connect/token' \
 'JWKS: http://localhost:8080/realms/axiz/protocol/openid-connect/certs' \
 'UserInfo: http://localhost:8080/realms/axiz/protocol/openid-connect/userinfo' \
 'Logout: http://localhost:8080/realms/axiz/protocol/openid-connect/logout' \
 'Revocación: http://localhost:8080/realms/axiz/protocol/openid-connect/revoke'
