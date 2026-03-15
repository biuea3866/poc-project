# Editor & Document Tree UX Spec

> 사이드바 계층형 문서 트리 + Markdown 에디터 레이아웃 상세 스펙

---

## 1. 사이드바 문서 트리

### 1.1 개요

현재 사이드바는 고정 네비게이션 링크(Documents, Search, Revisions, Trash)만 존재한다.
이 스펙은 **Documents 섹션 아래에 계층형 문서 트리를 추가**하여 폴더 구조로 문서를 탐색할 수 있게 한다.

### 1.2 트리 레이아웃

```
┌──────────────────────────────┐
│  NAVIGATION                  │  ← 기존 섹션 라벨
│  ▸ Documents                 │
│  ○ Search                    │
│  ○ Revisions                 │
│  ○ Trash                     │
│                              │
│  DOCUMENTS                   │  ← 새 섹션 라벨
│  ┌────────────────────────┐  │
│  │ [+] 새 문서            │  │  ← 문서 생성 버튼
│  └────────────────────────┘  │
│                              │
│  ▾ 프로젝트 개요      ⋯     │  ← 루트 문서 (펼침)
│    ○ 기술 스택 결정   ⋯     │  ← 하위 문서 (level 1)
│    ▸ 아키텍처 설계    ⋯     │  ← 하위 + 자녀 있음 (접힘)
│      ○ 마이크로서비스 ⋯     │  ← level 2 (부모 펼침 시)
│  ○ 회의록             ⋯     │  ← 루트 문서 (자녀 없음)
│  ▸ 기술 블로그         ⋯     │  ← 루트 문서 (접힘)
│                              │
└──────────────────────────────┘
```

### 1.3 트리 아이템 스타일

#### 기본 아이템

```
┌──────────────────────────────────────────────┐
│ [▸/▾] [📄] 문서 제목              [⋯]       │
│                                   (hover)    │
└──────────────────────────────────────────────┘
```

| 요소 | 스타일 | 설명 |
|---|---|---|
| 펼침/접힘 화살표 | `ChevronRight` (접힘) / `ChevronDown` (펼침), `h-3.5 w-3.5 text-muted` | 자녀 있을 때만 표시 |
| 문서 아이콘 | `FileText h-4 w-4 text-muted` | 항상 표시 |
| 문서 제목 | `text-sm text-secondary truncate` | 한 줄, 말줄임 |
| 더보기 | `MoreHorizontal h-4 w-4` | 호버 시에만 표시 |

#### 상태별 스타일

| 상태 | 스타일 |
|---|---|
| 기본 | `text-secondary hover:bg-surface hover:text-primary` |
| 활성 (현재 문서) | `bg-accent-light font-semibold text-accent` |
| DRAFT 문서 | 제목 옆 `Badge variant="neutral" size="sm"` → "DRAFT" |
| AI 처리 중 | 아이콘 옆 `ai-dot-processing` (노란 점 깜빡) |

#### 들여쓰기

| Level | padding-left |
|---|---|
| 0 (루트) | `pl-3` (12px) |
| 1 | `pl-7` (28px) |
| 2 | `pl-11` (44px) |
| 3+ | `pl-{4n+3}` (4 × level + 12px) |

각 레벨당 `16px` 들여쓰기 추가.

### 1.4 더보기 메뉴 (Context Menu)

트리 아이템의 `⋯` 버튼 클릭 시 드롭다운 메뉴 표시

```
┌──────────────────────┐
│ [Edit3]   이름 변경  │
│ [Plus]    하위 문서   │
│ ─────────────────── │
│ [Trash2]  삭제       │  ← text-danger
└──────────────────────┘
```

#### 드롭다운 스타일

```css
/* 컨테이너 */
absolute right-0 top-full mt-1 z-50
rounded-xl border border-border-line bg-card shadow-card
py-1 min-w-[160px]

/* 메뉴 아이템 */
flex items-center gap-2 px-3 py-2 text-sm text-secondary
hover:bg-surface hover:text-primary transition cursor-pointer

/* 위험 아이템 */
text-danger hover:bg-danger-light
```

#### 이름 변경 인터랙션

