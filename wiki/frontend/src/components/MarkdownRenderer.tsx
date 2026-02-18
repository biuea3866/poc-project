"use client";

import ReactMarkdown from "react-markdown";

export default function MarkdownRenderer({ content }: { content: string }) {
  return (
    <div className="prose prose-slate max-w-none prose-headings:text-[#212529] prose-p:text-[#495057] prose-a:text-[#12b886] hover:prose-a:underline prose-code:text-[#12b886] prose-code:bg-[#f8f9fa] prose-code:px-1 prose-code:rounded prose-pre:bg-[#212529] prose-pre:text-[#f8f9fa]">
      <ReactMarkdown>
        {content}
      </ReactMarkdown>
    </div>
  );
}
