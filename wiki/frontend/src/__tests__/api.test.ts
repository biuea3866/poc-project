import "@testing-library/jest-dom";
import { apiFetch } from "@/lib/api";

describe("apiFetch", () => {
  const originalHref = window.location.href;

  beforeEach(() => {
    window.localStorage.clear();
    jest.restoreAllMocks();
    // Use jsdom's location.assign or just spy on href via location.assign
  });

  it("adds Authorization header when token exists", async () => {
    window.localStorage.setItem("accessToken", "test-token");

    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      text: () => Promise.resolve(JSON.stringify({ data: "ok" }))
    });

    await apiFetch("/api/v1/test");

    const fetchCall = (global.fetch as jest.Mock).mock.calls[0];
    const headers = fetchCall[1].headers;
    expect(headers.get("Authorization")).toBe("Bearer test-token");
  });

  it("clears tokens on 401 response", async () => {
    window.localStorage.setItem("accessToken", "expired-token");
    window.localStorage.setItem("refreshToken", "expired-refresh");

    global.fetch = jest.fn().mockResolvedValue({
      ok: false,
      status: 401,
      text: () => Promise.resolve("")
    });

    // apiFetch returns a never-resolving promise on 401, race with timeout
    await Promise.race([
      apiFetch("/api/v1/test").catch(() => {}),
      new Promise((resolve) => setTimeout(resolve, 50))
    ]);

    expect(window.localStorage.getItem("accessToken")).toBeNull();
    expect(window.localStorage.getItem("refreshToken")).toBeNull();
  });

  it("clears tokens on 403 response", async () => {
    window.localStorage.setItem("accessToken", "bad-token");
    window.localStorage.setItem("refreshToken", "bad-refresh");

    global.fetch = jest.fn().mockResolvedValue({
      ok: false,
      status: 403,
      text: () => Promise.resolve("")
    });

    await Promise.race([
      apiFetch("/api/v1/test").catch(() => {}),
      new Promise((resolve) => setTimeout(resolve, 50))
    ]);

    expect(window.localStorage.getItem("accessToken")).toBeNull();
    expect(window.localStorage.getItem("refreshToken")).toBeNull();
  });

  it("throws error on non-auth error responses", async () => {
    global.fetch = jest.fn().mockResolvedValue({
      ok: false,
      status: 500,
      text: () => Promise.resolve(JSON.stringify({ message: "Server error" }))
    });

    await expect(apiFetch("/api/v1/test")).rejects.toThrow("Server error");
  });
});
