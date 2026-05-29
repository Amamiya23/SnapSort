#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOCAL_GRADLE="/home/cat/.gradle/wrapper/dists/gradle-8.12-all/btiplb0pmjji56hfpl9949cgc/gradle-8.12/bin/gradle"

build_type="release"
version_name="${VERSION_NAME:-}"
version_code="${VERSION_CODE:-}"

usage() {
  cat <<'USAGE'
Usage: scripts/build-apk.sh [--release|--debug] [--name VERSION_NAME]

Build SnapSort with automatically generated Android version metadata.

Options:
  --release              Build release APK (default)
  --debug                Build debug APK
  --name VERSION_NAME    Override generated versionName
  -h, --help             Show this help

Environment:
  VERSION_CODE           Override generated versionCode
  VERSION_NAME           Override generated versionName
  GRADLE_BIN             Override Gradle executable path
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --release)
      build_type="release"
      shift
      ;;
    --debug)
      build_type="debug"
      shift
      ;;
    --name|--version-name)
      if [[ $# -lt 2 ]]; then
        echo "Missing value for $1" >&2
        exit 2
      fi
      version_name="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if [[ -z "$version_code" ]]; then
  version_code="$(date +%s)"
fi

if ! [[ "$version_code" =~ ^[0-9]+$ ]] || (( version_code <= 0 || version_code > 2100000000 )); then
  echo "VERSION_CODE must be a positive integer no larger than 2100000000." >&2
  exit 2
fi

if [[ -z "$version_name" ]]; then
  version_name="v$(date +%Y.%m.%d-%H%M)"
fi

case "$build_type" in
  release)
    gradle_task=":app:assembleRelease"
    ;;
  debug)
    gradle_task=":app:assembleDebug"
    ;;
esac

if [[ -n "${GRADLE_BIN:-}" ]]; then
  gradle_cmd=("$GRADLE_BIN")
elif [[ -x "$ROOT_DIR/gradlew" ]]; then
  gradle_cmd=("$ROOT_DIR/gradlew")
elif [[ -x "$LOCAL_GRADLE" ]]; then
  gradle_cmd=("$LOCAL_GRADLE")
elif command -v gradle >/dev/null 2>&1; then
  gradle_cmd=("gradle")
else
  echo "Gradle executable not found. Set GRADLE_BIN to your Gradle path." >&2
  exit 1
fi

echo "Building SnapSort ${build_type} APK"
echo "VERSION_CODE=${version_code}"
echo "VERSION_NAME=${version_name}"

cd "$ROOT_DIR"
"${gradle_cmd[@]}" "$gradle_task" "-PVERSION_CODE=$version_code" "-PVERSION_NAME=$version_name"