1. "이름 변경" 클릭
2. 문서 제목이 인라인 `input`으로 전환
3. 입력 스타일: `text-sm border border-accent rounded px-1.5 py-0.5 bg-card focus:ring-2 focus:ring-accent/20`
4. Enter → `PUT /api/v1/documents/{id}` 호출 (title 변경)
5. ESC → 취소, 원래 제목 복원

#### 삭제 인터랙션

1. "삭제" 클릭
2. 확인 모달 표시:
   ```
   "프로젝트 개요" 문서를 삭제하시겠습니까?
   하위 문서 2건도 함께 삭제됩니다.
   ```
3. 확인 → `DELETE /api/v1/documents/{id}` → toast 표시 → 트리 갱신

### 1.5 새 문서 버튼

- 위치: DOCUMENTS 섹션 라벨 바로 아래
- 스타일: `w-full flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-muted border border-dashed border-border-line hover:border-accent hover:text-accent transition`
- 아이콘: `Plus h-4 w-4`
- 클릭 시: `POST /api/v1/documents` (빈 DRAFT 문서 생성) → 에디터 페이지로 이동

### 1.6 드래그 앤 드롭 (Future)

> 향후 구현 예정. 현재 스펙에서는 시각적 가이드만 정의.

- 드래그 시작: 선택된 아이템에 `opacity-50 shadow-lg` 적용
- 드롭 대상: `border-t-2 border-accent` (상단 삽입) 또는 `bg-accent-light` (자녀로 이동)
- 드롭 후: `PUT /api/v1/documents/{id}` (parentId, order 변경)

---

## 2. Markdown 에디터

### 2.1 에디터 페이지 레이아웃

경로: `/documents/{id}/edit` (기존 문서 수정) 또는 `/documents/new` (새 문서)

```
┌──────────────────────────────────────────────────────────────────┐
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  [← 뒤로]    제목 입력 영역         [DRAFT] [저장] [발행] │  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │ [B] [I] [H1] [H2] [H3] [Code] [Link] [Image] [─] [Quote]│  │
│  └────────────────────────────────────────────────────────────┘  │
│                                                                  │
│  ┌───────────────────────┬───────────────────────┐              │
│  │                       │                       │              │
│  │   Editor (좌)         │   Preview (우)        │              │
│  │                       │                       │              │
│  │   # 제목              │   제목                │              │
│  │                       │                       │              │
│  │   본문 내용을          │   본문 내용을          │              │
│  │   마크다운으로         │   렌더링된 HTML로      │              │
│  │   작성합니다.          │   미리봅니다.          │              │
│  │                       │                       │              │
│  │                       │                       │              │
│  └───────────────────────┴───────────────────────┘              │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  자동 저장됨 · 2초 전                                      │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

### 2.2 제목 입력 영역

```css
/* 제목 인풋 */
text-3xl font-extrabold text-primary
border-none outline-none bg-transparent
placeholder:text-disabled
w-full
```

- 플레이스홀더: `"제목을 입력하세요..."`
- 최대 길이: 255자
- Enter 키: 본문 에디터로 포커스 이동

### 2.3 상단 액션 바

| 요소 | 스타일 | 조건 | 동작 |
|---|---|---|---|
| 뒤로가기 | `ChevronLeft + "뒤로"`, `text-muted hover:text-primary` | 항상 | 문서 상세로 이동 |
| DRAFT 뱃지 | `Badge variant="neutral"` → "DRAFT" | `status === "DRAFT"` | — |
| ACTIVE 뱃지 | `Badge variant="success"` → "ACTIVE" | `status === "ACTIVE"` | — |
| 저장 버튼 | `btn-ghost border border-border-line` | 항상 | `PUT /api/v1/documents/{id}` |
| 발행 버튼 | `btn-accent` → "발행" | `status === "DRAFT"` | `POST /api/v1/documents/{id}/publish` |

#### 발행 플로우

1. "발행" 클릭
2. 확인 모달:
   ```
   문서를 발행하시겠습니까?

   발행하면 AI가 자동으로 요약과 태그를
   생성합니다. ACTIVE 상태로 전환됩니다.

        [취소]     [발행하기]
   ```
3. 확인 → `POST /api/v1/documents/{id}/publish`
4. 성공 → DRAFT 뱃지 → ACTIVE 뱃지 전환 + `toast.success("문서가 발행되었습니다. AI 분석이 시작됩니다.")`
5. SSE 연결 시작 → AI 상태 실시간 추적

### 2.4 툴바

#### 레이아웃

```
┌────────────────────────────────────────────────────────────────┐
│ [B] [I] [S] │ [H1] [H2] [H3] │ [</>] [``] │ [🔗] [📷] │ [─] [❝] │
└────────────────────────────────────────────────────────────────┘
```

#### 버튼 정의

| 그룹 | 버튼 | 아이콘 | Markdown 삽입 | 단축키 |
|---|---|---|---|---|
| 텍스트 | Bold | `Bold` | `**text**` | `Cmd+B` |
| | Italic | `Italic` | `*text*` | `Cmd+I` |
| | Strikethrough | `Strikethrough` | `~~text~~` | `Cmd+Shift+S` |
| 제목 | H1 | `Heading1` | `# ` | — |
| | H2 | `Heading2` | `## ` | — |
| | H3 | `Heading3` | `### ` | — |
| 코드 | Code Block | `Code` | `` ```\n\n``` `` | `Cmd+Shift+K` |
| | Inline Code | `Terminal` | `` `code` `` | `Cmd+E` |
| 삽입 | Link | `Link` | `[text](url)` | `Cmd+K` |
| | Image | `Image` | `![alt](url)` | — |
| 블록 | Divider | `Minus` | `---\n` | — |
| | Quote | `Quote` | `> ` | — |

