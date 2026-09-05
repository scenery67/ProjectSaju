import { LogOut } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Emoji from '../components/Emoji';
import PageTitle from '../components/PageTitle';
import { useUser } from '../contexts/useUser';
import { AVATAR_EMOJI, updateProfile, type AvatarKey } from '../lib/auth';

const PROVIDER_LABEL: Record<string, string> = {
  KAKAO: '카카오',
  GOOGLE: 'Google',
  NAVER: '네이버',
  DEV_BYPASS: '개발용',
};

const AVATAR_OPTIONS = Object.keys(AVATAR_EMOJI) as AvatarKey[];

export default function SettingsPage() {
  const navigate = useNavigate();
  const { user, setUser, logout } = useUser();
  const [editing, setEditing] = useState(false);
  const [draftNickname, setDraftNickname] = useState('');
  const [draftAvatar, setDraftAvatar] = useState<AvatarKey>('FOX');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function startEditing() {
    if (!user) return;
    setDraftNickname(user.nickname);
    setDraftAvatar(user.avatarKey);
    setError(null);
    setEditing(true);
  }

  async function handleSave() {
    if (!draftNickname.trim()) {
      setError('닉네임을 입력해주세요.');
      return;
    }
    setSaving(true);
    const updated = await updateProfile(draftNickname.trim(), draftAvatar);
    setSaving(false);
    if (updated) {
      setUser(updated);
      setEditing(false);
    } else {
      setError('저장에 실패했어요. 잠시 후 다시 시도해주세요.');
    }
  }

  if (user === undefined) {
    return <main className="flex flex-1 items-center justify-center p-4 text-sm text-neutral-400">확인 중...</main>;
  }

  if (user === null) {
    return (
      <main className="flex flex-1 flex-col items-center justify-center gap-3 p-4 text-center text-sm text-neutral-500">
        로그인하면 설정을 볼 수 있어요.
        <Link to="/login" className="font-semibold text-violet-500 underline">
          로그인하기
        </Link>
      </main>
    );
  }

  return (
    <main className="flex flex-1 flex-col gap-6 px-4 pb-6 pt-5">
      <PageTitle>설정</PageTitle>

      <section className="flex flex-col gap-3">
        <h3 className="text-xs font-semibold text-neutral-500">프로필</h3>
        <div className="flex flex-col gap-3 rounded-2xl border border-neutral-800 bg-neutral-900 p-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <span className="flex h-12 w-12 items-center justify-center rounded-full bg-violet-950">
                <Emoji name={AVATAR_EMOJI[user.avatarKey]} className="h-7 w-7" />
              </span>
              <div className="flex flex-col">
                <span className="text-sm font-bold text-white">{user.nickname}</span>
                <span className="text-xs text-neutral-500">{PROVIDER_LABEL[user.provider] ?? user.provider} 계정</span>
              </div>
            </div>
            {!editing && (
              <button
                type="button"
                onClick={startEditing}
                className="rounded-full border border-teal-700 px-3.5 py-1.5 text-xs font-semibold text-teal-400"
              >
                편집
              </button>
            )}
          </div>

          {editing && (
            <div className="flex flex-col gap-3 border-t border-neutral-800 pt-3">
              <div className="flex flex-col gap-1.5">
                <label className="text-[11px] font-medium text-neutral-400">닉네임</label>
                <input
                  className="rounded-xl border border-neutral-800 bg-neutral-800 px-3.5 py-2.5 text-sm text-white outline-none focus:border-violet-400"
                  value={draftNickname}
                  maxLength={20}
                  onChange={(e) => setDraftNickname(e.target.value)}
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="text-[11px] font-medium text-neutral-400">아바타</label>
                <div className="grid grid-cols-8 gap-2">
                  {AVATAR_OPTIONS.map((key) => (
                    <button
                      key={key}
                      type="button"
                      onClick={() => setDraftAvatar(key)}
                      className={`flex h-9 w-9 items-center justify-center rounded-full ${
                        draftAvatar === key ? 'bg-violet-600' : 'bg-neutral-800'
                      }`}
                    >
                      <Emoji name={AVATAR_EMOJI[key]} className="h-5 w-5" />
                    </button>
                  ))}
                </div>
              </div>
              {error && <p className="text-xs font-medium text-violet-500">{error}</p>}
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={handleSave}
                  disabled={saving}
                  className="flex-1 rounded-full bg-teal-500 py-2.5 text-xs font-bold text-white disabled:opacity-50"
                >
                  {saving ? '저장 중...' : '변경'}
                </button>
                <button
                  type="button"
                  onClick={() => setEditing(false)}
                  className="flex-1 rounded-full border border-neutral-800 py-2.5 text-xs font-semibold text-neutral-400"
                >
                  취소
                </button>
              </div>
            </div>
          )}
        </div>
      </section>

      <section className="flex flex-col gap-3">
        <h3 className="text-xs font-semibold text-neutral-500">계정</h3>
        <div className="flex flex-col divide-y divide-neutral-800 rounded-2xl border border-neutral-800 bg-neutral-900">
          <div className="flex items-center justify-between px-4 py-3.5">
            <span className="text-sm text-neutral-100">로그인 방식</span>
            <span className="rounded-full bg-emerald-900/60 px-2.5 py-1 text-[11px] font-semibold text-emerald-300">
              {PROVIDER_LABEL[user.provider] ?? user.provider}
            </span>
          </div>
        </div>
      </section>

      <section className="flex flex-col gap-3">
        <h3 className="text-xs font-semibold text-neutral-500">개인정보 및 보안</h3>
        <div className="flex flex-col divide-y divide-neutral-800 rounded-2xl border border-neutral-800 bg-neutral-900">
          {['개인정보 처리방침', '서비스 이용약관', '데이터 관리'].map((label) => (
            <div key={label} className="flex items-center justify-between px-4 py-3.5">
              <span className="text-sm text-neutral-100">{label}</span>
              <span className="rounded-full bg-neutral-800 px-2 py-0.5 text-[10px] font-semibold text-neutral-500">
                준비중
              </span>
            </div>
          ))}
        </div>
      </section>

      <section className="flex flex-col gap-3">
        <h3 className="text-xs font-semibold text-neutral-500">앱 정보</h3>
        <div className="flex flex-col divide-y divide-neutral-800 rounded-2xl border border-neutral-800 bg-neutral-900">
          <div className="flex items-center justify-between px-4 py-3.5">
            <span className="text-sm text-neutral-100">버전</span>
            <span className="text-xs text-neutral-500">1.0.0</span>
          </div>
          <div className="flex items-center justify-between px-4 py-3.5">
            <span className="text-sm text-neutral-100">문의하기</span>
            <span className="rounded-full bg-neutral-800 px-2 py-0.5 text-[10px] font-semibold text-neutral-500">
              준비중
            </span>
          </div>
        </div>
      </section>

      <button
        type="button"
        className="flex items-center justify-center gap-2 rounded-2xl border border-neutral-800 py-3 text-sm font-semibold text-red-400"
        onClick={() => {
          void logout();
          navigate('/');
        }}
      >
        <LogOut className="h-4.5 w-4.5" strokeWidth={2} />
        로그아웃
      </button>
    </main>
  );
}
