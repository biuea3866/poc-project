# [Ticket #3] Soft Delete BaseEntity

## 개요
- TDD 참조: tdd.md 섹션 4.2 (도메인 모델 - 공통 구조)
- 선행 티켓: #1 (DB 스키마 - deleted_at 컬럼 존재 확인)
- 크기: S

## 작업 내용

### 변경 사항

모든 Soft Delete 대상 엔티티가 상속할 `BaseEntity` 추상 클래스를 구현한다. JPA Auditing을 활용하여 생성/수정 시각을 자동 관리하고, Optimistic Lock을 위한 version 필드를 포함한다.

#### BaseEntity 구현

```kotlin
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    var createdAt: LocalDateTime = LocalDateTime.now()
        protected set

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    var updatedAt: LocalDateTime = LocalDateTime.now()
        protected set

    @Column(name = "deleted_at", columnDefinition = "DATETIME(6)")
    var deletedAt: LocalDateTime? = null
        protected set

    @Version
    @Column(name = "version", nullable = false)
    var version: Int = 0
        protected set

    fun softDelete() {
        this.deletedAt = LocalDateTime.now()
    }

    fun isDeleted(): Boolean = deletedAt != null
}
```

#### JPA Auditing 설정

```kotlin
@Configuration
@EnableJpaAuditing
class JpaAuditingConfig
```

#### 설계 결정
- `@MappedSuperclass`: 테이블 상속 전략이 아닌 필드 상속만 사용
- `@CreatedDate` + `@LastModifiedDate`: Spring Data JPA Auditing으로 자동 관리
- `@Version`: 모든 BaseEntity 상속 엔티티에 version 필드 포함. 실제 낙관적 락이 필요한 엔티티(Order, Subscription, CreditBalance)에서 활용
- `deletedAt`: nullable. NULL이면 활성, NOT NULL이면 삭제됨
- `softDelete()`: 명시적 소프트 삭제 메서드 제공 (엔티티 내부 로직에서 사용)
- Setter는 `protected set`으로 외부 직접 수정 차단

#### BaseEntity를 상속하는 엔티티 목록
| 엔티티 | @Version 활용 | @SQLRestriction | @SQLDelete |
|--------|:---:|:---:|:---:|
| Product | - | O | O |
| Order | O (동시 상태 변경 방지) | O | O |
| Payment | - | O | O |
| BillingKey | - | O | O |
| Refund | - | O | O |
| Subscription | O (동시 갱신 방지) | O | O |

#### BaseEntity를 상속하지 않는 엔티티
| 엔티티 | 이유 |
|--------|------|
| ProductMetadata | 수정 없는 key-value, version/deleted_at 불필요 |
| ProductPrice | 이력성 테이블, append-only |
| OrderItem | 주문 생성 시 1회 기록, 수정 없음 |
| CreditBalance | version은 자체 관리, deleted_at 불필요 |
| CreditLedger | 원장 기록, append-only |
| OrderStatusHistory | 이력 기록, append-only |
| PaymentStatusHistory | 이력 기록, append-only |
| PgWebhookLog | 로그성 데이터, append-only |

### 수정 파일 목록
| 레포 | 모듈 | 파일 경로 | 변경 유형 |
|------|------|----------|----------|
| greeting_payment-server | domain/common | BaseEntity.kt | 신규 |
| greeting_payment-server | infrastructure/config | JpaAuditingConfig.kt | 신규 |

## 테스트 케이스

### 정상 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T3-01 | createdAt 자동 설정 | BaseEntity 상속 엔티티 신규 생성 | repository.save() | createdAt이 현재 시각으로 자동 설정됨 |
| T3-02 | updatedAt 자동 갱신 | 엔티티 저장 후 1초 대기 | 필드 수정 후 repository.save() | updatedAt이 createdAt보다 이후 시각 |
| T3-03 | createdAt 불변 | 엔티티 저장 완료 | 필드 수정 후 save() | createdAt은 최초 값 유지 (updatable=false) |
| T3-04 | version 자동 증가 | 엔티티(version=0) 저장 | 필드 수정 후 save() | version이 1로 증가 |
| T3-05 | softDelete() 호출 | 활성 엔티티 (deletedAt=null) | softDelete() 호출 후 save() | deletedAt에 현재 시각 설정됨 |
| T3-06 | isDeleted() true 반환 | softDelete() 호출된 엔티티 | isDeleted() | true 반환 |
| T3-07 | isDeleted() false 반환 | 활성 엔티티 (deletedAt=null) | isDeleted() | false 반환 |
| T3-08 | protected setter 캡슐화 | 외부 코드에서 | entity.createdAt = ... 직접 대입 시도 | 컴파일 에러 (protected set) |

### 예외/엣지 케이스
| ID | 테스트명 | Given | When | Then |
|----|---------|-------|------|------|
| T3-E01 | 낙관적 락 충돌 | 같은 엔티티 2개 영속성 컨텍스트에서 로드 (version=0) | 양쪽 모두 수정 후 save() | 먼저 저장한 쪽 성공, 나중 쪽 OptimisticLockException |
| T3-E02 | deletedAt null 초기값 | 새 엔티티 생성 | 엔티티 인스턴스화 | deletedAt == null |
| T3-E03 | @EnableJpaAuditing 미설정 시 | JpaAuditingConfig 없음 | 엔티티 save() | createdAt/updatedAt 자동 설정 안 됨 (설정 필수 확인) |

## 기대 결과 (AC)
- [ ] `BaseEntity`가 `@MappedSuperclass`로 구현됨
- [ ] `created_at` 필드에 `@CreatedDate` 적용, 최초 저장 시 자동 설정됨
- [ ] `updated_at` 필드에 `@LastModifiedDate` 적용, 수정 시 자동 갱신됨
- [ ] `deleted_at` 필드가 nullable로 선언, 초기값 null
- [ ] `version` 필드에 `@Version` 적용, JPA가 낙관적 락을 자동 관리함
- [ ] `JpaAuditingConfig`에서 `@EnableJpaAuditing` 활성화됨
- [ ] `softDelete()` 메서드로 명시적 소프트 삭제 가능
- [ ] Setter가 `protected set`으로 캡슐화됨
