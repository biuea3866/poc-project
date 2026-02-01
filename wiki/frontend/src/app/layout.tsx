import type { Metadata } from "next";
import { Space_Grotesk, Source_Serif_4 } from "next/font/google";
import "./globals.css";

const space = Space_Grotesk({
  subsets: ["latin"],
  variable: "--font-sans"
});

const serif = Source_Serif_4({
  subsets: ["latin"],
  variable: "--font-serif"
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
    <html lang="ko" className={`${space.variable} ${serif.variable}`}>
      <body className="min-h-screen bg-ink font-[var(--font-sans)] text-linen antialiased">
        {children}
      </body>
    </html>
  );
}
