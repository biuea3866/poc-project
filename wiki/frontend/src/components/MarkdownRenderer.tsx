"use client";

import ReactMarkdown from "react-markdown";

export default function MarkdownRenderer({ content }: { content: string }) {
  return (
    <div className="prose prose-slate max-w-none prose-headings:text-primary prose-p:text-secondary prose-a:text-accent hover:prose-a:underline prose-code:text-accent prose-code:bg-accent-light/50 prose-code:rounded prose-code:px-1.5 prose-pre:bg-primary prose-pre:text-surface" style={{ lineHeight: 1.8 }}>
      <ReactMarkdown>
        {content}
      </ReactMarkdown>
    </div>
  );
}
