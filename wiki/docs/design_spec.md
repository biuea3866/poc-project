# AI Wiki — UI/UX Design Spec (Tistory/Velog Style Redesign)

> **Date:** 2026-03-15
> **Designer:** Claude (AI Agent)
> **Direction:** Velog 라이트 테마 기반 + Tistory 카드 그리드 레이아웃 + 블루-퍼플 포인트 컬러

---

## 1. Color Palette

### Light Theme (Default)

| Token | Hex | Tailwind Key | Usage |
|-------|-----|-------------|-------|
| Background | `#f8f9fa` | `bg-surface` | 전체 배경 |
| Card / Panel | `#ffffff` | `bg-card` | 카드, 사이드바, 헤더 |
| Border | `#e9ecef` | `border-line` | 구분선, 카드 보더 |
| Border Hover | `#dee2e6` | `border-hover` | hover 시 보더 |
| Text Primary | `#212529` | `text-primary` | 제목, 강조 텍스트 |
| Text Secondary | `#495057` | `text-secondary` | 본문 텍스트 |
| Text Muted | `#868e96` | `text-muted` | 부가 정보, 날짜 |
| Text Disabled | `#adb5bd` | `text-disabled` | 비활성 텍스트 |
| Accent Start | `#6366f1` | `accent` | 포인트 컬러 (인디고) |
| Accent End | `#8b5cf6` | `accent-purple` | 그라데이션 끝 (보라) |
| Accent Light | `#eef2ff` | `accent-light` | 액센트 배경 |
| Success | `#12b886` | `success` | AI 완료, 성공 상태 |
| Warning | `#f59e0b` | `warning` | 대기 중, 처리 중 |
| Danger | `#ef4444` | `danger` | 삭제, 에러 |
| Danger Light | `#fef2f2` | `danger-light` | 에러 배경 |

### Dark Theme (Toggle)

| Token | Hex | Tailwind Key |
|-------|-----|-------------|
| Background | `#0f172a` | `dark:bg-surface` |
| Card / Panel | `#1e293b` | `dark:bg-card` |
| Border | `#334155` | `dark:border-line` |
| Text Primary | `#f1f5f9` | `dark:text-primary` |
| Text Secondary | `#94a3b8` | `dark:text-secondary` |
| Text Muted | `#64748b` | `dark:text-muted` |

---

## 2. Typography System

```
Font Family: 'Pretendard Variable', 'Noto Sans KR', -apple-system, sans-serif
Code Font: 'JetBrains Mono', 'Fira Code', monospace
```

| Element | Size | Weight | Line Height | Letter Spacing |
|---------|------|--------|-------------|----------------|
| H1 (Page Title) | `text-3xl` (30px) | `font-extrabold` (800) | 1.2 | `-0.02em` |
| H2 (Section) | `text-xl` (20px) | `font-bold` (700) | 1.3 | `-0.01em` |
| H3 (Subsection) | `text-lg` (18px) | `font-semibold` (600) | 1.4 | `0` |
| Body | `text-base` (16px) | `font-normal` (400) | 1.8 | `0` |
| Small / Meta | `text-sm` (14px) | `font-medium` (500) | 1.5 | `0` |
| Caption | `text-xs` (12px) | `font-medium` (500) | 1.5 | `0.02em` |
| Tag | `text-xs` (12px) | `font-semibold` (600) | 1 | `0` |

---

## 3. Component Design Specs

### 3.1 AppShell (Header + Sidebar)

#### Header
```
Height: 64px
Background: bg-card (white) / dark:bg-card
Border-bottom: 1px solid border-line
Shadow: shadow-sm
Position: sticky top-0 z-50
```

**Layout (flex, justify-between, items-center, px-6):**

| Left | Center | Right |
|------|--------|-------|
| 로고 (AI Wiki, accent 그라데이션 텍스트) | 검색바 (flex-1, max-w-xl, 중앙 배치) | 새 문서 버튼 + 프로필 아바타 |

