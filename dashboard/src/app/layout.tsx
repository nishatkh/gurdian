import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Guardian AI — Security & Incident Administration",
  description: "Administrative and incident review console for Guardian AI Bangladesh",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className="dark">
      <body className="min-h-screen bg-[#050505] text-[#F5F7F2] antialiased">{children}</body>
    </html>
  );
}