#### 툴바 스타일

```css
/* 컨테이너 */
flex items-center gap-0.5 rounded-xl border border-border-line bg-card px-2 py-1.5

/* 버튼 기본 */
p-2 rounded-lg text-muted hover:bg-surface hover:text-primary transition

/* 버튼 활성 (텍스트 선택 상태에서 해당 포맷 적용 중) */
bg-accent-light text-accent

/* 구분선 */
w-px h-5 bg-border-line mx-1
```

### 2.5 에디터 영역 (Split View)

#### 좌측: 편집기

```css
/* 에디터 컨테이너 */
flex-1 min-h-[500px] p-6
font-mono text-sm leading-relaxed text-primary
border-r border-border-line

/* 편집 영역 */
outline-none resize-none w-full h-full bg-transparent
```

- 라인 넘버: 좌측에 `text-xs text-disabled pr-4` (옵션)
- 탭 크기: 2 spaces
- 현재 줄 하이라이트: `bg-accent-light/30`

#### 우측: 미리보기

```css
/* 미리보기 컨테이너 */
flex-1 p-6 overflow-y-auto

/* Markdown 렌더링 */
prose prose-sm max-w-none
/* 기존 MarkdownRenderer 컴포넌트 스타일 적용 */
```

#### Split View 토글

- 기본: 50:50 분할
- 모바일 (`md` 미만): 에디터만 표시, 미리보기는 탭 전환
- 토글 버튼: 툴바 우측 끝에 `[에디터] [미리보기] [분할]` 세그먼트

```css
/* 세그먼트 컨트롤 */
flex items-center rounded-lg border border-border-line bg-surface p-0.5

/* 세그먼트 버튼 */
px-3 py-1 text-xs font-medium text-muted rounded-md

/* 활성 세그먼트 */
bg-card text-primary shadow-soft
```

### 2.6 자동 저장 인디케이터

#### 위치

에디터 하단, 전체 너비 바

#### 상태별 표시

| 상태 | 아이콘 | 텍스트 | 스타일 |
|---|---|---|---|
| 저장 완료 | `Check h-3.5 w-3.5 text-success` | "자동 저장됨 · 2초 전" | `text-xs text-muted` |
| 저장 중 | `Loader2 h-3.5 w-3.5 text-warning animate-spin` | "저장 중..." | `text-xs text-warning` |
| 저장 실패 | `AlertCircle h-3.5 w-3.5 text-danger` | "저장 실패 · 재시도" | `text-xs text-danger` |
| 변경 사항 없음 | — | — | 숨김 |

#### 자동 저장 동작