- **Logo:** "AI Wiki" 텍스트, `bg-gradient-to-r from-accent to-accent-purple bg-clip-text text-transparent font-extrabold text-xl`
- **Search Bar:** `rounded-xl bg-surface border border-line px-4 py-2.5 text-sm`, 좌측 검색 아이콘, placeholder "문서 검색...", focus 시 `border-accent ring-2 ring-accent/20`
- **New Doc Button:** `rounded-lg bg-gradient-to-r from-accent to-accent-purple text-white px-4 py-2 text-sm font-semibold`, hover 시 `shadow-lg shadow-accent/25`
- **Profile Avatar:** 32x32 원형, 이니셜 표시, `bg-accent-light text-accent font-bold`

#### Sidebar (Left, Collapsible)

```
Width: 260px (expanded) / 64px (collapsed)
Background: bg-card
Border-right: 1px solid border-line
Transition: width 300ms ease
```

**Sections (top to bottom):**

1. **Navigation Links** — 아이콘 + 레이블 (Documents, Search, Revisions, Trash)
   - `rounded-lg px-3 py-2.5 text-sm text-secondary`
   - Active: `bg-accent-light text-accent font-semibold`
   - Hover: `bg-surface text-primary`

2. **Categories / Collections** — 접이식 트리 (Tistory 스타일)
   - 카테고리 헤더: `text-xs font-bold uppercase tracking-wider text-muted`
   - 하위 항목: `pl-6 text-sm text-secondary`

3. **AI Status Panel** (하단 고정)
   - `rounded-xl bg-surface border border-line p-4`
   - 상태 인디케이터: 초록색 dot (`success`) = Idle, 노란색 dot (`warning`) = Processing, 회색 = Off
   - 라벨: "Summaries", "Tagger" 등

4. **Collapse Toggle** — 사이드바 하단, 화살표 아이콘 버튼

---

### 3.2 AuthForm (Login / Signup)

**Layout:**
```
전체 페이지 2분할:
- 좌측 (50%): 일러스트/브랜딩 영역 — bg-gradient-to-br from-accent to-accent-purple, 중앙에 로고 + 슬로건
- 우측 (50%): 폼 영역 — bg-card, 중앙 정렬, max-w-sm
```

**Form Elements:**

- **Title:** `text-2xl font-extrabold text-primary`, 아래 subtitle `text-sm text-muted mt-2`
- **Input Fields:**
  - `rounded-xl border border-line bg-surface px-4 py-3 text-base text-primary`
  - Placeholder: `text-disabled`
  - Focus: `border-accent ring-2 ring-accent/20 outline-none`
  - Label: `text-sm font-medium text-secondary mb-1.5`
- **Submit Button:**
  - `w-full rounded-xl bg-gradient-to-r from-accent to-accent-purple text-white py-3 text-sm font-bold`
  - Hover: `opacity-90 shadow-lg shadow-accent/25 translate-y-[-1px]`
  - Loading: `opacity-60 cursor-not-allowed`
- **Error Message:**
  - `rounded-lg border border-danger/30 bg-danger-light px-3 py-2 text-sm text-danger`
- **Switch Link:** `text-sm text-muted` + `text-accent font-semibold hover:underline`

---

### 3.3 DocumentCard (Dashboard Grid Item)

**Card Container:**
```
rounded-xl bg-card border border-line p-5
hover: shadow-md border-hover translate-y-[-2px]
transition: all 200ms ease
cursor: pointer
```

**Inner Layout:**
```
┌──────────────────────────┐
│  Category Badge (option) │
│  Title (font-bold, 2줄)  │
│  Preview text (2줄 clamp)│
│                          │
│  ┌───┐ ┌───┐ ┌───┐      │
│  │Tag│ │Tag│ │Tag│      │ ← rounded-full bg-accent-light text-accent
│  └───┘ └───┘ └───┘      │
│                          │
│  Author · Date · AI 🟢  │ ← text-xs text-muted
└──────────────────────────┘
```

