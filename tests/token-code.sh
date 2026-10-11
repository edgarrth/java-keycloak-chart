#!/usr/bin/env bash
set -euo pipefail
set -a
source .env
set +a
python3 - <<'PYTHON'
import json
import sys
sys.path.insert(0, "tests")
from e2e import authorization_code
print(json.dumps(authorization_code()))
PYTHON
