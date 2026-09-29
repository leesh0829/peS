#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

bash "${repo_dir}/backend/gradlew" -p "${repo_dir}/backend" test bootJar --no-daemon
npm --prefix "${repo_dir}/frontend" run lint
npm --prefix "${repo_dir}/frontend" run build

docker_path="$(command -v docker || true)"
if [[ -n "${docker_path}" && "${docker_path}" != /mnt/* ]]; then
  "${docker_path}" compose --file "${repo_dir}/compose.yaml" config >/dev/null
else
  echo "SKIP: A Linux Docker CLI is not available; Compose validation requires the documented handoff."
fi
