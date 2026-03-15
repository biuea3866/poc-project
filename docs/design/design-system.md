# AI Wiki Design System

> 이 문서는 AI Wiki 프론트엔드의 디자인 시스템을 정의합니다.
> Notion/Linear 스타일의 모던하고 깔끔한 UI를 지향합니다.

---

## 1. Color Palette

### Primary Brand

| Token | Hex | 용도 |
|---|---|---|
| `accent` | `#6366f1` | 주요 CTA, 활성 상태, 링크 |
| `accent-purple` | `#8b5cf6` | 그래디언트 보조색 |
| `accent-light` | `#eef2ff` | 액센트 배경, 뱃지 배경 |

### Neutral (Light Mode)

| Token | Hex | 용도 |
|---|---|---|
| `surface` | `#f8f9fa` | 페이지 배경 |
| `card` | `#ffffff` | 카드, 모달, 사이드바 배경 |
| `border-line` | `#e9ecef` | 기본 보더 |
| `border-hover` | `#dee2e6` | 호버 보더 |
| `primary` | `#212529` | 제목, 강조 텍스트 |
| `secondary` | `#495057` | 본문 텍스트 |
| `muted` | `#868e96` | 보조 텍스트, 라벨 |
| `disabled` | `#adb5bd` | 비활성, 플레이스홀더 |

### Neutral (Dark Mode)

| Token | Hex | 용도 |
|---|---|---|
| `surface` | `#0f172a` | 페이지 배경 |
| `card` | `#1e293b` | 카드 배경 |
| `border-line` | `#334155` | 보더 |
| `primary` | `#f1f5f9` | 제목 텍스트 |
| `secondary` | `#94a3b8` | 본문 텍스트 |

### Status Colors

| Token | Hex | 용도 |
|---|---|---|
| `success` | `#12b886` | 완료, 성공 상태 |
| `warning` | `#f59e0b` | 처리 중, 주의 |
| `danger` | `#ef4444` | 실패, 삭제, 에러 |
| `danger-light` | `#fef2f2` | 에러 메시지 배경 |

### Gradient

```css
/* 브랜드 그래디언트 — 로고, CTA 버튼, 로그인 좌측 패널 */
background: linear-gradient(to right, #6366f1, #8b5cf6);
```

---

## 2. Typography

### Font Stack

| 용도 | 폰트 |
|---|---|
| 본문 (sans) | Pretendard Variable, Noto Sans KR, -apple-system, sans-serif |
| 코드 (mono) | JetBrains Mono, Fira Code, monospace |

### Type Scale

| Level | Size | Weight | Line Height | 용도 |
|---|---|---|---|---|
| Display | 36px (`text-4xl`) | 800 (extrabold) | 1.1 | 로그인 브랜딩 |
| H1 | 30px (`text-3xl`) | 800 (extrabold) | 1.2 | 페이지 제목 |
| H2 | 20px (`text-xl`) | 700 (bold) | 1.3 | 섹션 제목 |
| H3 | 18px (`text-lg`) | 700 (bold) | 1.4 | 카드 제목 |
| Body | 16px (`text-base`) | 400 (normal) | 1.6 | 본문 |
| Body-sm | 14px (`text-sm`) | 400–500 | 1.5 | 보조 텍스트, 메타 정보 |
| Caption | 12px (`text-xs`) | 600–700 (bold) | 1.4 | 라벨, 뱃지, 상태 텍스트 |

### 한글 특수 사항
- `font-feature-settings: "ss01", "ss02"` 적용
- `antialiased` 렌더링
- 트래킹(letter-spacing): 제목에 `tracking-tight` 적용

---

## 3. Spacing System

4px 기준 그리드 (`1 unit = 4px`)

| Token | px | 용도 |
|---|---|---|
| `0.5` | 2px | 미세 간격 (dot indicator) |
| `1` | 4px | 인라인 요소 간격 |
| `1.5` | 6px | 아이콘-텍스트 gap |
| `2` | 8px | 태그 내부 패딩 |
| `2.5` | 10px | 네비게이션 아이템 py |
| `3` | 12px | 사이드바 패딩, 섹션 내부 |
| `4` | 16px | 카드 내부 패딩 (작은) |
| `5` | 20px | 카드 내부 패딩 (기본) |
| `6` | 24px | 페이지 좌우 패딩, 헤더 px |
| `8` | 32px | 섹션 간 간격 |
| `10` | 40px | 페이지 상단 py |
| `12` | 48px | 대형 섹션 간격 |

### Layout Constraints

| 요소 | 값 |
|---|---|
| 사이드바 열림 | `w-[260px]` |
| 사이드바 닫힘 | `w-16` (64px) |
| 헤더 높이 | `h-16` (64px) |
| 콘텐츠 최대폭 | `max-w-5xl` (1024px) |
| 에디터 최대폭 | `max-w-3xl` (768px) |

---

## 4. Border Radius

| Token | 값 | 용도 |
|---|---|---|
| `rounded` | 4px | 작은 뱃지 |
| `rounded-lg` | 8px | 버튼, 네비 아이템 |
| `rounded-xl` | 12px | 카드, 입력 필드, 모달 |
| `rounded-2xl` | 16px | 대형 카드 |
| `rounded-full` | 9999px | 태그 뱃지, 아바타 |