- **Title:** `text-lg font-bold text-primary line-clamp-2`
- **Preview:** `text-sm text-secondary line-clamp-2 mt-2`
- **Tags:** `text-xs font-semibold rounded-full px-2.5 py-0.5 bg-accent-light text-accent`
- **Meta Row:** `text-xs text-muted flex items-center gap-3 mt-4`
- **AI Status Badge:**
  - Completed: `w-2 h-2 rounded-full bg-success` (초록 dot)
  - Processing: `w-2 h-2 rounded-full bg-warning animate-pulse` (노란 dot 펄스)
  - Pending: `w-2 h-2 rounded-full bg-disabled` (회색 dot)

---

### 3.4 SearchBar (Global, in Header)

```
Container: relative flex items-center
Input: w-full rounded-xl bg-surface border border-line pl-10 pr-4 py-2.5 text-sm
Icon: absolute left-3, Search icon, text-muted
Focus: border-accent ring-2 ring-accent/20
```

- Keyboard shortcut hint: `Cmd+K` 표시 (`text-xs text-disabled border border-line rounded px-1.5 py-0.5`)
- 검색 결과 dropdown: `absolute top-full mt-2 w-full bg-card border border-line rounded-xl shadow-lg max-h-80 overflow-y-auto`

---

## 4. Page Layout Specs

### 4.1 Dashboard

```
┌─── Header (64px, full width) ──────────────────────────────┐
├─── Sidebar (260px) ─┬─── Main Content ─────────────────────┤
│                     │                                      │
│  Navigation         │  ┌── Greeting Section ─────────────┐ │
│  Categories         │  │  "Welcome back, 홍길동"         │ │
│  AI Status          │  │  오늘의 AI 활동 요약 카드       │ │
│                     │  └──────────────────────────────────┘ │
│                     │                                      │
│                     │  ┌── Document Grid ─────────────────┐ │
│                     │  │  ┌─Card─┐ ┌─Card─┐ ┌─Card─┐    │ │
│                     │  │  └──────┘ └──────┘ └──────┘    │ │
│                     │  │  ┌─Card─┐ ┌─Card─┐ ┌─Card─┐    │ │
│                     │  │  └──────┘ └──────┘ └──────┘    │ │
│                     │  └──────────────────────────────────┘ │
├─────────────────────┴──────────────────────────────────────┤
```

- **Greeting:** `text-3xl font-extrabold text-primary` + AI 활동 카운트 배지
- **Document Grid:** `grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5`
- **Sort/Filter Bar:** 그리드 위, `flex justify-between items-center`
  - 좌: "최근 문서" / "모든 문서" 탭
  - 우: 정렬 드롭다운 (최신순, 이름순)

### 4.2 Document Viewer

```
┌─── Header ─────────────────────────────────────────────────┐
├─── Sidebar ─┬─── Article Content (max-w-3xl, mx-auto) ────┤
│             │                                              │
│             │  ← 뒤로가기                                  │
│             │                                              │
│             │  # Document Title                            │
│             │  Author · Date · Tags                        │
│             │                                              │
│             │  ┌── AI Summary Card ──────────────────┐     │
│             │  │  ✨ AI 요약                         │     │
│             │  │  "요약 내용..."                      │     │
│             │  └─────────────────────────────────────┘     │
│             │                                              │
│             │  Markdown Content (prose)                     │
│             │  line-height: 1.8                             │
│             │  좌우 여백 넉넉                              │
│             │                                              │
│             │  ──── Footer ────                            │
│             │  최근 수정 · 변경 이력 보기                   │
│             │                                              │
├─────────────┴──────────────────────────────────────────────┤
```

- **AI Summary Card:** `rounded-xl border border-accent/20 bg-accent-light/50 p-6`
  - 아이콘: `Sparkles` in `text-accent`
  - 내용: `text-lg leading-relaxed text-secondary italic`
- **Article Prose:** Tailwind Typography (`@tailwindcss/typography`) 플러그인 사용
  - `prose prose-slate max-w-none`
  - `prose-headings:text-primary prose-p:text-secondary`
  - `prose-a:text-accent hover:prose-a:underline`
  - `prose-code:text-accent prose-code:bg-accent-light/50 prose-code:rounded prose-code:px-1.5`

