# Secure File Upload

현재 단계는 파일 업로드 기능이나 화면 없이, 확장자 차단 정책을 관리하는 REST API만 제공합니다.

## 실행

PostgreSQL 데이터베이스를 준비한 뒤 다음 환경 변수를 설정합니다.

```text
DATABASE_URL=jdbc:postgresql://localhost:5432/secure_file_upload
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres
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
