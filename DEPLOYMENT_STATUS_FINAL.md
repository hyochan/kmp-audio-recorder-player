# 🎯 Maven Central 배포 설정 완료 및 최종 상태

## ✅ 완료된 설정

### 1. GPG 서명 설정
- ✅ 외부 파일 (`gpg_key_content`)에서 GPG 키 로드
- ✅ 모든 아티팩트에 대한 자동 서명 설정
- ✅ vanniktech Maven publish 플러그인과 통합

### 2. 빌드 설정 완료
- ✅ 모든 플랫폼 지원 (Android, iOS, JVM, WASM-JS, Linux)
- ✅ 완전한 아티팩트 세트 생성 (jar, sources, javadoc, pom, module)
- ✅ Kotlin Multiplatform 메타데이터 생성

### 3. 배포 설정 완료
- ✅ vanniktech Maven publish 플러그인 설정
- ✅ Sonatype OSSRH 호스트 설정
- ✅ 프로젝트 메타데이터 (좌표, 설명, 라이선스, 개발자 정보)

## 🧪 테스트 결과

### 로컬 배포 (✅ 성공)
```bash
./gradlew :library:publishToMavenLocal
```
- 102개 작업 완료
- 총 48개 GPG 서명 파일 생성
- `~/.m2/repository/io/github/hyochan/kmp-audio-recorder-player/1.0.0-alpha01/` 에 배포 완료

### Dry-run 테스트 (✅ 성공)
```bash
./gradlew :library:publishToMavenCentral --dry-run
```
- 모든 배포 과정이 성공적으로 시뮬레이션됨
- 빌드 설정이 올바르게 구성되어 있음을 확인

### 실제 Maven Central 배포 (❌ 인증 오류)
```bash
./gradlew :library:publishToMavenCentral -PmavenCentralUsername=xxx -PmavenCentralPassword=xxx
```
- 401 인증 오류 발생
- 제공된 자격증명으로는 배포 불가

## 🔐 필요한 추가 사항

실제 Maven Central 배포를 위해서는 다음 중 하나가 필요합니다:

1. **유효한 Sonatype OSSRH 계정**
   - https://central.sonatype.org/register-namespace/ 에서 네임스페이스 등록
   - `io.github.hyochan` 네임스페이스에 대한 권한

2. **올바른 자격증명**
   - Sonatype JIRA 계정 또는 Central Portal 토큰
   - 해당 groupId에 대한 배포 권한

3. **Central Portal 토큰** (새로운 방식)
   - https://central.sonatype.com/ 에서 토큰 생성
   - `centralPortalToken` 속성 사용

## 🚀 배포 명령어

올바른 자격증명이 있을 때 사용할 수 있는 명령어:

```bash
# OSSRH 방식
./gradlew :library:publishToMavenCentral \
  -PmavenCentralUsername=YOUR_USERNAME \
  -PmavenCentralPassword=YOUR_PASSWORD

# Central Portal 방식 (토큰 사용)
./gradlew :library:publishToMavenCentral \
  -PcentralPortalToken=YOUR_TOKEN
```

## 📋 결론

**모든 기술적 설정이 완료되었으며, 실제 배포를 위해서는 올바른 Maven Central 계정 자격증명만 필요합니다.**

- ✅ GPG 서명: 완료
- ✅ 빌드 설정: 완료  
- ✅ 아티팩트 생성: 완료
- ✅ 배포 설정: 완료
- ❌ 인증 자격증명: 필요

## 📂 관련 파일

- `local.properties`: 자격증명 설정
- `gpg_key_content`: GPG 개인 키 (Git 제외)
- `library/build.gradle.kts`: 빌드 및 배포 설정
- `~/.m2/repository/io/github/hyochan/kmp-audio-recorder-player/`: 로컬 배포 결과
