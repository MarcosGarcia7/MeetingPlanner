import ShareLinkPage from '@/app/components/invite-link/share-link-page';

export default async function JoinLinkPage({params}: PageProps<'/join/[token]'>) {
  const {token} = await params;
  return <ShareLinkPage token={token}/>;
}
