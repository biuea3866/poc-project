import Link from "next/link";

type NavItem = {
  label: string;
  href: string;
  tag?: string;
};

const navItems: NavItem[] = [
  { label: "Documents", href: "/dashboard" },
  { label: "Search", href: "/search", tag: "AI" },
  { label: "Revisions", href: "/revisions" },
  { label: "Trash", href: "/trash" }
];

export default function AppShell({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen w-full flex-col">
      <header className="sticky top-0 z-10 border-b border-white/10 bg-ink/80 px-6 py-4 backdrop-blur">
        <div className="mx-auto flex w-full max-w-6xl items-center justify-between">
          <div>
            <p className="text-xs uppercase tracking-[0.35em] text-sand/60">
              AI Wiki Workspace
            </p>
            <h1 className="text-xl font-semibold text-linen">
              Your knowledge, organized.
            </h1>
          </div>
          <div className="flex items-center gap-3">
            <button className="rounded-full border border-white/20 px-4 py-2 text-xs uppercase tracking-[0.3em] text-sand/80">
              New doc
            </button>
            <button className="rounded-full bg-ember px-4 py-2 text-xs font-semibold text-ink">
              Invite
            </button>
          </div>
        </div>
      </header>

      <div className="mx-auto flex w-full max-w-6xl flex-1 gap-6 px-6 py-6">
        <aside className="hidden w-64 flex-col gap-6 rounded-2xl border border-white/10 bg-coal/70 p-5 lg:flex">
          <div className="space-y-2">
            <p className="text-xs uppercase tracking-[0.3em] text-sand/60">
              Collections
            </p>
            <nav className="space-y-1">
              {navItems.map((item) => (
                <Link
                  key={item.href}
                  href={item.href}
                  className="flex items-center justify-between rounded-xl px-3 py-2 text-sm text-sand/80 transition hover:bg-white/5 hover:text-linen"
                >
                  <span>{item.label}</span>
                  {item.tag ? (
                    <span className="rounded-full border border-ember/60 px-2 py-0.5 text-[10px] uppercase tracking-[0.2em] text-ember">
                      {item.tag}
                    </span>
                  ) : null}
                </Link>
              ))}
            </nav>
          </div>
          <div className="space-y-3 rounded-2xl border border-white/10 bg-steel/60 p-4 text-xs text-sand/80">
            <p className="uppercase tracking-[0.3em]">AI status</p>
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <span>Summaries</span>
                <span className="text-mint">Idle</span>
              </div>
              <div className="flex items-center justify-between">
                <span>Tagger</span>
                <span className="text-ember">Queue</span>
              </div>
            </div>
          </div>
        </aside>

        <main className="flex-1">
          <div className="rounded-2xl border border-white/10 bg-coal/70 p-6 shadow-glow">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
}
