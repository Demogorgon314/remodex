#!/usr/bin/env bash

# FILE: CodexMobileAndroid/scripts/generate-release-secrets.sh
# Purpose: Generate a local Android release keystore and the GitHub Actions secrets needed by the release workflow.
# Layer: developer utility
# Exports: none
# Depends on: keytool, openssl, base64, gh (optional for --set-gh-secrets)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEFAULT_OUTPUT_DIR="/tmp/remodex-android-release-secrets"

OUTPUT_DIR="${DEFAULT_OUTPUT_DIR}"
KEYSTORE_NAME="remodex-android-release.keystore"
KEY_ALIAS="remodex-release"
VALIDITY_DAYS="9125"
DNAME="CN=Remodex, OU=Mobile, O=Remodex, L=Local, ST=Local, C=US"
KEYSTORE_PASSWORD=""
KEY_PASSWORD=""
SET_GH_SECRETS="false"
FORCE="false"
GH_REPO=""

log() {
  echo "[generate-release-secrets] $*"
}

die() {
  echo "[generate-release-secrets] $*" >&2
  exit 1
}

usage() {
  cat <<EOF
Usage: $(basename "$0") [options]

Options:
  --output-dir DIR          Directory to write the keystore and generated secret files
                            Default: ${DEFAULT_OUTPUT_DIR}
  --keystore-name NAME      Keystore file name
                            Default: ${KEYSTORE_NAME}
  --alias NAME              Android signing key alias
                            Default: ${KEY_ALIAS}
  --dname VALUE             Distinguished name passed to keytool
                            Default: ${DNAME}
  --validity-days DAYS      Keystore validity in days
                            Default: ${VALIDITY_DAYS}
  --keystore-password PASS  Use an explicit keystore password instead of generating one
  --key-password PASS       Optional separate key password; defaults to the keystore password
  --set-gh-secrets          Push the generated values to GitHub repository secrets via gh
  --repo OWNER/REPO         GitHub repo to target with --set-gh-secrets
  --force                   Overwrite an existing output directory contents
  --help                    Show this help text

Generated files:
  <output-dir>/<keystore-name>
  <output-dir>/github-secrets.env

Secrets created:
  ANDROID_KEYSTORE_BASE64
  ANDROID_KEYSTORE_PASSWORD
  ANDROID_KEY_ALIAS
  ANDROID_KEY_PASSWORD
EOF
}

require_command() {
  local command_name="$1"
  command -v "${command_name}" >/dev/null 2>&1 || die "Missing required command: ${command_name}"
}

