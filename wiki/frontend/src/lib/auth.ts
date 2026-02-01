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
