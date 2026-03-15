import { apiFetch } from "@/lib/api";

type AuthTokens = {
  accessToken?: string;
  refreshToken?: string;
  data?: {
    accessToken?: string;
    refreshToken?: string;
  };
};

type LoginPayload = {
  email: string;
  password: string;
};

type SignupPayload = {
  name: string;
  email: string;
  password: string;
};

export async function login(payload: LoginPayload): Promise<void> {
  const response = await apiFetch<AuthTokens>("/api/v1/auth/login", {
    method: "POST",
    body: JSON.stringify(payload)
  });
  persistTokens(response);
}

export async function signup(payload: SignupPayload): Promise<void> {
  const response = await apiFetch<AuthTokens>("/api/v1/auth/signup", {
    method: "POST",
    body: JSON.stringify(payload)
  });
  persistTokens(response);
}

/**
 * Refresh 토큰 회전(Rotation) — 새 access + refresh 토큰 쌍을 발급받아 localStorage에 저장한다.
 * 탈취 감지로 서버가 모든 세션을 무효화한 경우에도 토큰을 클리어한다.
 *
 * @returns 재발급 성공 여부
 */
export async function refreshAccessToken(): Promise<boolean> {
  if (typeof window === "undefined") return false;

  const refreshToken = window.localStorage.getItem("refreshToken");
  if (!refreshToken) return false;

  try {
    // apiFetch 내부 401 인터셉터를 우회하기 위해 직접 fetch 사용
    const API_BASE =
      process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
    const response = await fetch(`${API_BASE}/api/v1/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });

    if (!response.ok) {
      clearTokens();
      return false;
    }

    const data: AuthTokens = await response.json();
    persistTokens(data);
    return true;
  } catch {
    clearTokens();
    return false;
  }
}

export function persistTokens(response: AuthTokens) {
  if (typeof window === "undefined") return;
  const accessToken = response.accessToken ?? response.data?.accessToken;
  const refreshToken = response.refreshToken ?? response.data?.refreshToken;

  if (accessToken) {
    window.localStorage.setItem("accessToken", accessToken);
  }
  if (refreshToken) {
    window.localStorage.setItem("refreshToken", refreshToken);
  }
}

export function clearTokens() {
  if (typeof window === "undefined") return;
  window.localStorage.removeItem("accessToken");
  window.localStorage.removeItem("refreshToken");
}

export function getAccessToken() {
  if (typeof window === "undefined") return null;
  return window.localStorage.getItem("accessToken");
}
