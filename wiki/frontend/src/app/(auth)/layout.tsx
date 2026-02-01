export default function AuthLayout({
  children
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen px-6 py-12">
      <div className="mx-auto flex w-full max-w-5xl flex-col gap-12">
        <header className="flex flex-wrap items-end justify-between gap-6">
          <div>
            <p className="text-sm uppercase tracking-[0.3em] text-sand/70">
              AI Wiki
            </p>
            <h1 className="mt-3 text-4xl font-semibold text-linen">
              Write freely. AI keeps it crisp.
            </h1>
          </div>
          <div className="max-w-sm text-sm text-sand/70">
            Keep your knowledge in motion. We will summarize, tag, and connect
            it behind the scenes.
          </div>
        </header>
        <div className="grid gap-10 lg:grid-cols-[1.1fr_0.9fr]">
          <section className="rounded-2xl border border-white/10 bg-coal/70 p-8 shadow-glow">
            {children}
          </section>
          <aside className="rounded-2xl border border-white/10 bg-steel/60 p-8 text-sm text-sand/80">
            <h2 className="text-lg font-semibold text-linen">Why it works</h2>
            <ul className="mt-4 space-y-3">
              <li>Auto summaries in under 3 lines.</li>
              <li>Smart tags for fast retrieval.</li>
              <li>Document trees that scale with you.</li>
              <li>AI status tracked in real time.</li>
            </ul>
          </aside>
        </div>
      </div>
    </div>
  );
}
