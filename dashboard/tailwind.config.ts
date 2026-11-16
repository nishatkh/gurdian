import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./src/**/*.{js,ts,jsx,tsx,mdx}"],
  theme: {
    extend: {
      colors: {
        obsidian: "#050505",
        charcoal: "#111111",
        card: "#171717",
        elevated: "#202020",
        lime: {
          guardian: "#B6FF55",
          muted: "#76A834",
        },
      },
    },
  },
  plugins: [],
};

export default config;
