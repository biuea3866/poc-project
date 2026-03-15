"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { login, signup } from "@/lib/auth";

type Mode = "login" | "signup";

const modeCopy: Record<Mode, { title: string; action: string }> = {
  login: {
    title: "로그인",
    action: "로그인"
  },
  signup: {
    title: "회원가입",
    action: "회원가입"
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
        setStatus("success");
        router.replace("/login");
      } else {
        await login({ email, password });
        setStatus("success");
        router.replace("/dashboard");
      }
    } catch (err) {
      setStatus("error");
      setError(err instanceof Error ? err.message : "문제가 발생했습니다. 다시 시도해주세요.");
    }
  };

  const inputClass =
    "w-full rounded-lg border border-line bg-white px-4 py-3 text-sm text-primary placeholder:text-muted focus:border-accent focus:outline-none focus:ring-1 focus:ring-accent";

  return (
    <div className="w-full max-w-sm rounded-2xl bg-white p-8 shadow-card">
      {/* Logo */}
      <div className="mb-6 text-center">
        <h1 className="text-2xl font-extrabold">
          <span className="bg-gradient-to-r from-accent to-accent-purple bg-clip-text text-transparent">
            AI Wiki
          </span>
        </h1>
        <p className="mt-2 text-sm text-muted">{copy.title}</p>
      </div>

      <form className="flex flex-col gap-4" onSubmit={onSubmit}>
        {mode === "signup" ? (
          <label className="flex flex-col gap-1.5 text-sm font-medium text-secondary">
            이름
            <input
              required
              value={name}
              onChange={(event) => setName(event.target.value)}
              className={inputClass}
              placeholder="홍길동"
            />
          </label>
        ) : null}
        <label className="flex flex-col gap-1.5 text-sm font-medium text-secondary">
          이메일
          <input
            required
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className={inputClass}
            placeholder="you@example.com"
          />
        </label>
        <label className="flex flex-col gap-1.5 text-sm font-medium text-secondary">
          비밀번호
          <input
            required
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className={inputClass}
            placeholder="••••••••"
          />
        </label>
        {status === "error" ? (
          <p className="rounded-lg border border-danger/30 bg-danger/5 px-3 py-2 text-sm text-danger">
            {error}
          </p>
        ) : null}
        <button
          type="submit"
          disabled={status === "loading"}
          className="mt-2 rounded-lg bg-gradient-to-r from-accent to-accent-purple px-5 py-3 text-sm font-semibold text-white transition hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {status === "loading" ? "처리 중..." : copy.action}
        </button>
      </form>

      <div className="mt-6 text-center text-sm text-muted">
        {mode === "login" ? (
          <span>
            계정이 없으신가요?{" "}
            <Link className="font-medium text-accent hover:underline" href="/signup">
              회원가입
            </Link>
          </span>
        ) : (
          <span>
            이미 계정이 있으신가요?{" "}
            <Link className="font-medium text-accent hover:underline" href="/login">
              로그인
            </Link>
          </span>
        )}
      </div>
    </div>
  );
}
