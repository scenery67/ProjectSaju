import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { findPersonaByType } from '../data/personas';
import { withBatchimPostposition } from '../lib/korean';
import {
  ConsultationApiError,
  fetchConsultationMessages,
  sendConsultationMessage,
  type ConsultationMessage,
  type ConsultationSession,
} from '../lib/consultation';
import type { PersonaType } from '../types/saju';

export default function ConsultationPage() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const location = useLocation();
  const session = (location.state as { session?: ConsultationSession } | null)?.session;
  const persona = session ? findPersonaByType(session.personaType as PersonaType) : undefined;

  const [messages, setMessages] = useState<ConsultationMessage[] | null>(null);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!sessionId) return;
    fetchConsultationMessages(sessionId)
      .then(setMessages)
      .catch(() => setMessages([]));
  }, [sessionId]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ block: 'end' });
  }, [messages]);

  if (!sessionId) {
    return (
      <main className="flex flex-1 flex-col items-center justify-center gap-3 p-4 text-sm text-neutral-500">
        상담 세션 정보가 없어요.
        <Link to="/" className="font-semibold text-violet-500 underline">
          홈으로 돌아가기
        </Link>
      </main>
    );
  }

  async function handleSend(e: React.FormEvent) {
    e.preventDefault();
    if (!sessionId || !input.trim() || sending) return;
    const content = input.trim();
    const pendingId = `pending-${Date.now()}`;
    setSending(true);
    setError(null);
    setMessages((prev) => [
      ...(prev ?? []),
      { id: pendingId, role: 'USER', content, createdAt: new Date().toISOString() },
    ]);
    setInput('');
    try {
      const reply = await sendConsultationMessage(sessionId, content);
      setMessages((prev) => [...(prev ?? []), reply]);
    } catch (e) {
      if (e instanceof ConsultationApiError && e.status === 402) {
        // 크레딧 부족이면 서버에 질문 자체가 저장되지 않는다 — 낙관적으로
        // 추가했던 사용자 메시지를 되돌린다(안 그러면 새로고침 시 사라져서 헷갈림).
        setMessages((prev) => prev?.filter((m) => m.id !== pendingId) ?? null);
        setInput(content);
        setError('크레딧이 부족해요. 마이페이지에서 충전해주세요.');
      } else if (e instanceof ConsultationApiError && e.status === 502) {
        setError('답변을 받아오지 못했어요 — 사용한 크레딧은 돌려드렸어요. 다시 시도해주세요.');
      } else {
        setError('문제가 생겼어요. 잠시 후 다시 시도해주세요.');
      }
    } finally {
      setSending(false);
    }
  }

  return (
    <main className="flex flex-1 flex-col px-4 pb-4 pt-5">
      <div className="mb-3 flex flex-col gap-1">
        <h2 className="text-xl font-bold tracking-tight text-white">
          {persona ? `${withBatchimPostposition(persona.characterName, '와', '과')}의 상담` : '상담'}
        </h2>
        <p className="text-xs text-neutral-400">질문 1건당 크레딧 1개가 소모돼요.</p>
      </div>

      <div className="flex flex-1 flex-col gap-3 overflow-y-auto">
        {messages === null && <p className="text-xs text-neutral-400">불러오는 중...</p>}
        {messages?.length === 0 && (
          <p className="text-xs text-neutral-400">
            궁금한 걸 편하게 물어보세요. {persona?.characterName ?? '상담사'}가 사주를 참고해서 답해줄 거예요.
          </p>
        )}
        {messages?.map((m) => (
          <div key={m.id} className={`flex ${m.role === 'USER' ? 'justify-end' : 'justify-start'}`}>
            <div
              className={`max-w-[80%] rounded-2xl px-4 py-2.5 text-sm leading-relaxed ${
                m.role === 'USER'
                  ? 'bg-violet-500 text-white'
                  : 'border border-neutral-800 bg-neutral-900 text-neutral-100'
              }`}
            >
              {m.content}
            </div>
          </div>
        ))}
        {sending && (
          <div className="flex justify-start">
            <div className="max-w-[80%] rounded-2xl border border-neutral-800 bg-neutral-900 px-4 py-2.5 text-sm text-neutral-400">
              답변을 준비하고 있어요...
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {error && <p className="mt-2 text-xs font-medium text-violet-500">{error}</p>}

      <form className="mt-3 flex gap-2" onSubmit={handleSend}>
        <input
          className="flex-1 rounded-full border border-neutral-800 bg-neutral-800 px-4 py-2.5 text-sm outline-none focus:border-violet-400"
          placeholder="궁금한 걸 물어보세요"
          value={input}
          maxLength={2000}
          disabled={sending}
          onChange={(e) => setInput(e.target.value)}
        />
        <button
          type="submit"
          disabled={sending || !input.trim()}
          className="shrink-0 rounded-full bg-violet-500 px-5 py-2.5 text-sm font-bold text-white disabled:opacity-50"
        >
          보내기
        </button>
      </form>
    </main>
  );
}
