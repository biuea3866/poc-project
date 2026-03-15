"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import DocumentTree from "@/components/DocumentTree";
import NotificationBell from "@/components/NotificationBell";

type NavItem = {
  label: string;
  href: string;
  icon: string;
  tag?: string;
};

const navItems: NavItem[] = [
  { label: "Documents", href: "/dashboard", icon: "M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" },
  { label: "Search", href: "/search", icon: "M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z", tag: "AI" },
  { label: "Revisions", href: "/revisions", icon: "M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" },
  { label: "Trash", href: "/trash", icon: "M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" }
];

export default function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();

  return (
    <div className="flex min-h-screen w-full flex-col">
      {/* Header */}
      <header className="sticky top-0 z-50 h-16 border-b border-line bg-white shadow-sm">
        <div className="mx-auto flex h-full w-full max-w-7xl items-center justify-between px-6">
          <Link href="/dashboard" className="text-xl font-extrabold">
            <span className="bg-gradient-to-r from-accent to-accent-purple bg-clip-text text-transparent">
              AI Wiki
            </span>
          </Link>

          <div className="mx-8 flex flex-1 max-w-xl items-center">
            <div className="relative w-full">
              <svg
                className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
              <input
                type="text"
                placeholder="문서 검색..."
                className="w-full rounded-lg border border-line bg-surface py-2 pl-10 pr-4 text-sm text-primary placeholder:text-muted focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent"
              />
            </div>
          </div>

          <div className="flex items-center gap-3">
            <NotificationBell />
            <Link
              href="/documents/new"
              className="rounded-lg bg-gradient-to-r from-accent to-accent-purple px-4 py-2 text-sm font-semibold text-white transition hover:opacity-90"
            >
              새 문서
            </Link>
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-accent-light text-sm font-semibold text-accent">
              U
            </div>
          </div>
        </div>
      </header>

      <div className="mx-auto flex w-full max-w-7xl flex-1">
        {/* Sidebar */}
        <aside className="hidden w-60 flex-col border-r border-line bg-white lg:flex">
          <nav className="flex flex-col gap-1 p-4">
            <p className="mb-2 px-3 text-xs font-semibold uppercase tracking-wider text-muted">
              Menu
            </p>
            {navItems.map((item) => {
              const isActive = pathname === item.href || (item.href === "/dashboard" && pathname === "/dashboard");
              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition ${
                    isActive
                      ? "bg-accent-light font-semibold text-accent"
                      : "text-secondary hover:bg-surface hover:text-primary"
                  }`}
                >
                  <svg className="h-5 w-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d={item.icon} />
                  </svg>
                  <span>{item.label}</span>
                  {item.tag ? (
                    <span className="ml-auto rounded-full bg-gradient-to-r from-accent to-accent-purple px-2 py-0.5 text-[10px] font-bold text-white">
                      {item.tag}
                    </span>
                  ) : null}
                </Link>
              );
            })}
          </nav>

          {/* Document Tree */}
          <div className="flex-1 overflow-y-auto border-t border-line px-2 py-3">
            <p className="mb-2 px-3 text-xs font-semibold uppercase tracking-wider text-muted">
              문서 트리
            </p>
            <DocumentTree />
          </div>

          {/* AI Status */}
          <div className="border-t border-line p-4">
            <div className="rounded-xl bg-surface p-4 text-xs">
              <p className="mb-3 font-semibold uppercase tracking-wider text-muted">AI Status</p>
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-secondary">Summaries</span>
                  <span className="font-medium text-success">Idle</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-secondary">Tagger</span>
                  <span className="font-medium text-warning">Queue</span>
                </div>
              </div>
            </div>
          </div>
        </aside>

        {/* Main Content */}
        <main className="flex-1 bg-surface p-6">
          {children}
        </main>
      </div>
    </div>
  );
}
