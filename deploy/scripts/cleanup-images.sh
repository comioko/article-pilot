#!/usr/bin/env bash
# 清掉 article-pilot 仓库里超出保留数量的旧 backend / frontend sha 镜像。
# 保留 backend-latest、backend-sha-<当前 commit>、frontend-latest、frontend-sha-<当前 commit>，
# 再额外保留最近 N 个 sha 镜像（默认 N=1），其它全部删除。
#
# 用法:
#   IMAGE_TAG=sha-<git-sha> KEEP_COUNT=1 bash cleanup-images.sh
# 默认 KEEP_COUNT=1，可调。
#
# 设计为幂等，跑多次不会误删被引用的镜像。

set -euo pipefail

REPO="${REPO:-ghcr.io/comioko/article-pilot}"
KEEP_COUNT="${KEEP_COUNT:-1}"
IMAGE_TAG="${IMAGE_TAG:-sha-current}"

log() { printf '[cleanup-images] %s\n' "$*"; }

# 收集所有 sha 镜像，按 tag 名降序排（git sha 字典序≈时间序）
mapfile -t BACKEND_SHAS < <(docker images --format '{{.Repository}}:{{.Tag}}' \
  | grep -E "^${REPO}:backend-sha-" | sort -r)
mapfile -t FRONTEND_SHAS < <(docker images --format '{{.Repository}}:{{.Tag}}' \
  | grep -E "^${REPO}:frontend-sha-" | sort -r)

prune_repo() {
  local kind="$1"; shift
  local shas=("$@")
  local current="${REPO}:${kind}-${IMAGE_TAG}"
  local latest="${REPO}:${kind}-latest"

  local kept=()
  kept+=("$current" "$latest")

  # 追加前 KEEP_COUNT 个 sha（排除 current 之后）
  for tag in "${shas[@]}"; do
    if [[ "$tag" == "$current" ]]; then continue; fi
    kept+=("$tag")
    if (( ${#kept[@]} >= KEEP_COUNT + 2 )); then break; fi
  done

  log "${kind} 保留: ${kept[*]}"

  for tag in "${shas[@]}"; do
    local skip=false
    for k in "${kept[@]}"; do
      if [[ "$tag" == "$k" ]]; then skip=true; break; fi
    done
    if $skip; then continue; fi
    if docker rmi "$tag" >/dev/null 2>&1; then
      log "${kind} 删除: $tag"
    fi
  done
}

prune_repo backend "${BACKEND_SHAS[@]}"
prune_repo frontend "${FRONTEND_SHAS[@]}"

# 最后清一次悬空层
docker image prune -f >/dev/null || true

log "清理完成"
df -h / | tail -n 1
