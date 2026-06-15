#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_APK="$ROOT_DIR/app/build/outputs/apk/release/app-release.apk"

apk_path="${APK_PATH:-$DEFAULT_APK}"
asset_label="${ASSET_LABEL:-}"
repo="${GH_REPO:-}"
clobber=false

usage() {
  cat <<'USAGE'
Usage: scripts/upload-release-apk.sh <tag> [--apk APK_PATH] [--repo OWNER/REPO] [--label LABEL] [--clobber]

Upload a compiled SnapSort APK to an existing GitHub Release using gh.

Arguments:
  tag                   Git tag for the target GitHub Release

Options:
  --apk APK_PATH        APK to upload (default: app/build/outputs/apk/release/app-release.apk)
  --repo OWNER/REPO     GitHub repository passed to gh --repo
  --label LABEL         Display label for the uploaded release asset
  --clobber             Overwrite an existing release asset with the same name
  -h, --help            Show this help

Environment:
  APK_PATH              Override APK path
  ASSET_LABEL           Override release asset display label
  GH_REPO               Override GitHub repository passed to gh --repo
USAGE
}

if [[ $# -eq 0 ]]; then
  usage >&2
  exit 2
fi

tag=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --apk)
      if [[ $# -lt 2 ]]; then
        echo "Missing value for $1" >&2
        exit 2
      fi
      apk_path="$2"
      shift 2
      ;;
    --repo)
      if [[ $# -lt 2 ]]; then
        echo "Missing value for $1" >&2
        exit 2
      fi
      repo="$2"
      shift 2
      ;;
    --label)
      if [[ $# -lt 2 ]]; then
        echo "Missing value for $1" >&2
        exit 2
      fi
      asset_label="$2"
      shift 2
      ;;
    --clobber)
      clobber=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    -*)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
    *)
      if [[ -n "$tag" ]]; then
        echo "Unexpected argument: $1" >&2
        usage >&2
        exit 2
      fi
      tag="$1"
      shift
      ;;
  esac
done

if [[ -z "$tag" ]]; then
  echo "Missing release tag." >&2
  usage >&2
  exit 2
fi

if ! command -v gh >/dev/null 2>&1; then
  echo "GitHub CLI not found. Install gh and authenticate with 'gh auth login'." >&2
  exit 1
fi

if [[ "$apk_path" != /* ]]; then
  apk_path="$ROOT_DIR/$apk_path"
fi

if [[ ! -f "$apk_path" ]]; then
  echo "APK not found: $apk_path" >&2
  echo "Build one first, for example: scripts/build-apk.sh --release" >&2
  exit 1
fi

asset="$apk_path"
if [[ -n "$asset_label" ]]; then
  asset="${asset}#${asset_label}"
fi

gh_args=(release upload "$tag" "$asset")
if [[ "$clobber" == true ]]; then
  gh_args+=(--clobber)
fi
if [[ -n "$repo" ]]; then
  gh_args+=(--repo "$repo")
fi

echo "Uploading APK to GitHub Release: $tag"
echo "APK: $apk_path"
if [[ -n "$repo" ]]; then
  echo "Repository: $repo"
fi
if [[ -n "$asset_label" ]]; then
  echo "Asset label: $asset_label"
fi

gh "${gh_args[@]}"
