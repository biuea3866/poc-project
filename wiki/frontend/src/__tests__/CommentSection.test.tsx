import React from "react";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import CommentSection from "@/components/CommentSection";
import type { Comment } from "@/lib/comments";

// Mock comments lib
const mockGetComments = jest.fn();
const mockCreateComment = jest.fn();
const mockUpdateComment = jest.fn();
const mockDeleteComment = jest.fn();

jest.mock("@/lib/comments", () => ({
  getComments: (...args: unknown[]) => mockGetComments(...args),
  createComment: (...args: unknown[]) => mockCreateComment(...args),
  updateComment: (...args: unknown[]) => mockUpdateComment(...args),
  deleteComment: (...args: unknown[]) => mockDeleteComment(...args),
}));

function makeComment(overrides: Partial<Comment> = {}): Comment {
  return {
    id: 1,
    authorId: 42,
    content: "테스트 댓글",
    parentId: null,
    isDeleted: false,
    createdAt: new Date().toISOString(),
    replies: [],
    ...overrides,
  };
}

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>
  );
}

describe("CommentSection 컴포넌트", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Mock localStorage
    Object.defineProperty(window, "localStorage", {
      value: { getItem: jest.fn(() => null), setItem: jest.fn() },
      writable: true,
    });
  });

  // TC-1: 댓글 목록 렌더링
  it("댓글 목록을 렌더링한다", async () => {
    const comments: Comment[] = [
      makeComment({ id: 1, content: "첫 번째 댓글", authorId: 1 }),
      makeComment({ id: 2, content: "두 번째 댓글", authorId: 2 }),
    ];
    mockGetComments.mockResolvedValue(comments);

    renderWithClient(<CommentSection documentId={10} />);

    await waitFor(() => {
      expect(screen.getByText("첫 번째 댓글")).toBeInTheDocument();
      expect(screen.getByText("두 번째 댓글")).toBeInTheDocument();
    });
  });

  // TC-2: 삭제된 댓글 플레이스홀더
  it("삭제된 댓글은 플레이스홀더 텍스트를 표시한다", async () => {
    const comments: Comment[] = [
      makeComment({ id: 1, content: "삭제된 댓글입니다.", isDeleted: true }),
    ];
    mockGetComments.mockResolvedValue(comments);

    renderWithClient(<CommentSection documentId={10} />);

    await waitFor(() => {
      expect(screen.getByText("삭제된 댓글입니다.")).toBeInTheDocument();
    });
  });

  // TC-3: 빈 목록 메시지
  it("댓글이 없으면 안내 메시지를 표시한다", async () => {
    mockGetComments.mockResolvedValue([]);

    renderWithClient(<CommentSection documentId={10} />);

    await waitFor(() => {
      expect(screen.getByText("첫 번째 댓글을 작성해보세요.")).toBeInTheDocument();
    });
  });

  // TC-4: 댓글 작성 폼 동작
  it("댓글 작성 폼에서 내용을 입력하고 등록할 수 있다", async () => {
    const user = userEvent.setup();
    mockGetComments.mockResolvedValue([]);
    mockCreateComment.mockResolvedValue(makeComment({ content: "새 댓글" }));

    renderWithClient(<CommentSection documentId={10} />);

    await waitFor(() => {
      expect(screen.getByPlaceholderText("댓글을 입력하세요...")).toBeInTheDocument();
    });

    const textarea = screen.getByPlaceholderText("댓글을 입력하세요...");
    await user.type(textarea, "새 댓글");

    const submitBtn = screen.getByRole("button", { name: "댓글 등록" });
    expect(submitBtn).not.toBeDisabled();
    await user.click(submitBtn);

    await waitFor(() => {
      expect(mockCreateComment).toHaveBeenCalledWith(10, "새 댓글");
    });
  });

  // TC-5: 내용 없으면 등록 버튼 비활성화
  it("댓글 내용이 없으면 등록 버튼이 비활성화된다", async () => {
    mockGetComments.mockResolvedValue([]);

    renderWithClient(<CommentSection documentId={10} />);

    await waitFor(() => {
      const submitBtn = screen.getByRole("button", { name: "댓글 등록" });
      expect(submitBtn).toBeDisabled();
    });
  });

  // TC-6: 대댓글 표시
  it("대댓글이 인덴트되어 표시된다", async () => {
    const reply = makeComment({ id: 2, content: "대댓글 내용", parentId: 1 });
    const root = makeComment({ id: 1, content: "루트 댓글", replies: [reply] });
    mockGetComments.mockResolvedValue([root]);

    renderWithClient(<CommentSection documentId={10} />);

    await waitFor(() => {
      expect(screen.getByText("루트 댓글")).toBeInTheDocument();
      expect(screen.getByText("대댓글 내용")).toBeInTheDocument();
    });
  });

  // TC-7: 본인 댓글에만 수정/삭제 버튼 노출
  it("본인 댓글(userId=42)에만 수정/삭제 버튼이 보인다", async () => {
    // Mock localStorage to return userId 42
    Object.defineProperty(window, "localStorage", {
      value: { getItem: jest.fn(() => "42"), setItem: jest.fn() },
      writable: true,
    });

    const myComment = makeComment({ id: 1, content: "내 댓글", authorId: 42 });
    const otherComment = makeComment({ id: 2, content: "남의 댓글", authorId: 99 });
    mockGetComments.mockResolvedValue([myComment, otherComment]);

    renderWithClient(<CommentSection documentId={10} />);

    await waitFor(() => {
      expect(screen.getByText("내 댓글")).toBeInTheDocument();
    });

    // Should find exactly one pair of edit/delete buttons (for my comment only)
    const editButtons = screen.getAllByRole("button", { name: "수정" });
    const deleteButtons = screen.getAllByRole("button", { name: "삭제" });
    expect(editButtons).toHaveLength(1);
    expect(deleteButtons).toHaveLength(1);
  });
});
