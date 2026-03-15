import { clearTokens, refreshAccessToken } from "@/lib/auth";

// 서버 사이드(SSR)에서는 직접 BE 주소, 클라이언트 사이드에서는 Next.js 프록시 통해서 호출
const API_BASE = typeof window === "undefined"
  ? (process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080")
  : "";

export type ApiError = {
  message: string;
  status: number;
};

/**
 * API 요청 래퍼.
 * - 401 응답 시 Refresh 토큰으로 자동 재발급을 시도하고 원래 요청을 1회 재시도한다.
 * - 재발급도 실패하면 토큰을 클리어하고 로그인 페이지로 이동한다.
 * - isRetry 플래그로 무한 루프를 방지한다.
 */
export async function apiFetch<T>(
  path: string,
  init: RequestInit = {},
  isRetry = false
): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("Content-Type", "application/json");

  if (typeof window !== "undefined") {
    const token = window.localStorage.getItem("accessToken");
    if (token) {
      headers.set("Authorization", `Bearer ${token}`);
    }
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers
  });

  // 401 처리: Refresh 토큰으로 재발급 시도 (1회만)
  if (response.status === 401 && !isRetry) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      // 재발급 성공 → 원래 요청 재시도
      return apiFetch<T>(path, init, true);
    }
    // 재발급 실패 → 토큰 클리어 후 로그인 페이지로 이동
    clearTokens();
    if (typeof window !== "undefined") {
      window.location.href = "/login";
    }
    throw new Error("세션이 만료되었습니다. 다시 로그인해주세요.");
  }

  if (!response.ok) {
    const body = await safeJson(response);
    const message =
      body?.message || body?.error || `Request failed (${response.status})`;
    const error: ApiError = { message, status: response.status };
    throw new Error(error.message);
  }

  return safeJson(response) as Promise<T>;
}

async function safeJson(response: Response): Promise<any> {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return { message: text };
  }
}
