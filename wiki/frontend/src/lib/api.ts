const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export type ApiError = {
  message: string;
  status: number;
};

export async function apiFetch<T>(
  path: string,
  init: RequestInit = {}
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

  if (!response.ok) {
    // 401/403 → 토큰 제거 후 로그인 페이지로 리다이렉트
    if (
      (response.status === 401 || response.status === 403) &&
      typeof window !== "undefined"
    ) {
      window.localStorage.removeItem("accessToken");
      window.localStorage.removeItem("refreshToken");
      window.location.href = "/login";
      // 리다이렉트 후 이어지는 코드 실행 방지
      return new Promise<T>(() => {});
    }

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