random_secret() {
  openssl rand -hex 24
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --output-dir)
        [[ $# -ge 2 ]] || die "--output-dir requires a value"
        OUTPUT_DIR="$2"
        shift 2
        ;;
      --keystore-name)
        [[ $# -ge 2 ]] || die "--keystore-name requires a value"
        KEYSTORE_NAME="$2"
        shift 2
        ;;
      --alias)
        [[ $# -ge 2 ]] || die "--alias requires a value"
        KEY_ALIAS="$2"
        shift 2
        ;;
      --dname)
        [[ $# -ge 2 ]] || die "--dname requires a value"
        DNAME="$2"
        shift 2
        ;;
      --validity-days)
        [[ $# -ge 2 ]] || die "--validity-days requires a value"
        VALIDITY_DAYS="$2"
        shift 2
        ;;
      --keystore-password)
        [[ $# -ge 2 ]] || die "--keystore-password requires a value"
        KEYSTORE_PASSWORD="$2"
        shift 2
        ;;
      --key-password)
        [[ $# -ge 2 ]] || die "--key-password requires a value"
        KEY_PASSWORD="$2"
        shift 2
        ;;
      --set-gh-secrets)
        SET_GH_SECRETS="true"
        shift
        ;;
      --repo)
        [[ $# -ge 2 ]] || die "--repo requires a value"
        GH_REPO="$2"
        shift 2
        ;;
      --force)
        FORCE="true"
        shift
        ;;
      --help)
        usage
        exit 0
        ;;
      *)
        die "Unknown argument: $1"
        ;;
    esac
  done
}

ensure_output_dir() {
  if [[ -e "${OUTPUT_DIR}" && "${FORCE}" != "true" ]]; then
    if [[ -n "$(find "${OUTPUT_DIR}" -mindepth 1 -maxdepth 1 2>/dev/null | head -n 1)" ]]; then
      die "Output directory already contains files: ${OUTPUT_DIR}. Re-run with --force or choose another directory."
    fi
  fi

  mkdir -p "${OUTPUT_DIR}"
  chmod 700 "${OUTPUT_DIR}"
}

write_env_file() {
  local env_file="$1"
  local keystore_base64="$2"

  cat > "${env_file}" <<EOF
ANDROID_KEYSTORE_BASE64=${keystore_base64}
ANDROID_KEYSTORE_PASSWORD=${KEYSTORE_PASSWORD}
ANDROID_KEY_ALIAS=${KEY_ALIAS}
ANDROID_KEY_PASSWORD=${KEY_PASSWORD}
EOF

  chmod 600 "${env_file}"
}

set_github_secret() {
  local name="$1"
  local value="$2"

  if [[ -n "${GH_REPO}" ]]; then
    printf '%s' "${value}" | gh secret set "${name}" --repo "${GH_REPO}"
  else
    printf '%s' "${value}" | gh secret set "${name}"
  fi
}

main() {
  parse_args "$@"

  require_command keytool
  require_command openssl
  require_command base64

  if [[ "${SET_GH_SECRETS}" == "true" ]]; then
    require_command gh
  fi

  if [[ -z "${KEYSTORE_PASSWORD}" ]]; then
    KEYSTORE_PASSWORD="$(random_secret)"
  fi

  if [[ -z "${KEY_PASSWORD}" ]]; then
    KEY_PASSWORD="${KEYSTORE_PASSWORD}"
  fi

  [[ "${VALIDITY_DAYS}" =~ ^[0-9]+$ ]] || die "--validity-days must be numeric"

  ensure_output_dir

  local keystore_path="${OUTPUT_DIR}/${KEYSTORE_NAME}"
  local env_file="${OUTPUT_DIR}/github-secrets.env"

  if [[ -e "${keystore_path}" && "${FORCE}" != "true" ]]; then
    die "Keystore already exists at ${keystore_path}. Re-run with --force or change --output-dir/--keystore-name."
  fi

  rm -f "${keystore_path}" "${env_file}"

  keytool -genkeypair \
    -keystore "${keystore_path}" \
    -storepass "${KEYSTORE_PASSWORD}" \
    -alias "${KEY_ALIAS}" \
    -keypass "${KEY_PASSWORD}" \
    -keyalg RSA \
    -keysize 4096 \
    -validity "${VALIDITY_DAYS}" \
    -dname "${DNAME}"

  chmod 600 "${keystore_path}"

  local keystore_base64
  keystore_base64="$(base64 < "${keystore_path}" | tr -d '\n')"

  write_env_file "${env_file}" "${keystore_base64}"

  if [[ "${SET_GH_SECRETS}" == "true" ]]; then
    set_github_secret "ANDROID_KEYSTORE_BASE64" "${keystore_base64}"
    set_github_secret "ANDROID_KEYSTORE_PASSWORD" "${KEYSTORE_PASSWORD}"
    set_github_secret "ANDROID_KEY_ALIAS" "${KEY_ALIAS}"
    set_github_secret "ANDROID_KEY_PASSWORD" "${KEY_PASSWORD}"
  fi

  log "Keystore written to ${keystore_path}"
  log "GitHub secrets file written to ${env_file}"

  cat <<EOF

Next steps:
  1. Back up these two files somewhere safe:
     - ${keystore_path}
     - ${env_file}
  2. Add the four values from ${env_file} to your GitHub repository secrets.
  3. Trigger the "Android Release" workflow and provide a version_name like 1.2.3.

If you prefer GitHub CLI to set secrets for you directly, re-run:
  $(basename "$0") --set-gh-secrets --force --output-dir "${OUTPUT_DIR}"${GH_REPO:+ --repo "${GH_REPO}"}
EOF
}

main "$@"
