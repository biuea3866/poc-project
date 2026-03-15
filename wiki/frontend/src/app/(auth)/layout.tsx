export default function AuthLayout({
  children
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen items-start justify-center bg-surface px-6 pt-24">
      {children}
    </div>
  );
}