### 4.3 Auth Pages

```
┌────────────────────────────────────────────────────────────┐
│                                                            │
│  ┌── Branding (50%) ──────┬── Form (50%) ────────────────┐ │
│  │                        │                              │ │
│  │  bg-gradient           │     ┌── Form Card ────────┐  │ │
│  │  from-accent           │     │  Title              │  │ │
│  │  to-accent-purple      │     │  Subtitle           │  │ │
│  │                        │     │                     │  │ │
│  │    ✦ AI Wiki           │     │  [Name]             │  │ │
│  │    "지식을 기록하면     │     │  [Email]            │  │ │
│  │     AI가 정리합니다"   │     │  [Password]         │  │ │
│  │                        │     │                     │  │ │
│  │                        │     │  [Submit Button]    │  │ │
│  │                        │     │  Switch Link        │  │ │
│  │                        │     └─────────────────────┘  │ │
│  └────────────────────────┴──────────────────────────────┘ │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

- 모바일: 브랜딩 영역 숨김, 폼만 전체 너비
- 반응형 breakpoint: `lg:grid-cols-2`

### 4.4 Trash Page

- 리스트 뷰 유지 (현재와 유사)
- 카드 스타일: `bg-card rounded-lg border border-line p-5`
- hover 시 액션 버튼 표시 (복구/삭제)
- 빈 상태: 중앙 아이콘 + 메시지

### 4.5 Search Page

- 상단: 검색 입력 (큰 사이즈, `text-lg py-4 rounded-xl`)
- 필터: 태그, 날짜 범위, AI 상태
- 결과: DocumentCard 리스트 (1열) + 검색어 하이라이트

---

## 5. tailwind.config.ts Changes

```typescript
import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./src/**/*.{ts,tsx}"],
  darkMode: "class",
  theme: {
    extend: {
      fontFamily: {
        sans: [
          "Pretendard Variable",
          "Noto Sans KR",
          "-apple-system",
          "BlinkMacSystemFont",
          "sans-serif",
        ],
        mono: ["JetBrains Mono", "Fira Code", "monospace"],
      },
      colors: {
        surface: "#f8f9fa",
        card: "#ffffff",
        "border-line": "#e9ecef",
        "border-hover": "#dee2e6",
        primary: "#212529",
        secondary: "#495057",
        muted: "#868e96",
        disabled: "#adb5bd",
        accent: "#6366f1",
        "accent-purple": "#8b5cf6",
        "accent-light": "#eef2ff",
        success: "#12b886",
        warning: "#f59e0b",
        danger: "#ef4444",
        "danger-light": "#fef2f2",
      },
      boxShadow: {
        soft: "0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.06)",
        card: "0 4px 6px -1px rgba(0,0,0,0.07), 0 2px 4px -2px rgba(0,0,0,0.05)",
        glow: "0 0 0 3px rgba(99,102,241,0.15)",
      },
      borderRadius: {
        xl: "0.75rem",
        "2xl": "1rem",
      },
    },
  },
  plugins: [require("@tailwindcss/typography")],
};

export default config;
```

---

## 6. globals.css Changes

```css
@tailwind base;
@tailwind components;
@tailwind utilities;

@layer base {
  :root {
    color-scheme: light;
  }

  .dark {
    color-scheme: dark;
  }

  body {
    @apply bg-surface text-secondary antialiased;
    font-feature-settings: "ss01", "ss02";
  }

  /* Scrollbar styling */
  ::-webkit-scrollbar {
    width: 6px;
  }
  ::-webkit-scrollbar-track {
    @apply bg-transparent;
  }
  ::-webkit-scrollbar-thumb {
    @apply rounded-full bg-disabled/40;
  }
  ::-webkit-scrollbar-thumb:hover {
    @apply bg-muted/50;
  }
}

