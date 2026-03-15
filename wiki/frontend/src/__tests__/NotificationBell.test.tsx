import React from "react";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import NotificationBell from "@/components/NotificationBell";

// Mock next/navigation
jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: jest.fn() }),
}));

// Mock notification hooks and API
const mockGetUnreadCount = jest.fn();
const mockGetNotifications = jest.fn();
const mockMarkAsRead = jest.fn();
const mockMarkAllAsRead = jest.fn();

jest.mock("@/lib/notifications", () => ({
  getNotifications: (...args: unknown[]) => mockGetNotifications(...args),
  getUnreadCount: () => mockGetUnreadCount(),
  markAsRead: (id: number) => mockMarkAsRead(id),
  markAllAsRead: () => mockMarkAllAsRead(),
}));

// Mock useNotifications hook
jest.mock("@/hooks/useNotifications", () => ({
  useNotifications: () => ({
    data: { count: mockUnreadCount },
    isLoading: false,
  }),
}));

let mockUnreadCount = 0;

function makeNotification(overrides = {}) {
  return {
    id: 1,
    type: "AI_COMPLETED" as const,
    targetUserId: 10,
    refId: 100,
    message: "문서 AI 처리가 완료되었습니다.",
    isRead: false,
    createdAt: new Date().toISOString(),
    readAt: null,
    ...overrides,
  };
}

function renderWithQuery(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>
  );
}

describe("NotificationBell", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockUnreadCount = 0;
    mockGetUnreadCount.mockResolvedValue({ count: 0 });
    mockGetNotifications.mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });
  });

  // TC-1: 미읽음 뱃지 렌더링
  it("미읽음 알림이 없으면 뱃지가 표시되지 않는다", () => {
    mockUnreadCount = 0;
    renderWithQuery(<NotificationBell />);
    expect(screen.queryByLabelText(/미읽음 알림/)).not.toBeInTheDocument();
  });

  it("미읽음 알림이 있으면 뱃지에 카운트가 표시된다", () => {
    mockUnreadCount = 3;
    renderWithQuery(<NotificationBell />);
    expect(screen.getByLabelText("미읽음 알림 3개")).toBeInTheDocument();
    expect(screen.getByText("3")).toBeInTheDocument();
  });

  it("미읽음 알림이 99개를 초과하면 99+로 표시된다", () => {
    mockUnreadCount = 100;
    renderWithQuery(<NotificationBell />);
    expect(screen.getByText("99+")).toBeInTheDocument();
  });

  // TC-2: 드롭다운 열기/닫기
  it("벨 아이콘 클릭 시 드롭다운이 열린다", async () => {
    const user = userEvent.setup();
    renderWithQuery(<NotificationBell />);

    await user.click(screen.getByLabelText("알림"));

    await waitFor(() => {
      expect(screen.getByText("알림")).toBeInTheDocument();
    });
  });

  it("알림 목록이 비어 있으면 '알림이 없습니다' 메시지를 표시한다", async () => {
    const user = userEvent.setup();
    mockGetNotifications.mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });

    renderWithQuery(<NotificationBell />);

    await user.click(screen.getByLabelText("알림"));

    await waitFor(() => {
      expect(screen.getByText("알림이 없습니다")).toBeInTheDocument();
    });
  });

  it("알림 목록이 있으면 메시지를 표시한다", async () => {
    const user = userEvent.setup();
    const notification = makeNotification();
    mockGetNotifications.mockResolvedValue({
      content: [notification],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });

    renderWithQuery(<NotificationBell />);

    await user.click(screen.getByLabelText("알림"));

    await waitFor(() => {
      expect(screen.getByText("문서 AI 처리가 완료되었습니다.")).toBeInTheDocument();
    });
  });

  it("드롭다운 외부 클릭 시 닫힌다", async () => {
    const user = userEvent.setup();
    renderWithQuery(
      <div>
        <NotificationBell />
        <button data-testid="outside">outside</button>
      </div>
    );

    await user.click(screen.getByLabelText("알림"));

    // Dropdown should be open
    await waitFor(() => {
      expect(screen.getByText("알림")).toBeInTheDocument();
    });

    await user.click(screen.getByTestId("outside"));

    await waitFor(() => {
      expect(screen.queryByText("모두 읽음")).not.toBeInTheDocument();
    });
  });

  // TC-3: 읽음 처리
  it("미읽음 알림이 있을 때 '모두 읽음' 버튼이 표시된다", async () => {
    const user = userEvent.setup();
    mockUnreadCount = 2;
    mockGetNotifications.mockResolvedValue({
      content: [makeNotification({ isRead: false })],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    mockMarkAllAsRead.mockResolvedValue(undefined);

    renderWithQuery(<NotificationBell />);
    await user.click(screen.getByLabelText("알림"));

    await waitFor(() => {
      expect(screen.getByText("모두 읽음")).toBeInTheDocument();
    });
  });

  it("'모두 읽음' 클릭 시 markAllAsRead API가 호출된다", async () => {
    const user = userEvent.setup();
    mockUnreadCount = 1;
    mockGetNotifications.mockResolvedValue({
      content: [makeNotification({ isRead: false })],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    mockMarkAllAsRead.mockResolvedValue(undefined);

    renderWithQuery(<NotificationBell />);
    await user.click(screen.getByLabelText("알림"));

    await waitFor(() => {
      expect(screen.getByText("모두 읽음")).toBeInTheDocument();
    });

    await user.click(screen.getByText("모두 읽음"));

    await waitFor(() => {
      expect(mockMarkAllAsRead).toHaveBeenCalledTimes(1);
    });
  });

  it("읽은 알림은 opacity가 낮게 표시된다", async () => {
    const user = userEvent.setup();
    const readNotification = makeNotification({ isRead: true });
    mockGetNotifications.mockResolvedValue({
      content: [readNotification],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });

    renderWithQuery(<NotificationBell />);
    await user.click(screen.getByLabelText("알림"));

    await waitFor(() => {
      const item = screen.getByText("문서 AI 처리가 완료되었습니다.").closest("button");
      expect(item).toHaveClass("opacity-60");
    });
  });
});
