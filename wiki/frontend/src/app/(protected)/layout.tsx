"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { getAccessToken } from "@/lib/auth";

export default function ProtectedLayout({
  children
}: {
  children: React.ReactNode;
}) {
  const router = useRouter();

  useEffect(() => {
    if (!getAccessToken()) {
      router.replace("/login");
    }
  }, [router]);

  return (
    <div className="min-h-screen px-6 py-10">
      <div className="mx-auto w-full max-w-6xl rounded-2xl border border-white/10 bg-coal/70 p-8 shadow-glow">
        {children}
      </div>
    </div>
  );
}
