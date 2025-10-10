import { useEffect, useState } from 'react';
import { getProfile } from '../api/auth';
import type { MemberResponse } from '../api/types';
import { useSyncTokenStore } from '../api/http';
import { Card, CardTitle } from '../components/Card';

export default function ProfilePage() {
  useSyncTokenStore();
  const [profile, setProfile] = useState<MemberResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    (async () => {
      try {
        const me = await getProfile();
        setProfile(me);
      } catch (e: any) { setError(e.message); }
    })();
  }, []);

  if (error) return <p style={{ color: 'var(--danger)' }}>{error}</p>;
  if (!profile) return <p className="muted">불러오는 중...</p>;
  return (
    <div style={{ maxWidth: 640 }}>
      <Card>
        <CardTitle>프로필</CardTitle>
        <div className="stack-v">
          <div className="space-between"><span className="muted">이름</span><span>{profile.name}</span></div>
          <div className="space-between"><span className="muted">아이디</span><span>{profile.userId}</span></div>
          <div className="space-between"><span className="muted">권한</span><span>{profile.roles.join(', ')}</span></div>
        </div>
      </Card>
    </div>
  );
}
