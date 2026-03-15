#!/usr/bin/env bash
# k6 전체 테스트 실행 스크립트
# 결과는 tests/k6/results/ 에 JSON으로 저장됨

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESULTS_DIR="${SCRIPT_DIR}/results"
API_BASE="${API_BASE:-http://localhost:8081}"

# 결과 디렉토리 생성
mkdir -p "${RESULTS_DIR}"

echo "========================================"
echo " k6 QA Load Test Suite"
echo " API_BASE: ${API_BASE}"
echo " Results: ${RESULTS_DIR}"
echo "========================================"

# k6 설치 확인
if ! command -v k6 &>/dev/null; then
  echo "[ERROR] k6 is not installed."
  echo "  brew install k6  (macOS)"
  echo "  sudo apt install k6  (Ubuntu)"
  exit 1
fi

FAILED=0
PASSED=0

run_scenario() {
  local name="$1"
  local script="$2"
  local result_file="${RESULTS_DIR}/${name}.json"

  echo ""
  echo "--- Running: ${name} ---"

  if k6 run \
    --env API_BASE="${API_BASE}" \
    --out json="${result_file}" \
    "${script}"; then
    echo "[PASS] ${name}"
    PASSED=$((PASSED + 1))
  else
    echo "[FAIL] ${name}"
    FAILED=$((FAILED + 1))
  fi
}

# 각 시나리오 실행
run_scenario "01-auth" "${SCRIPT_DIR}/scenarios/01-auth.js"
run_scenario "02-documents" "${SCRIPT_DIR}/scenarios/02-documents.js"
run_scenario "03-search" "${SCRIPT_DIR}/scenarios/03-search.js"
run_scenario "04-full-user-journey" "${SCRIPT_DIR}/scenarios/04-full-user-journey.js"
run_scenario "load-test" "${SCRIPT_DIR}/load-test.js"

echo ""
echo "========================================"
echo " Test Results"
echo "  PASSED: ${PASSED}"
echo "  FAILED: ${FAILED}"
echo "========================================"

if [ "${FAILED}" -gt 0 ]; then
  echo "[FAIL] One or more k6 tests failed."
  exit 1
else
  echo "[SUCCESS] All k6 tests passed."
  exit 0
fi
