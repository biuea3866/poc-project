import "./globals.css";
import type { ReactNode } from "react";

export const metadata = {
  title: "AI Wiki",
  description: "Markdown authoring, AI analysis, and searchable knowledge.",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  );
}
