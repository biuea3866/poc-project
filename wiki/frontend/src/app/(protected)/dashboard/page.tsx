"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { clearTokens } from "@/lib/auth";

export default function DashboardPage() {
  const router = useRouter();

  return (
    <div className="flex flex-col gap-6">
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
      <section className="rounded-2xl border border-white/10 bg-steel/60 p-6">
        <p className="text-sm text-sand/70">
          This is a placeholder protected route. Next, we will plug in the tree
          view, editor, and AI status panels here.
        </p>
        <div className="mt-4 flex gap-3">
          <Link
            href="/login"
            className="rounded-full bg-ember px-4 py-2 text-sm font-semibold text-ink"
          >
            Go to login
          </Link>
          <Link
            href="/signup"
            className="rounded-full border border-white/20 px-4 py-2 text-sm text-linen"
          >
            Create another account
          </Link>
        </div>
      </section>
    </div>
  );
}
