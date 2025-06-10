# 인증 및 권한 관리

## 1. GitHub OIDC란?

GitHub Actions 워크플로우가 AWS에 신원을 증명하는 방법입니다.

- GitHub가 ID 제공자(IdP) 역할을 하여,
- Actions가 "내가 특정 레포지토리의 워크플로우임"을 AWS에 증명하는 토큰을 발급받습니다.

## 2. AWS IAM Role과 Assume Role

AWS IAM Role은 권한을 가진 역할을 의미하며,

- GitHub Actions가 발급받은 토큰으로 해당 역할을 맡아(assume role),
- 필요한 AWS 리소스(예: EC2 SSM 명령 실행 권한)에 접근할 수 있게 합니다.

## 3. OIDC + IAM Role 연동 구성

- GitHub 측: OIDC 신뢰 정책 설정
- AWS 측: IAM Role 정책 설정 및 GitHub Actions에 권한 위임

## 4. 보안 고려 사항

- 최소 권한 원칙 적용
- 특정 GitHub 리포지토리로 권한 범위 제한

---

## 개념 요약

GitHub OIDC + AWS IAM Role은

특정 GitHub 리포지토리 내 워크플로우가

안전하게 AWS 리소스에 권한을 위임받아 접근할 수 있도록 하는 인증 및 권한 부여 메커니즘입니다.