1. 사용자 입력 후 **3초 무입력** 시 자동 저장 트리거
2. `PUT /api/v1/documents/{id}` 호출 (title + content)
3. 저장 성공 → "자동 저장됨" 표시
4. 저장 실패 → "저장 실패" + "재시도" 링크
5. 수동 저장: `Cmd+S` → 즉시 저장

---

## 3. 문서 상세 페이지 AI 상태 UI

### 3.1 AI 상태별 분기

현재 문서 상세 페이지에서 `aiStatus`에 따라 요약/태그 영역의 UI를 분기한다.

#### PENDING / PROCESSING

```
┌──────────────────────────────────────────────────┐
│  [Sparkles]  AI 분석 중...                        │
│                                                  │
│  ┌──────────────────────────────────────────┐    │
│  │ ████████████████  ← skeleton pulse      │    │  ← 요약 스켈레톤
│  │ ██████████████████████                  │    │
│  └──────────────────────────────────────────┘    │
│                                                  │
│  ┌────┐ ┌──────┐ ┌────────┐  ← tag skeletons   │
│  │████│ │██████│ │████████│                     │
│  └────┘ └──────┘ └────────┘                     │
│                                                  │
│  [ai-dot-processing] PROCESSING                  │
└──────────────────────────────────────────────────┘
```

- 컨테이너: `rounded-xl border border-accent/20 bg-accent-light/30 p-6`
- 제목: `text-sm font-bold text-accent uppercase tracking-wider`
- 스켈레톤: 요약 2줄 + 태그 3개
- SSE 연결: `GET /api/v1/documents/{id}/ai-status/stream`으로 실시간 추적

#### COMPLETED

```
┌──────────────────────────────────────────────────┐
│  [Sparkles]  AI 요약                              │
│                                                  │
│  "이 문서는 Kotlin Coroutines의 기본 개념과       │
│   structured concurrency 패턴을 설명합니다."      │
│                                                  │
│  [# Kotlin]  [# Coroutines]  [# 비동기]          │
└──────────────────────────────────────────────────┘
```

- 기존 구현과 동일 (현재 코드 유지)
- 요약: `text-lg leading-relaxed text-secondary italic`
- 태그: `Badge variant="tag"` → `rounded-full bg-accent-light px-2.5 py-0.5 text-xs font-semibold text-accent`

#### FAILED

```
┌──────────────────────────────────────────────────┐
│  [AlertCircle]  AI 분석 실패                      │
│                                                  │
│  AI 요약/태그 생성에 실패했습니다.                  │
│  네트워크 문제이거나 일시적인 오류일 수 있습니다.    │
│                                                  │
│             [재분석 요청]                          │
└──────────────────────────────────────────────────┘
```

- 컨테이너: `rounded-xl border border-danger/20 bg-danger-light p-6`
- 아이콘: `AlertCircle h-5 w-5 text-danger`
- 제목: `text-sm font-bold text-danger uppercase tracking-wider`
- 메시지: `text-sm text-secondary`
- 재분석 버튼: `btn-ghost border border-border-line text-sm`
- 클릭 시: `POST /api/v1/documents/{id}/analyze` → PENDING으로 전환 → SSE 재연결

---

## 4. 반응형

| Breakpoint | 에디터 | 트리 |
|---|---|---|
| `lg+` (1024px+) | Split view (50:50) | 사이드바에 트리 표시 |
| `md` (768px) | 에디터/미리보기 탭 전환 | 사이드바 숨김, 햄버거 메뉴 |
| `sm` 미만 | 에디터만, 미리보기 별도 버튼 | 풀스크린 에디터, 트리 드로어 |

---

## 5. 키보드 단축키 요약

| 단축키 | 동작 |
|---|---|
| `Cmd+S` | 수동 저장 |
| `Cmd+B` | Bold |
| `Cmd+I` | Italic |
| `Cmd+Shift+S` | Strikethrough |
| `Cmd+K` | 링크 삽입 |
| `Cmd+E` | 인라인 코드 |
| `Cmd+Shift+K` | 코드 블록 |
| `Cmd+Enter` | 발행 (DRAFT → ACTIVE) |
| `Escape` | 에디터 → 문서 상세로 돌아가기 |
