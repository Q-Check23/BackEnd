## 📋 관련 이슈

<!-- 관련 이슈 번호를 입력해주세요. 예: #2 -->
Closes #

---

## 📝 변경 사항

<!-- 변경 사항을 간단히 설명해주세요. -->

-
-
-

---

## 🔄 변경 유형

<!-- 해당하는 항목에 [x]로 체크해주세요. -->

- [ ] ✨ Feature - 새 기능 추가
- [ ] 🔧 Change - 기존 기능 변경/삭제
- [ ] 🐛 Bug - 버그 수정
- [ ] 📚 Docs - 문서 수정
- [ ] ♻️ Refactor - 코드 리팩토링
- [ ] ⚙️ CI - CI/CD 설정 변경

---

## 🔌 API 변경 사항 (해당 시)

<!-- API 변경이 있는 경우 작성해주세요. -->

| Method | Endpoint | 변경 내용 |
|--------|----------|-----------|
|        |          |           |

### Breaking Change
- [ ] ⚠️ Breaking Change 있음 (기존 API 변경)
- [ ] ✅ Breaking Change 없음

---

## 🧪 테스트

### 체크리스트

- [ ] `./gradlew build` 성공
- [ ] `./gradlew test` 통과
- [ ] 로컬에서 API 테스트 완료
- [ ] Postman/Swagger로 동작 확인

### 테스트 방법

<!-- 리뷰어가 테스트할 수 있는 방법을 작성해주세요. -->

```bash
# 예시
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{"title": "테스트"}'
```

---

## 📌 추가 정보

<!-- 리뷰어가 알아야 할 추가 정보가 있다면 작성해주세요. -->
