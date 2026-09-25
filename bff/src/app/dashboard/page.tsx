import { verifySession } from '@/lib/dal';

export default async function DashboardPage() {
  const session = await verifySession();

  return (
    <main style={{ padding: 32 }}>
      <h1>Welcome, @{session.username}!</h1>
      <p>You are signed in to Meeting Planner.</p>
    </main>
  );
}