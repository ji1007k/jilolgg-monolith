#!/usr/bin/env bash
# Claude 이슈 작업이 실제로 "완료"됐는지 판별한다.
# GitHub의 초록 체크(conclusion=success)는 완료 신호가 아니다.
# 편집 도구가 없어 아무것도 못 고쳐도 success로 끝나기 때문이다(이슈 #45).
#
# 사용법: bin/check-claude-run.sh [run_id]
#   run_id를 생략하면 claude.yml의 가장 최근 실행을 본다.
set -u
REPO="${REPO:-ji1007k/jilolgg-monolith}"

RUN_ID="${1:-}"
if [ -z "$RUN_ID" ]; then
  RUN_ID=$(gh run list --repo "$REPO" --workflow claude.yml --limit 1 --json databaseId --jq '.[0].databaseId')
fi
echo "run: $RUN_ID  ($REPO)"

LOG=$(gh run view "$RUN_ID" --repo "$REPO" --log 2>/dev/null)

TURNS=$(printf '%s' "$LOG" | grep -oE '"num_turns": *[0-9]+' | grep -oE '[0-9]+$' | head -1)
IS_ERROR=$(printf '%s' "$LOG" | grep -oE '"is_error": *(true|false)' | grep -oE '(true|false)$' | head -1)
DENIALS=$(printf '%s' "$LOG" | grep -oE '"permission_denials_count": *[0-9]+' | grep -oE '[0-9]+$' | head -1)
HAS_EDIT=$(printf '%s' "$LOG" | grep -cE '"(Edit|Write)"')
NO_BRANCH=$(printf '%s' "$LOG" | grep -c "Error checking for changes in branch")
BRANCHES=$(git ls-remote --heads upstream 'refs/heads/claude/*' 2>/dev/null | wc -l | tr -d ' ')

echo "  턴 수            : ${TURNS:-?}"
echo "  is_error         : ${IS_ERROR:-?}"
echo "  권한 거부 횟수   : ${DENIALS:-?}"
echo "  Edit/Write 보유  : $([ "$HAS_EDIT" -gt 0 ] && echo yes || echo 'NO  <- 이게 no면 절대 못 고친다')"
echo "  브랜치 생성 실패 : $([ "$NO_BRANCH" -gt 0 ] && echo 'yes <- 아무것도 커밋 안 함' || echo no)"
echo "  claude/* 브랜치  : $BRANCHES 개"
echo

if [ "$HAS_EDIT" -eq 0 ]; then
  echo "판정: 미완료 — 편집 도구가 없다. claude.yml의 --allowedTools를 확인할 것."
elif [ "$NO_BRANCH" -gt 0 ] || [ "$BRANCHES" -eq 0 ]; then
  echo "판정: 미완료 — 코드 변경이 없다."
else
  echo "판정: 완료로 보임 — PR이 열렸는지 확인:  gh pr list --repo $REPO --limit 5"
fi
