export default function PlaceholderPage({ title }: { title: string }) {
  return (
    <main className="flex flex-1 items-center justify-center p-4 text-sm text-neutral-400">
      {title} (준비 중)
    </main>
  );
}
