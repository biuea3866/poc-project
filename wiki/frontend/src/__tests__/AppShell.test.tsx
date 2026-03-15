import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

jest.mock("next/navigation", () => ({
  usePathname: () => "/dashboard",
  useRouter: () => ({ push: jest.fn(), replace: jest.fn(), back: jest.fn() }),
  useParams: () => ({})
}));

import AppShell from "@/components/AppShell";

function renderWithProviders(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } }
  });
  return render(
    <QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>
  );
}

describe("AppShell", () => {
  it("renders the AI Wiki logo", () => {
    renderWithProviders(<AppShell><div>content</div></AppShell>);
    expect(screen.getByText("AI Wiki")).toBeInTheDocument();
  });

  it("renders navigation items", () => {
    renderWithProviders(<AppShell><div>content</div></AppShell>);
    expect(screen.getByText("Documents")).toBeInTheDocument();
    expect(screen.getByText("Search")).toBeInTheDocument();
    expect(screen.getByText("Revisions")).toBeInTheDocument();
    expect(screen.getByText("Trash")).toBeInTheDocument();
  });

  it("renders children content", () => {
    renderWithProviders(<AppShell><div>test child content</div></AppShell>);
    expect(screen.getByText("test child content")).toBeInTheDocument();
  });

  it("renders the new document button", () => {
    renderWithProviders(<AppShell><div>content</div></AppShell>);
    expect(screen.getByText("새 문서")).toBeInTheDocument();
  });
});