@layer components {
  /* Gradient accent text */
  .text-accent-gradient {
    @apply bg-gradient-to-r from-accent to-accent-purple bg-clip-text text-transparent;
  }

  /* Gradient accent button */
  .btn-accent {
    @apply rounded-lg bg-gradient-to-r from-accent to-accent-purple px-4 py-2 text-sm font-semibold text-white transition;
    @apply hover:opacity-90 hover:shadow-lg hover:shadow-accent/25 hover:-translate-y-px;
    @apply disabled:opacity-60 disabled:cursor-not-allowed disabled:hover:translate-y-0 disabled:hover:shadow-none;
  }

  /* Ghost button */
  .btn-ghost {
    @apply rounded-lg px-3 py-2 text-sm font-medium text-secondary transition;
    @apply hover:bg-surface hover:text-primary;
  }

  /* Input field */
  .input-field {
    @apply w-full rounded-xl border border-border-line bg-surface px-4 py-3 text-base text-primary;
    @apply placeholder:text-disabled;
    @apply focus:border-accent focus:ring-2 focus:ring-accent/20 focus:outline-none;
    @apply transition;
  }

  /* Card */
  .card {
    @apply rounded-xl border border-border-line bg-card p-5;
    @apply transition-all duration-200;
    @apply hover:shadow-card hover:border-border-hover hover:-translate-y-0.5;
  }

  /* AI status dot */
  .ai-dot-idle {
    @apply h-2 w-2 rounded-full bg-success;
  }
  .ai-dot-processing {
    @apply h-2 w-2 rounded-full bg-warning animate-pulse;
  }
  .ai-dot-off {
    @apply h-2 w-2 rounded-full bg-disabled;
  }
}

@layer utilities {
  .animate-in {
    animation: animateIn 0.5s ease forwards;
  }

  @keyframes animateIn {
    from {
      opacity: 0;
      transform: translateY(8px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
}

/* Dark mode overrides */
.dark body {
  @apply bg-[#0f172a] text-[#94a3b8];
}

.dark .card,
.dark .bg-card {
  @apply bg-[#1e293b] border-[#334155];
}

.dark .bg-surface {
  @apply bg-[#0f172a];
}

.dark .input-field {
  @apply bg-[#1e293b] border-[#334155] text-[#f1f5f9] placeholder:text-[#475569];
}
```

---

## 7. Design Tokens Summary (Quick Reference)

| Category | Before (Dark) | After (Light) |
|----------|--------------|---------------|
| Background | `#0b0e13` (ink) | `#f8f9fa` (surface) |
| Card | `#111720` (coal) | `#ffffff` (card) |
| Accent | `#f97316` (ember/orange) | `#6366f1 → #8b5cf6` (indigo-purple gradient) |
| Text | `#f4efe6` (linen) | `#212529` (primary) |
| Border | `rgba(255,255,255,0.1)` | `#e9ecef` (border-line) |
| Success | `#34d399` (mint) | `#12b886` (success) |
| Body line-height | ~1.5 | 1.8 |
| Font | system default | Pretendard + Noto Sans KR |
| Grid overlay | Yes | No |

---

## 8. Implementation Priority

1. `tailwind.config.ts` — 색상, 폰트, 플러그인 업데이트
2. `globals.css` — 라이트 테마 기본, 컴포넌트 유틸리티 클래스
3. `AppShell.tsx` — 헤더 + 사이드바 전면 리디자인
4. `AuthForm.tsx` — 2분할 레이아웃, 인풋/버튼 스타일
5. Dashboard `page.tsx` — DocumentCard 그리드
6. Document `[id]/page.tsx` — Article 뷰어 스타일 적용
7. Trash / Search / Revisions — 나머지 페이지 적용
8. 다크모드 토글 컴포넌트 추가

---

## 9. Dependencies to Add

```bash
npm install @tailwindcss/typography
```

Font (Next.js `layout.tsx`에서 로드):
```typescript
import { Noto_Sans_KR } from "next/font/google";
// 또는 Pretendard CDN 추가
```
