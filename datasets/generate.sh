#!/usr/bin/env bash
set -euo pipefail
read -rsp 'Clave de la identidad telefónica: ' phone_password
printf '\n'
export AXIZ_SEED_PASSWORD="$phone_password"
python3 - <<'PYTHON'
import hashlib,os,secrets,pathlib
salt=secrets.token_bytes(16)
h=hashlib.pbkdf2_hmac('sha256',os.environ['AXIZ_SEED_PASSWORD'].encode(),salt,600000)
p=pathlib.Path('datasets/phone-seed.sql')
s=p.read_text()
import re
s=re.sub(r"decode\('[0-9a-f]+','hex'\),decode\('[0-9a-f]+','hex'\)",f"decode('{salt.hex()}','hex'),decode('{h.hex()}','hex')",s)
p.write_text(s)
PYTHON
unset AXIZ_SEED_PASSWORD phone_password
