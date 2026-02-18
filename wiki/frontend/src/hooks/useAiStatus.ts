"use client";

import { useEffect, useState } from "react";
import { AiStatus } from "@/types/document";
import { useQueryClient } from "@tanstack/react-query";

export function useAiStatus(documentId: number | string | undefined) {
  const [status, setStatus] = useState<AiStatus>("PENDING");
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!documentId) return;

    // SSE 연결
    const eventSource = new EventSource(
      `${process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080"}/api/v1/documents/${documentId}/ai-status/stream`
    );

    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        const newStatus = data.status as AiStatus;
        setStatus(newStatus);

        // 상태가 완료되면 관련 쿼리(요약, 태그 등) 무효화하여 다시 불러오기
        if (newStatus === "COMPLETED") {
          queryClient.invalidateQueries({ queryKey: ["document", String(documentId)] });
          queryClient.invalidateQueries({ queryKey: ["document-summary", String(documentId)] });
          queryClient.invalidateQueries({ queryKey: ["document-tags", String(documentId)] });
          // 트리 목록도 갱신
          queryClient.invalidateQueries({ queryKey: ["documents"] });
        }
      } catch (error) {
        console.error("SSE parse error:", error);
      }
    };

    eventSource.onerror = (error) => {
      console.error("SSE connection error:", error);
      eventSource.close();
    };

    return () => {
      eventSource.close();
    };
  }, [documentId, queryClient]);

  return status;
}
