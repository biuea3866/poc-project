import type { DocumentCard } from "@/lib/types";

const statusTone = {
  DRAFT: "#5c6b5c",
  ACTIVE: "#126f54",
  DELETED: "#b6465f",
};

const aiTone = {
  NOT_STARTED: "#6b7280",
  PENDING: "#9a7b00",
  PROCESSING: "#0f766e",
  COMPLETED: "#166534",
  FAILED: "#b91c1c",
};

export function DocumentStatusBoard({ items }: { items: DocumentCard[] }) {
  return (
    <section style={{ display: "grid", gap: 16 }}>
      {items.map((item) => (
        <article
          key={item.id}
          style={{
            border: "1px solid rgba(31,42,31,0.12)",
            borderRadius: 18,
            padding: 20,
            background: "rgba(255,255,255,0.7)",
            boxShadow: "0 10px 30px rgba(31,42,31,0.06)",
          }}
        >
          <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
            <div>
              <h2 style={{ margin: 0, fontSize: 22 }}>{item.title}</h2>
              <p style={{ margin: "8px 0 0", color: "#475569", lineHeight: 1.5 }}>{item.excerpt}</p>
            </div>
            <div style={{ display: "grid", gap: 8, alignContent: "start" }}>
              <span style={{ color: statusTone[item.status], fontWeight: 700 }}>{item.status}</span>
              <span style={{ color: aiTone[item.aiStatus], fontWeight: 700 }}>{item.aiStatus}</span>
            </div>
          </div>
          <div style={{ display: "flex", flexWrap: "wrap", gap: 8, marginTop: 16 }}>
            {item.tags.map((tag) => (
              <span
                key={tag}
                style={{
                  borderRadius: 999,
                  padding: "6px 10px",
                  background: "#edf7ef",
                  color: "#126f54",
                  fontSize: 12,
                  fontWeight: 700,
                }}
              >
                #{tag}
              </span>
            ))}
          </div>
          <div style={{ marginTop: 14, fontSize: 13, color: "#64748b" }}>최근 수정: {item.updatedAt}</div>
        </article>
      ))}
    </section>
  );
}
