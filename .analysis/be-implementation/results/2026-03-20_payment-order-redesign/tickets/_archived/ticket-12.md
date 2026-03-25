# [Ticket #12] BillingKey 관리

## 개요
- TDD 참조: tdd.md 섹션 4.1.3 (billing_key 테이블), 4.2 (domain/payment/BillingKey.kt), 8.4
- 선행 티켓: #2
- 크기: M

## 작업 내용

### 변경 사항

1. **BillingKey entity 구현**
   - `workspaceId`: 워크스페이스 ID
   - `billingKeyValue`: PG 빌링키 값 (암호화 저장)
   - `cardCompany`: 카드사명
   - `cardNumberMasked`: 마스킹된 카드번호 (예: `****-****-****-1234`)
   - `email`: 결제 알림 이메일
   - `isPrimary`: 주 결제 수단 여부 (기본 true)
   - `gateway`: PG 이름 (기본 `TOSS`)
   - BaseEntity 상속 (soft delete 지원)

2. **billingKeyValue 암호화/복호화**
   - 기존 payment-server의 `encryption_key` 패턴 재사용
   - 저장 시 AES 암호화 → DB에는 암호문 저장
   - 조회 시 복호화하여 반환
   - JPA `@Convert` 또는 별도 암호화 서비스로 구현
   - 암호화 키는 환경변수/Vault에서 주입

3. **BillingKeyRepository 구현**
   - `findByWorkspaceIdAndDeletedAtIsNull(workspaceId: Int): List<BillingKey>`
   - `findByWorkspaceIdAndIsPrimaryTrueAndDeletedAtIsNull(workspaceId: Int): BillingKey?`
   - `findByIdAndDeletedAtIsNull(id: Long): BillingKey?`

4. **BillingKeyService 구현**
   - `register(workspaceId, billingKeyValue, cardCompany, cardNumberMasked, email?, gateway)`:
     - 기존 primary 빌링키가 있으면 isPrimary=false로 변경
     - 새 빌링키를 isPrimary=true로 등록
     - 빌링키 값 암호화 후 저장
   - `delete(billingKeyId: Long)`:
     - Soft delete (deleted_at 설정)
     - 삭제된 빌링키가 primary였다면 남은 것 중 가장 최근 등록 건을 primary로 승격
     - 빌링키가 없으면 `BillingKeyNotFoundException`
   - `changePrimary(workspaceId: Int, billingKeyId: Long)`:
     - 기존 primary → isPrimary=false
     - 지정 빌링키 → isPrimary=true
     - 해당 워크스페이스의 빌링키가 아니면 예외
   - `findByWorkspace(workspaceId: Int): List<BillingKey>`:
     - 해당 워크스페이스의 활성(deleted_at IS NULL) 빌링키 목록
   - `findPrimaryByWorkspace(workspaceId: Int): BillingKey`:
     - isPrimary=true인 빌링키 반환
     - 없으면 `BillingKeyNotFoundException`

5. **카드 만료 모니터링 (future placeholder)**
   - 이 티켓에서는 구현하지 않음
   - 향후 카드 만료일 필드 추가 및 만료 임박 알림 스케줄러 구현 예정
   - 코드에 TODO 주석으로 표시

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | domain | domain/payment/BillingKey.kt | 신규 |
| greeting_payment-server | domain | domain/payment/exception/BillingKeyNotFoundException.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/repository/BillingKeyRepository.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/encryption/BillingKeyEncryptor.kt | 신규 |
| greeting_payment-server | infrastructure | infrastructure/encryption/AesEncryptionConverter.kt | 신규 또는 기존 리팩토링 |
| greeting_payment-server | application | application/BillingKeyService.kt | 신규 |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T12-01 | 빌링키 등록 성공 (첫 번째) | 워크스페이스에 빌링키 없음 | register(ws1, key, "삼성", "****1234") | BillingKey(isPrimary=true) 저장 |
| T12-02 | 빌링키 등록 — 기존 primary 교체 | 기존 BillingKey(isPrimary=true) | register(ws1, newKey, ...) | 기존 isPrimary=false, 신규 isPrimary=true |
| T12-03 | 빌링키 soft delete | BillingKey(id=1) 존재 | delete(1) | deleted_at 설정 |
| T12-04 | 삭제 후 primary 자동 승격 | primary BillingKey 삭제, 다른 빌링키 존재 | delete(primaryId) | 남은 최신 건 isPrimary=true |
| T12-05 | primary 변경 | BillingKey A(primary), B(non-primary) | changePrimary(ws1, B.id) | A.isPrimary=false, B.isPrimary=true |
| T12-06 | 워크스페이스 빌링키 목록 | ws1에 2건 활성, 1건 삭제 | findByWorkspace(ws1) | 2건 반환 |
| T12-07 | primary 빌링키 조회 | isPrimary=true 1건 존재 | findPrimaryByWorkspace(ws1) | 해당 빌링키 반환 |
| T12-08 | 암호화 저장 확인 | billingKeyValue="toss_billing_123" | register() → DB 직접 조회 | 암호화된 값 저장됨 |
| T12-09 | 복호화 조회 확인 | 암호화된 값 DB 저장 | findByWorkspace() | 복호화된 원본 값 반환 |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T12-E01 | 존재하지 않는 빌링키 삭제 | id=999 없음 | delete(999) | BillingKeyNotFoundException |
| T12-E02 | 다른 워크스페이스 빌링키 primary 변경 | ws1의 빌링키를 ws2에서 변경 시도 | changePrimary(ws2, billingKeyOfWs1) | 예외 (권한 없음) |
| T12-E03 | primary 빌링키 없음 | 워크스페이스에 빌링키 0건 | findPrimaryByWorkspace(ws1) | BillingKeyNotFoundException |
| T12-E04 | 마지막 빌링키 삭제 | 빌링키 1건만 존재 | delete() | soft delete, primary 승격 대상 없음 (정상) |
| T12-E05 | 이미 삭제된 빌링키 재삭제 | deleted_at 이미 설정 | delete() | BillingKeyNotFoundException |

## 기대 결과 (AC)
- [ ] BillingKey entity가 workspaceId, billingKeyValue(암호화), cardInfo, isPrimary, gateway 필드를 가진다
- [ ] billingKeyValue가 AES 암호화되어 DB에 저장되고, 조회 시 복호화된다
- [ ] 기존 payment-server의 encryption_key 패턴과 호환된다
- [ ] register() 시 기존 primary가 자동으로 해제되고 신규가 primary가 된다
- [ ] delete()가 soft delete이며, primary 삭제 시 남은 건 중 자동 승격된다
- [ ] changePrimary()가 워크스페이스 소유권을 검증한다
- [ ] 단위 테스트 커버리지 80% 이상
