"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { apiFetch } from "@/lib/api";
import { Document, DocumentListResponse } from "@/types/document";

function TreeNode({ doc, depth = 0 }: { doc: Document; depth?: number }) {
  const pathname = usePathname();
  const [expanded, setExpanded] = useState(false);
  const hasChildren = doc.children && doc.children.length > 0;
  const isActive = pathname === `/documents/${doc.id}`;

  return (
    <div>
      <div
        className={`group flex items-center gap-1 rounded-md transition ${
          isActive
            ? "bg-accent-light text-accent font-semibold"
            : "text-secondary hover:bg-surface hover:text-primary"
        }`}
        style={{ paddingLeft: `${depth * 16 + 8}px` }}
      >
        {/* Expand/Collapse toggle */}
        <button
          type="button"
          onClick={() => setExpanded(!expanded)}
          className={`flex h-6 w-6 flex-shrink-0 items-center justify-center rounded text-muted hover:text-primary ${
            hasChildren ? "visible" : "invisible"
          }`}
        >
          <svg
            className={`h-3.5 w-3.5 transition-transform ${expanded ? "rotate-90" : ""}`}
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
        </button>

        <Link
          href={`/documents/${doc.id}`}
          className="flex flex-1 items-center gap-2 py-1.5 pr-2 text-sm"
        >
          {/* Document icon */}
          <svg className="h-4 w-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
            />
          </svg>
          <span className="truncate">{doc.title}</span>
          {doc.status === "DRAFT" && (
            <span className="flex-shrink-0 rounded bg-warning/10 px-1.5 py-0.5 text-[10px] font-medium text-warning">
              DRAFT
            </span>
          )}
        </Link>
      </div>

      {/* Children */}
      {expanded && hasChildren && (
        <div>
          {doc.children!.map((child) => (
            <TreeNode key={child.id} doc={child} depth={depth + 1} />
          ))}
        </div>
      )}
    </div>
  );
}

export default function DocumentTree() {
  const { data: documents, isLoading } = useQuery<Document[]>({
    queryKey: ["documents"],
    queryFn: () => apiFetch<DocumentListResponse>("/api/v1/documents").then(r => r.documents ?? [])
  });

  if (isLoading) {
    return (
      <div className="space-y-2 px-2">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="h-7 animate-pulse rounded bg-surface" />
        ))}
      </div>
    );
  }

  if (!documents || documents.length === 0) {
    return (
      <p className="px-4 py-2 text-xs text-muted">
        문서가 없습니다
      </p>
    );
  }

  // Build tree: only show root-level documents (parentId is null)
  const rootDocs = documents.filter((d) => !d.parentId);

  return (
    <div className="space-y-0.5">
      {rootDocs.map((doc) => (
        <TreeNode key={doc.id} doc={doc} />
      ))}
    </div>
  );
}