---

## 5. Shadows

| Token | 값 | 용도 |
|---|---|---|
| `shadow-soft` | `0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.06)` | 기본 |
| `shadow-card` | `0 4px 6px -1px rgba(0,0,0,0.07), 0 2px 4px -2px rgba(0,0,0,0.05)` | 카드 호버 |
| `shadow-glow` | `0 0 0 3px rgba(99,102,241,0.15)` | 포커스 링 |

---

## 6. Component Inventory

### 6.1 Button

#### Variants

| Variant | Class | 용도 |
|---|---|---|
| **Accent (Primary)** | `.btn-accent` | 주요 CTA — "새 문서", "발행", "저장" |
| **Ghost** | `.btn-ghost` | 보조 액션 — "로그아웃", "취소" |
| **Danger** | `text-danger hover:text-danger/80` | 파괴적 액션 — "삭제" |
| **Icon** | `p-2 rounded-lg hover:bg-surface` | 아이콘 전용 — 툴바 버튼 |

#### Props (구현 시)

| Prop | Type | Default | 설명 |
|---|---|---|---|
| `variant` | `"accent" \| "ghost" \| "danger" \| "icon"` | `"accent"` | 스타일 변형 |
| `size` | `"sm" \| "md" \| "lg"` | `"md"` | 크기 |
| `disabled` | `boolean` | `false` | 비활성 상태 |
| `loading` | `boolean` | `false` | 로딩 상태 (텍스트 → "처리 중...") |
| `leftIcon` | `ReactNode` | — | 왼쪽 아이콘 |

#### 사용 예시

```tsx
<Button variant="accent" leftIcon={<Plus className="h-4 w-4" />}>
  새 문서
</Button>

<Button variant="ghost" leftIcon={<LogOut className="h-4 w-4" />}>
  로그아웃
</Button>

<Button variant="danger" leftIcon={<Trash2 className="h-4 w-4" />}>
  삭제
</Button>
```

---

### 6.2 Badge

AI 상태, 문서 상태, 태그 표시에 사용

#### Variants

| Variant | 배경 | 텍스트 | 용도 |
|---|---|---|---|
| `tag` | `bg-accent-light` | `text-accent` | 태그 표시 (`# Kotlin`) |
| `success` | `bg-success/10` | `text-success` | COMPLETED, SUCCESS |
| `warning` | `bg-warning/10` | `text-warning` | PENDING, PROCESSING |
| `danger` | `bg-danger-light` | `text-danger` | FAILED, DELETED |
| `info` | `bg-accent-light` | `text-accent` | AI 추천, 기능 뱃지 |
| `neutral` | `bg-surface` | `text-muted` | DRAFT 상태 |

#### Props

| Prop | Type | Default | 설명 |
|---|---|---|---|
| `variant` | `"tag" \| "success" \| "warning" \| "danger" \| "info" \| "neutral"` | `"tag"` | 스타일 |
| `size` | `"sm" \| "md"` | `"sm"` | 크기 |
| `dot` | `boolean` | `false` | 좌측 상태 점 표시 |

#### 스타일 규칙

```css
/* 공통 */
rounded-full px-2.5 py-0.5 text-xs font-semibold

/* dot 옵션 시 */
flex items-center gap-1.5
/* dot: h-2 w-2 rounded-full bg-{status-color} */
```

#### 사용 예시

```tsx
<Badge variant="tag"># Kotlin</Badge>
<Badge variant="success" dot>COMPLETED</Badge>
<Badge variant="warning" dot>PROCESSING</Badge>
<Badge variant="danger">FAILED</Badge>
<Badge variant="info">AI 추천</Badge>
```

---

### 6.3 Skeleton

데이터 로딩 중 플레이스홀더

#### Variants

| Variant | 스타일 | 용도 |
|---|---|---|
| `text` | `h-4 rounded bg-border-line animate-pulse` | 텍스트 줄 |
| `title` | `h-8 w-2/3 rounded-lg bg-border-line animate-pulse` | 제목 |
| `card` | `h-40 rounded-xl border border-border-line bg-card animate-pulse` | 카드 전체 |
| `avatar` | `h-8 w-8 rounded-full bg-border-line animate-pulse` | 프로필 |
| `image` | `aspect-video rounded-xl bg-border-line animate-pulse` | 이미지 |

#### Props

| Prop | Type | Default | 설명 |
|---|---|---|---|
| `variant` | `"text" \| "title" \| "card" \| "avatar" \| "image"` | `"text"` | 형태 |
| `width` | `string` | `"100%"` | 너비 |
| `count` | `number` | `1` | 반복 횟수 |

#### 사용 예시

```tsx
{/* 문서 상세 로딩 */}
<Skeleton variant="title" />
<div className="flex gap-4">
  <Skeleton variant="text" width="w-24" />
  <Skeleton variant="text" width="w-24" />
</div>

{/* 카드 그리드 로딩 */}
<div className="grid grid-cols-3 gap-5">
  <Skeleton variant="card" count={6} />
</div>
```

