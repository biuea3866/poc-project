import type { Metadata } from "next";
import { Noto_Sans_KR } from "next/font/google";
import Providers from "@/components/Providers";
import "./globals.css";

const notoSansKR = Noto_Sans_KR({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700", "800"],
  variable: "--font-sans"
});

export const metadata: Metadata = {
  title: "AI Wiki",
  description: "AI-powered knowledge workspace"
};

export default function RootLayout({
  children
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ko" className={notoSansKR.variable}>
      <body className="min-h-screen bg-surface font-[family-name:var(--font-sans)] text-primary antialiased">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
