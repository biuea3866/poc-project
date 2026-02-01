"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { clearTokens } from "@/lib/auth";

export default function DashboardPage() {
  const router = useRouter();

  return (
    <div className="flex flex-col gap-8">
      <header className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <p className="text-sm uppercase tracking-[0.3em] text-sand/60">
            Dashboard
          </p>
          <h1 className="text-3xl font-semibold text-linen">Welcome back.</h1>
        </div>
        <button
          type="button"
          onClick={() => {
            clearTokens();
            router.replace("/login");
          }}
          className="rounded-full border border-white/15 px-4 py-2 text-sm text-linen transition hover:border-ember/60 hover:text-ember"
        >
          Log out
        </button>
      </header>

      <section className="grid gap-4 lg:grid-cols-[1.2fr_0.8fr]">
        <div className="rounded-2xl border border-white/10 bg-steel/60 p-6">
          <h2 className="text-lg font-semibold text-linen">Today&apos;s focus</h2>
          <p className="mt-2 text-sm text-sand/70">
            Keep writing. The AI pipeline will summarize and tag as you go.
          </p>
          <div className="mt-6 grid gap-3 md:grid-cols-2">
            {[
              "Product notes",
              "Architecture decisions",
              "Release checklist",
              "Customer insights"
            ].map((item) => (
              <div
                key={item}
                className="rounded-xl border border-white/10 bg-ink/60 p-4 text-sm text-sand/70"
              >
                {item}
              </div>
            ))}
          </div>
        </div>
        <div className="rounded-2xl border border-white/10 bg-ink/60 p-6">
          <h2 className="text-lg font-semibold text-linen">Quick actions</h2>
          <div className="mt-4 flex flex-col gap-3 text-sm text-sand/70">
            <Link
              href="/login"
              className="rounded-xl border border-white/15 px-4 py-3 transition hover:border-ember/60 hover:text-linen"
            >
              Review auth flow
            </Link>
            <Link
              href="/signup"
              className="rounded-xl border border-white/15 px-4 py-3 transition hover:border-ember/60 hover:text-linen"
            >
              Invite a teammate
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
