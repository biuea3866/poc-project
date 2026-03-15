import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        surface: "#f8f9fa",
        card: "#ffffff",
        line: "#e9ecef",
        primary: "#212529",
        secondary: "#495057",
        muted: "#868e96",
        accent: "#6366f1",
        "accent-purple": "#8b5cf6",
        "accent-light": "#eef2ff",
        success: "#12b886",
        warning: "#f59e0b",
        danger: "#ef4444"
      },
      boxShadow: {
        sm: "0 1px 2px rgba(0,0,0,0.05)",
        card: "0 2px 8px rgba(0,0,0,0.08)"
      },
      borderRadius: {
        xl: "1rem",
        "2xl": "1.5rem"
      }
    }
  },
  plugins: []
};

export default config;
