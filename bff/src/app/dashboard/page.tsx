import { verifySession } from '@/lib/dal';
import UpcomingMeetings from "@/app/components/dashboard/home/upcoming-meetings";

export default async function DashboardPage() {
  const session = await verifySession();

  return (
    <main style={{ padding: 32 }}>
      <h1>Welcome, @{session.username}!</h1>
      <p>You are signed in to Meeting Planner.</p>
      <UpcomingMeetings/>
    </main>
  );
}