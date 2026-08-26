# 모바일에서 지시해 수정·배포하기

폰에서 이슈나 PR에 `@claude ...` 라고 코멘트하면 Claude가 코드를 고쳐 PR을 여는 구성이다.
배포는 그 PR을 머지하면 기존 파이프라인이 이어받는다.

```
폰(깃허브 앱) ── "@claude 로그인 버튼 색 바꿔줘"
      │
      └─> claude.yml ── 코드 수정 -> 브랜치 -> PR 생성
                              │
                              └─> build.yml (테스트/빌드 검증)
                                        │
                            사람이 develop -> main 머지
                                        │
                              release-main.yml ── 이미지 빌드 -> 배포
```

## 1. 최초 설정 (한 번만)

세 가지를 등록한다. 성격이 서로 다르다.

| 무엇 | 정체 | 왜 필요한가 |
| --- | --- | --- |
| Claude GitHub 앱 | GitHub 앱(계정 권한) | Claude가 이 저장소의 코멘트를 읽고 브랜치·PR을 만들 수 있게 함 |
| `CLAUDE_CODE_OAUTH_TOKEN` | Claude 인증 토큰 | Actions 안에서 도는 Claude가 "누구 계정으로" 동작할지 |
| `BOT_PAT` | GitHub Personal Access Token | Claude가 연 PR에도 CI 검증이 붙게 하려고 |

### 1-1. Claude GitHub 앱 설치

<https://github.com/apps/claude> 에 접속해 **Install** 을 누른다.
설치 대상으로 `ji1007k/jilolgg-monolith` 저장소를 고른다(전체 저장소 대신 이 저장소만 골라도 된다).

저장소 관리자 권한이 필요하다.

### 1-2. `CLAUDE_CODE_OAUTH_TOKEN` 발급

Claude **Pro / Max 구독**을 쓰는 경우다. 별도 API 과금이 붙지 않는다.

로컬 터미널에서 Claude Code CLI로 발급한다.

```bash
claude setup-token
```

출력된 토큰 문자열을 그대로 시크릿 값으로 넣는다.

**API 키로 과금하려면** 대신 `ANTHROPIC_API_KEY`(`sk-ant-`로 시작)를 등록하고
`.github/workflows/claude.yml`의 입력을 `anthropic_api_key`로 바꾼다.
API 키는 Anthropic 콘솔에서 발급한다. **두 가지를 동시에 넣지 말 것.**

### 1-3. `BOT_PAT` 발급

이름은 이 저장소에서 임의로 정한 것이다. 실체는 **GitHub Personal Access Token**이다.

<https://github.com/settings/tokens> → **Generate new token (classic)**

- 스코프: **`repo`** 와 **`workflow`** 두 개를 체크
- 만료일: 만료되면 조용히 동작이 바뀌므로(아래 참조) 달력에 적어둘 것

**왜 필요한가** — GitHub은 `GITHUB_TOKEN`으로 만든 PR에는 **다른 워크플로를 발동시키지 않는다.**
그대로 두면 Claude가 연 PR에 `build.yml`(테스트/빌드 검증)이 붙지 않아,
검증되지 않은 변경을 그대로 머지하게 된다.

`BOT_PAT`가 없거나 만료되면 워크플로는 실패하지 않고 `GITHUB_TOKEN`으로 **조용히** 동작한다.
PR은 열리지만 CI가 안 붙는다. Claude가 연 PR에 체크가 하나도 없으면 이걸 의심할 것.

### 1-4. 시크릿 등록

저장소 → **Settings → Secrets and variables → Actions → New repository secret**

| Name | Secret |
| --- | --- |
| `CLAUDE_CODE_OAUTH_TOKEN` | 1-2에서 받은 토큰 |
| `BOT_PAT` | 1-3에서 받은 토큰 |

이름은 `.github/workflows/claude.yml`이 참조하는 것과 정확히 일치해야 한다.

## 2. 쓰는 법

폰에서 깃허브 앱을 열고 이슈를 하나 만들거나 기존 이슈에 코멘트한다.

```
@claude 주간 일정 카드의 팀 로고가 안 보여. 확인해서 고쳐줘.
```

```
@claude MatchItemWriter가 매 chunk마다 전건을 저장하는 문제 고쳐줘.
docs/agent-authoring-guide.md 규칙 따라서.
```

Claude는 저장소 루트의 `CLAUDE.md`를 읽고 프로젝트 규칙(빌드 명령, 아키텍처,
커밋 컨벤션, 전담 에이전트 위임 규칙)을 따른다. 지시에 그 문서를 다시 설명할 필요 없다.

## 3. 배포까지

Claude는 **PR을 열 뿐 머지하지 않는다.** 머지는 사람이 한다.

1. Claude가 연 PR에서 `build.yml` 검증 통과 확인
2. `develop`에 머지
3. `develop -> main` PR을 열고 머지
4. `release-main.yml`이 이미지를 굽고 같은 실행의 `deploy` 잡이 Railway에 배포

자동 머지는 켜지 않았다. CI가 테스트를 돌리게 됐지만, 검토 없이 운영에 나가는 것은
별개의 결정이다.

## 4. 한계

- **PR 검증은 외부 API 테스트를 제외하고 돈다**(`-PexcludeExternalApiTests`). 외부 상태로
  실패하면 검증을 신뢰할 수 없기 때문이다. 그 영역은 로컬에서 확인한다
- Claude가 연 PR도 사람 PR과 같은 규칙을 받는다. 특별 취급하지 않는다
- 지시가 모호하면 Claude가 되묻는 대신 추정해서 진행할 수 있다. 이슈 본문에 무엇을
  바꿔야 하는지 구체적으로 적는 편이 결과가 낫다

## 5. 대안: 헤르메스 에이전트

상주형 에이전트로 같은 목표를 달성하는 방법도 있다. 다만 **꺼지지 않는 호스트가 필요하다.**
WSL2에 올리면 `loginctl enable-linger` 없이는 세션이 닫힐 때 죽고, 노트북이 꺼져 있으면
지시를 받지 못한다. 검토 기록은 `docs/future-work.md` 참조.
