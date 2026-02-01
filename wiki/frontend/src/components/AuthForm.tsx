"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { login, signup } from "@/lib/auth";

type Mode = "login" | "signup";

const modeCopy: Record<Mode, { title: string; subtitle: string; action: string }> = {
  login: {
    title: "Log in to your workspace",
    subtitle: "Pick up where you left off. Your AI is already organizing.",
    action: "Log in"
  },
  signup: {
    title: "Create your AI Wiki account",
    subtitle: "Start with a note. We will keep it structured.",
    action: "Create account"
  }
};

export default function AuthForm({ mode }: { mode: Mode }) {
  const router = useRouter();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [status, setStatus] = useState<"idle" | "loading" | "error" | "success">(
    "idle"
  );
  const [error, setError] = useState("");

  const copy = modeCopy[mode];

  const onSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setStatus("loading");
    setError("");

    try {
      if (mode === "signup") {
        await signup({ name, email, password });
      } else {
        await login({ email, password });
      }
      setStatus("success");
      router.replace("/dashboard");
    } catch (err) {
      setStatus("error");
      setError(err instanceof Error ? err.message : "Login failed. Try again.");
    }
  };

  return (
    <div className="flex h-full flex-col gap-6">
      <div>
        <h2 className="text-2xl font-semibold text-linen">{copy.title}</h2>
        <p className="mt-2 text-sm text-sand/70">{copy.subtitle}</p>
      </div>

      <form className="flex flex-col gap-5" onSubmit={onSubmit}>
        {mode === "signup" ? (
          <label className="flex flex-col gap-2 text-sm text-sand/70">
            Name
            <input
              required
              value={name}
              onChange={(event) => setName(event.target.value)}
              className="rounded-xl border border-white/10 bg-ink/60 px-4 py-3 text-base text-linen placeholder:text-sand/40 focus:border-ember/70 focus:outline-none"
              placeholder="홍길동"
            />
          </label>
        ) : null}
        <label className="flex flex-col gap-2 text-sm text-sand/70">
          Email
          <input
            required
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className="rounded-xl border border-white/10 bg-ink/60 px-4 py-3 text-base text-linen placeholder:text-sand/40 focus:border-ember/70 focus:outline-none"
            placeholder="you@company.com"
          />
        </label>
        <label className="flex flex-col gap-2 text-sm text-sand/70">
          Password
          <input
            required
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className="rounded-xl border border-white/10 bg-ink/60 px-4 py-3 text-base text-linen placeholder:text-sand/40 focus:border-ember/70 focus:outline-none"
            placeholder="••••••••"
          />
        </label>
        {status === "error" ? (
          <p className="rounded-lg border border-ember/40 bg-ember/10 px-3 py-2 text-sm text-ember">
            {error}
          </p>
        ) : null}
        <button
          type="submit"
          disabled={status === "loading"}
          className="rounded-xl bg-ember px-5 py-3 text-sm font-semibold text-ink transition hover:translate-y-[-1px] hover:shadow-glow disabled:cursor-not-allowed disabled:opacity-60"
        >
          {status === "loading" ? "Working..." : copy.action}
        </button>
      </form>

      <div className="text-sm text-sand/70">
        {mode === "login" ? (
          <span>
            New here?{" "}
            <Link className="text-linen underline-offset-4 hover:underline" href="/signup">
              Create an account
            </Link>
          </span>
        ) : (
          <span>
            Already have an account?{" "}
            <Link className="text-linen underline-offset-4 hover:underline" href="/login">
              Log in
            </Link>
          </span>
        )}
      </div>
    </div>
  );
}
