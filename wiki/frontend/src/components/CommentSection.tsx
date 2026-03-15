"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Comment,
  getComments,
  createComment,
  updateComment,
  deleteComment,
} from "@/lib/comments";

function getStoredUserId(): number | null {
  if (typeof window === "undefined") return null;
  const raw = window.localStorage.getItem("userId");
  return raw ? Number(raw) : null;
}

interface CommentItemProps {
  comment: Comment;
  documentId: number;
  currentUserId: number | null;
  depth?: number;
}

function CommentItem({ comment, documentId, currentUserId, depth = 0 }: CommentItemProps) {
  const queryClient = useQueryClient();
  const [isReplying, setIsReplying] = useState(false);
  const [replyContent, setReplyContent] = useState("");
  const [isEditing, setIsEditing] = useState(false);
  const [editContent, setEditContent] = useState(comment.content);

  const createMutation = useMutation({
    mutationFn: (content: string) => createComment(documentId, content, comment.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["comments", documentId] });
      setReplyContent("");
      setIsReplying(false);
    },
  });

  const updateMutation = useMutation({
    mutationFn: (content: string) => updateComment(comment.id, content),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["comments", documentId] });
      setIsEditing(false);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteComment(comment.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["comments", documentId] });
    },
  });

  const isOwner = currentUserId !== null && currentUserId === comment.authorId;
  const indentClass = depth > 0 ? "ml-8" : "";

  return (
    <div className={`${indentClass} mt-3`}>
      <div
        className={`rounded-lg border p-4 ${
          comment.isDeleted
            ? "border-line bg-surface"
            : "border-line bg-white"
        }`}
      >
        {/* Header */}
        <div className="mb-2 flex items-center justify-between text-xs text-secondary">
          <span className="font-semibold">
            {comment.isDeleted ? "삭제됨" : `사용자 ${comment.authorId}`}
          </span>
          <span>{new Date(comment.createdAt).toLocaleString()}</span>
        </div>

        {/* Content */}
        {isEditing ? (
          <div className="space-y-2">
            <textarea
              className="w-full rounded border border-line p-2 text-sm focus:outline-none focus:ring-1 focus:ring-accent"
              rows={3}
              value={editContent}
              onChange={(e) => setEditContent(e.target.value)}
            />
            <div className="flex gap-2">
              <button
                onClick={() => updateMutation.mutate(editContent)}
                disabled={updateMutation.isPending || !editContent.trim()}
                className="rounded bg-accent px-3 py-1 text-xs font-medium text-white hover:bg-accent/90 disabled:opacity-50"
              >
                {updateMutation.isPending ? "저장 중..." : "저장"}
              </button>
              <button
                onClick={() => {
                  setIsEditing(false);
                  setEditContent(comment.content);
                }}
                className="rounded border border-line px-3 py-1 text-xs font-medium text-secondary hover:bg-surface"
              >
                취소
              </button>
            </div>
          </div>
        ) : (
          <p
            className={`text-sm leading-relaxed ${
              comment.isDeleted ? "italic text-muted" : "text-primary"
            }`}
          >
            {comment.content}
          </p>
        )}

        {/* Actions */}
        {!comment.isDeleted && !isEditing && (
          <div className="mt-2 flex items-center gap-3 text-xs">
            {depth === 0 && (
              <button
                onClick={() => setIsReplying((v) => !v)}
                className="text-secondary hover:text-accent"
              >
                답글
              </button>
            )}
            {isOwner && (
              <>
                <button
                  onClick={() => setIsEditing(true)}
                  className="text-secondary hover:text-accent"
                >
                  수정
                </button>
                <button
                  onClick={() => {
                    if (confirm("댓글을 삭제하시겠습니까?")) {
                      deleteMutation.mutate();
                    }
                  }}
                  disabled={deleteMutation.isPending}
                  className="text-danger hover:text-danger/80 disabled:opacity-50"
                >
                  삭제
                </button>
              </>
            )}
          </div>
        )}
      </div>

      {/* Reply form */}
      {isReplying && (
        <div className="ml-8 mt-2">
          <textarea
            className="w-full rounded border border-line p-2 text-sm focus:outline-none focus:ring-1 focus:ring-accent"
            rows={2}
            placeholder="답글을 입력하세요..."
            value={replyContent}
            onChange={(e) => setReplyContent(e.target.value)}
          />
          <div className="mt-1 flex gap-2">
            <button
              onClick={() => createMutation.mutate(replyContent)}
              disabled={createMutation.isPending || !replyContent.trim()}
              className="rounded bg-accent px-3 py-1 text-xs font-medium text-white hover:bg-accent/90 disabled:opacity-50"
            >
              {createMutation.isPending ? "등록 중..." : "답글 등록"}
            </button>
            <button
              onClick={() => {
                setIsReplying(false);
                setReplyContent("");
              }}
              className="rounded border border-line px-3 py-1 text-xs font-medium text-secondary hover:bg-surface"
            >
              취소
            </button>
          </div>
        </div>
      )}

      {/* Nested replies (depth === 1 max) */}
      {comment.replies.map((reply) => (
        <CommentItem
          key={reply.id}
          comment={reply}
          documentId={documentId}
          currentUserId={currentUserId}
          depth={depth + 1}
        />
      ))}
    </div>
  );
}

interface CommentSectionProps {
  documentId: number;
}

export default function CommentSection({ documentId }: CommentSectionProps) {
  const queryClient = useQueryClient();
  const [newContent, setNewContent] = useState("");
  const currentUserId = getStoredUserId();

  const { data: comments = [], isLoading } = useQuery<Comment[]>({
    queryKey: ["comments", documentId],
    queryFn: () => getComments(documentId),
  });

  const createMutation = useMutation({
    mutationFn: (content: string) => createComment(documentId, content),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["comments", documentId] });
      setNewContent("");
    },
  });

  return (
    <section className="space-y-6">
      <div className="flex items-center gap-2 border-t border-line pt-8">
        <svg
          className="h-5 w-5 text-secondary"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"
          />
        </svg>
        <h2 className="text-lg font-bold text-primary">
          댓글 {comments.length > 0 ? `(${comments.length})` : ""}
        </h2>
      </div>

      {/* New comment form */}
      <div className="rounded-lg border border-line bg-white p-4 shadow-sm">
        <textarea
          className="w-full rounded border border-line p-3 text-sm focus:outline-none focus:ring-1 focus:ring-accent"
          rows={3}
          placeholder="댓글을 입력하세요..."
          value={newContent}
          onChange={(e) => setNewContent(e.target.value)}
        />
        <div className="mt-2 flex justify-end">
          <button
            onClick={() => createMutation.mutate(newContent)}
            disabled={createMutation.isPending || !newContent.trim()}
            className="rounded bg-accent px-4 py-2 text-sm font-medium text-white hover:bg-accent/90 disabled:opacity-50"
          >
            {createMutation.isPending ? "등록 중..." : "댓글 등록"}
          </button>
        </div>
      </div>

      {/* Comment list */}
      {isLoading ? (
        <div className="space-y-3">
          {[1, 2].map((i) => (
            <div key={i} className="h-20 animate-pulse rounded-lg bg-white" />
          ))}
        </div>
      ) : comments.length === 0 ? (
        <p className="py-8 text-center text-sm text-muted">
          첫 번째 댓글을 작성해보세요.
        </p>
      ) : (
        <div className="space-y-1">
          {comments.map((comment) => (
            <CommentItem
              key={comment.id}
              comment={comment}
              documentId={documentId}
              currentUserId={currentUserId}
            />
          ))}
        </div>
      )}
    </section>
  );
}
