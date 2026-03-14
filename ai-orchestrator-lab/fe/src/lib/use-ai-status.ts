"use client";

import { useEffect, useState } from "react";
import type { AiStatus, SseStatusEvent } from "@/lib/types";

export function useAiStatus(documentId: number) {
  const [status, setStatus] = useState<AiStatus>("NOT_STARTED");

  useEffect(() => {
    const baseUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
    const eventSource = new EventSource(`${baseUrl}/api/v1/documents/${documentId}/ai-status/stream`);

    const handler = (event: MessageEvent) => {
      const payload = JSON.parse(event.data) as SseStatusEvent;
      setStatus(payload.status);
    };
    eventSource.addEventListener("ai-status", handler);

    eventSource.onerror = () => {
      eventSource.close();
    };

    return () => {
      eventSource.close();
    };
  }, [documentId]);

  return status;
}
