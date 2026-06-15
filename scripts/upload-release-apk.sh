#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_APK="$ROOT_DIR/app/build/outputs/apk/release/app-release.apk"

apk_path="${APK_PATH:-$DEFAULT_APK}"
asset_label="${ASSET_LABEL:-}"
repo="${GH_REPO:-}"
release_notes="${RELEASE_NOTES:-}"
release_notes_file="${RELEASE_NOTES_FILE:-}"
clobber=false

usage() {
  cat <<'USAGE'
Usage: scripts/upload-release-apk.sh <tag> [--apk APK_PATH] [--repo OWNER/REPO] [--label LABEL] [--notes NOTES|--notes-file FILE] [--clobber]

Upload a compiled SnapSort APK to an existing GitHub Release using gh, with optional release notes update.

Arguments:
  tag                   Git tag for the target GitHub Release

Options:
  --apk APK_PATH        APK to upload (default: app/build/outputs/apk/release/app-release.apk)
  --repo OWNER/REPO     GitHub repository passed to gh --repo
  --label LABEL         Display label for the uploaded release asset
  --notes NOTES         Replace release notes with this text before uploading the APK
  --notes-file FILE     Replace release notes from a Markdown file before uploading the APK
  --clobber             Overwrite an existing release asset with the same name
  -h, --help            Show this help

Environment:
  APK_PATH              Override APK path
  ASSET_LABEL           Override release asset display label
  GH_REPO               Override GitHub repository passed to gh --repo
  RELEASE_NOTES         Override release notes text
  RELEASE_NOTES_FILE    Override release notes file
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
    --notes)
      if [[ $# -lt 2 ]]; then
        echo "Missing value for $1" >&2
        exit 2
      fi
      release_notes="$2"
      shift 2
      ;;
    --notes-file)
      if [[ $# -lt 2 ]]; then
        echo "Missing value for $1" >&2
        exit 2
      fi
      release_notes_file="$2"
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

if [[ -n "$release_notes" && -n "$release_notes_file" ]]; then
  echo "Use only one of --notes or --notes-file." >&2
  exit 2
fi

if [[ "$apk_path" != /* ]]; then
  apk_path="$ROOT_DIR/$apk_path"
fi

if [[ ! -f "$apk_path" ]]; then
  echo "APK not found: $apk_path" >&2
  echo "Build one first, for example: scripts/build-apk.sh --release" >&2
  exit 1
fi

if [[ -n "$release_notes_file" && "$release_notes_file" != "-" ]]; then
  if [[ "$release_notes_file" != /* ]]; then
    release_notes_file="$ROOT_DIR/$release_notes_file"
  fi

  if [[ ! -f "$release_notes_file" ]]; then
    echo "Release notes file not found: $release_notes_file" >&2
    exit 1
  fi
fi

asset="$apk_path"
if [[ -n "$asset_label" ]]; then
  asset="${asset}#${asset_label}"
fi

repo_args=()
if [[ -n "$repo" ]]; then
  repo_args+=(--repo "$repo")
fi

echo "Uploading APK to GitHub Release: $tag"
echo "APK: $apk_path"
if [[ -n "$repo" ]]; then
  echo "Repository: $repo"
fi
if [[ -n "$asset_label" ]]; then
  echo "Asset label: $asset_label"
fi

if [[ -n "$release_notes" ]]; then
  echo "Updating release notes from --notes"
  gh release edit "$tag" --notes "$release_notes" "${repo_args[@]}"
elif [[ -n "$release_notes_file" ]]; then
  echo "Updating release notes from: $release_notes_file"
  gh release edit "$tag" --notes-file "$release_notes_file" "${repo_args[@]}"
fi

gh_args=(release upload "$tag" "$asset")
if [[ "$clobber" == true ]]; then
  gh_args+=(--clobber)
fi
gh_args+=("${repo_args[@]}")

gh "${gh_args[@]}"
