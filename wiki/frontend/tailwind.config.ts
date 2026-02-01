import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#0b0e13",
        coal: "#111720",
        steel: "#1b2330",
        linen: "#f4efe6",
        sand: "#d5c8b0",
        ember: "#f97316",
        mint: "#34d399"
      },
      boxShadow: {
        glow: "0 0 0 1px rgba(249,115,22,0.3), 0 12px 40px rgba(0,0,0,0.35)"
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