---

### 6.4 Toast

사용자 액션 결과 피드백 알림

#### Variants

| Variant | 좌측 아이콘 | 테두리 | 용도 |
|---|---|---|---|
| `success` | `CheckCircle` (success) | `border-success/30` | 저장/삭제 성공 |
| `error` | `AlertCircle` (danger) | `border-danger/30` | API 에러 |
| `info` | `Info` (accent) | `border-accent/30` | 일반 안내 |
| `warning` | `AlertTriangle` (warning) | `border-warning/30` | 주의 메시지 |

#### 동작 규칙

- 화면 우측 하단에 표시 (`fixed bottom-6 right-6`)
- 자동 소멸: 4초 (에러는 6초)
- 최대 3개 스택
- 진입: `animate-in` (아래→위 slide)
- 퇴장: opacity fade out

#### Props

| Prop | Type | Default | 설명 |
|---|---|---|---|
| `variant` | `"success" \| "error" \| "info" \| "warning"` | `"info"` | 스타일 |
| `title` | `string` | — | 제목 (옵션) |
| `message` | `string` | — | 메시지 내용 |
| `duration` | `number` | `4000` | 자동 닫힘 (ms) |

#### 레이아웃

```
┌──────────────────────────────────┐
│ [icon]  Title (bold)        [x] │
│         Message text            │
└──────────────────────────────────┘
```

```tsx
toast.success("문서가 저장되었습니다.");
toast.error("저장에 실패했습니다. 다시 시도해주세요.");
```

---

### 6.5 Modal (Dialog)

확인/취소 다이얼로그, 버전 미리보기 등

#### 스타일

```
┌─────────────────────────────────────┐
│                                     │
│  ┌───────────────────────────────┐  │  ← Overlay: bg-black/50
│  │  Title              [X btn]  │  │
│  │─────────────────────────────│  │
│  │                             │  │
│  │  Content area               │  │  ← Card: bg-card rounded-2xl
│  │                             │  │     shadow-card
│  │─────────────────────────────│  │     max-w-lg (default)
│  │        [Cancel]  [Confirm]  │  │
│  └───────────────────────────────┘  │
│                                     │
└─────────────────────────────────────┘
```

#### Props

| Prop | Type | Default | 설명 |
|---|---|---|---|
| `open` | `boolean` | — | 열림 상태 |
| `onClose` | `() => void` | — | 닫기 콜백 |
| `title` | `string` | — | 제목 |
| `size` | `"sm" \| "md" \| "lg" \| "xl"` | `"md"` | 너비 |
| `children` | `ReactNode` | — | 본문 콘텐츠 |
| `footer` | `ReactNode` | — | 하단 버튼 영역 |

#### Size Map

| Size | Tailwind | px |
|---|---|---|
| `sm` | `max-w-sm` | 384 |
| `md` | `max-w-lg` | 512 |
| `lg` | `max-w-2xl` | 672 |
| `xl` | `max-w-4xl` | 896 |

#### 동작 규칙

- ESC 키로 닫기
- 오버레이 클릭으로 닫기
- `animate-in` 애니메이션 적용
- 스크롤 잠금 (`overflow-hidden` on body)

#### 사용 예시

```tsx
<Modal open={isOpen} onClose={close} title="문서를 삭제하시겠습니까?"
  footer={
    <>
      <Button variant="ghost" onClick={close}>취소</Button>
      <Button variant="danger" onClick={handleDelete}>삭제</Button>
    </>
  }
>
  <p>삭제된 문서는 휴지통으로 이동합니다.</p>
</Modal>
```

---

## 7. Animation

| 이름 | 키프레임 | 용도 |
|---|---|---|
| `animate-in` | `opacity 0→1, translateY 8px→0, 0.5s ease` | 페이지/섹션 진입 |
| `animate-pulse` | Tailwind 기본 | 스켈레톤 로딩 |
| `transition` | `150ms ease` | 호버/포커스 전환 |

---

## 8. Iconography

- **라이브러리:** Lucide React (`lucide-react`)
- **기본 크기:** `h-4 w-4` (16px)
- **큰 아이콘:** `h-5 w-5` (20px) — 섹션 제목 옆
- **빈 상태 아이콘:** `h-12 w-12 opacity-10~20` — Empty state 중앙
- **색상:** 부모 텍스트 색상 상속 (`currentColor`)

### 주요 아이콘 매핑

| 기능 | 아이콘 |
|---|---|
| 문서 | `FileText` |
| 검색 | `Search` |
| 히스토리/버전 | `History` |
| 휴지통 | `Trash2` |
| AI 관련 | `Sparkles` |
| 수정 | `Edit3` |
| 삭제 | `Trash2` |
| 새 문서 | `Plus` |
| 뒤로가기 | `ChevronLeft` |
| 사이드바 토글 | `ChevronLeft` / `ChevronRight` |
| 캘린더/날짜 | `Calendar` |
| 사용자 | `User` |
| 알림/에러 | `AlertCircle` |
| 로그아웃 | `LogOut` |
| 시계 | `Clock` |
