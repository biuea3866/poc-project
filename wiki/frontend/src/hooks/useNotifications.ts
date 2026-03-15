"use client";

import { useEffect } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { getUnreadCount } from "@/lib/notifications";

const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export function useNotifications() {
  const queryClient = useQueryClient();

  useEffect(() => {
    if (typeof window === "undefined") return;

    const token = window.localStorage.getItem("accessToken");
    if (!token) return;

    // EventSource does not support custom headers.
    // SSE stream is authenticated via cookie or the Authorization header via a
    // proxy workaround. For now, we fall back to 30-second polling if the SSE
    // connection fails immediately (e.g., 401). The SSE endpoint is still kept
    // so that once bearer-in-cookie or a token-as-query-param approach is
    // implemented the real-time push path works automatically.
    let es: EventSource | null = null;
    let closed = false;

    try {
      es = new EventSource(`${API_BASE}/api/v1/notifications/stream`);

      es.addEventListener("notification", () => {
        queryClient.invalidateQueries({ queryKey: ["notifications", "unread-count"] });
        queryClient.invalidateQueries({ queryKey: ["notifications", "list"] });
      });

      es.onerror = () => {
        // Close gracefully on auth error; polling fallback handles refresh
        if (es && !closed) {
          es.close();
        }
      };
    } catch {
      // SSE not available (SSR, etc.)
    }

    return () => {
      closed = true;
      es?.close();
    };
  }, [queryClient]);

  return useQuery({
    queryKey: ["notifications", "unread-count"],
    queryFn: getUnreadCount,
    refetchInterval: 30_000, // 30-second polling fallback
    staleTime: 10_000,
  });
}
