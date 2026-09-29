# Secure File Upload

현재 단계는 화면 없이, 확장자 차단 정책과 단일 파일 업로드를 제공하는 REST API입니다.

## 실행

PostgreSQL 데이터베이스를 준비한 뒤 다음 환경 변수를 설정합니다.

```text
DATABASE_URL=jdbc:postgresql://localhost:5432/secure_file_upload
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres
UPLOAD_DIRECTORY=./uploads
```

Java 21 환경에서 Gradle Wrapper로 실행합니다.

```text
.\\gradlew.bat bootRun
.\\gradlew.bat test
```

Neon 연결 문자열은 JDBC URL 형태로 `DATABASE_URL`에 설정합니다. Flyway가 시작 시 테이블과 고정 확장자 초기 데이터를 생성합니다.

## API

- `GET /api/extension-policies/fixed`
- `PATCH /api/extension-policies/fixed/{id}` — `{ "blocked": true }`
- `GET /api/extension-policies/custom`
- `POST /api/extension-policies/custom` — `{ "extension": ".pdf" }`
- `DELETE /api/extension-policies/custom/{id}`
- `POST /api/files` — `multipart/form-data`의 `file` 필드

업로드 파일은 `UPLOAD_DIRECTORY`(기본값 `./uploads`)에 UUID 기반 이름으로 저장하며, Spring 정적 리소스 디렉터리와 분리됩니다.
무료 배포 환경의 로컬 파일 시스템은 재시작이나 재배포 후 유지되지 않을 수 있으므로, 이 저장소는 장기 보관 용도가 아닙니다.
