import { useState } from 'react';
import type { PersonReadingInput } from '../types/saju';

interface Props {
  label: string;
  value: PersonReadingInput;
  onChange: (value: PersonReadingInput) => void;
}

function isoToYymmdd(iso: string): string {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(iso)) return '';
  return iso.slice(2, 4) + iso.slice(5, 7) + iso.slice(8, 10);
}

// 네이티브 <input type="date">는 모바일에서 연도 스크롤이 오래 걸려 생년
// 입력이 불편했다(사용자 리포트). "260901"처럼 6자리 숫자(YY MM DD)를 받아
// 직접 YYYY-MM-DD로 조합한다. 2자리 연도는 현재 연도의 뒤 두 자리보다 크면
// 1900년대, 작거나 같으면 2000년대로 판단한다(주민등록번호 앞자리와 같은
// 흔한 관례).
function BirthDateInput({
  value,
  onChange,
}: {
  value: string;
  onChange: (value: string) => void;
}) {
  const [raw, setRaw] = useState(() => isoToYymmdd(value));

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const digits = e.target.value.replace(/\D/g, '').slice(0, 6);
    setRaw(digits);

    if (digits.length === 6) {
      const yy = Number(digits.slice(0, 2));
      const month = Number(digits.slice(2, 4));
      const day = Number(digits.slice(4, 6));
      const currentYy = new Date().getFullYear() % 100;
      const year = (yy <= currentYy ? 2000 : 1900) + yy;
      if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
        const mm = String(month).padStart(2, '0');
        const dd = String(day).padStart(2, '0');
        onChange(`${year}-${mm}-${dd}`);
        return;
      }
    }
    onChange('');
  };

  return (
    <input
      type="text"
      inputMode="numeric"
      pattern="[0-9]*"
      maxLength={6}
      placeholder="260901"
      required
      className="rounded-xl border border-neutral-800 bg-neutral-800 px-3.5 py-2.5 text-sm text-neutral-100 outline-none focus:border-violet-400"
      value={raw}
      onChange={handleChange}
    />
  );
}

// 네이티브 <input type="time">은 브라우저/OS 로케일에 따라 오전/오후 선택기가
// 붙으면서 폭이 늘어나 분(分) 입력란을 가리는 문제가 있었다. "2040"처럼
// 24시간제 4자리 숫자를 그대로 입력받아 직접 HH:mm으로 조합한다.
function BirthTimeInput({
  value,
  onChange,
}: {
  value: string | null;
  onChange: (value: string | null) => void;
}) {
  const [raw, setRaw] = useState(() => (value ?? '').replace(':', ''));

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const digits = e.target.value.replace(/\D/g, '').slice(0, 4);
    setRaw(digits);

    if (digits.length === 4) {
      const hour = Number(digits.slice(0, 2));
      const minute = Number(digits.slice(2, 4));
      if (hour <= 23 && minute <= 59) {
        onChange(`${digits.slice(0, 2)}:${digits.slice(2, 4)}`);
        return;
      }
    }
    onChange(null);
  };

  return (
    <input
      type="text"
      inputMode="numeric"
      pattern="[0-9]*"
      maxLength={4}
      placeholder="2040"
      className="rounded-xl border border-neutral-800 bg-neutral-800 px-3.5 py-2.5 text-sm text-neutral-100 outline-none focus:border-violet-400"
      value={raw}
      onChange={handleChange}
    />
  );
}

// Reusable birth-info form used for both the "self" and "partner" inputs.
// 본인/상대방 입력에 공통으로 쓰는 생년월일 입력 폼.
export default function PersonInputForm({ label, value, onChange }: Props) {
  const set = <K extends keyof PersonReadingInput>(
    key: K,
    v: PersonReadingInput[K],
  ) => onChange({ ...value, [key]: v });

  return (
    <fieldset className="flex flex-col gap-3.5 rounded-3xl border border-neutral-800 bg-neutral-900 p-5">
      <legend className="px-1 text-sm font-bold text-neutral-100">
        {label}
      </legend>

      <label className="flex flex-col gap-1.5 text-xs font-medium text-neutral-500">
        이름
        <input
          className="rounded-xl border border-neutral-800 bg-neutral-800 px-3.5 py-2.5 text-sm text-neutral-100 outline-none focus:border-violet-400"
          value={value.name}
          onChange={(e) => set('name', e.target.value)}
          required
          maxLength={20}
        />
      </label>

      <div className="flex gap-2">
        <label className="flex flex-1 flex-col gap-1.5 text-xs font-medium text-neutral-500">
          생년월일
          <BirthDateInput
            value={value.birthDate}
            onChange={(v) => set('birthDate', v)}
          />
        </label>
        <label className="flex w-28 flex-col gap-1.5 text-xs font-medium text-neutral-500">
          태어난 시간
          <BirthTimeInput
            value={value.birthTime}
            onChange={(v) => set('birthTime', v)}
          />
        </label>
      </div>

      <div className="flex gap-4">
        <label className="flex items-center gap-1.5 text-xs font-medium text-neutral-400">
          <input
            className="accent-violet-500"
            type="radio"
            name={`${label}-calendar`}
            checked={value.calendarType === 'SOLAR'}
            onChange={() => set('calendarType', 'SOLAR')}
          />
          양력
        </label>
        <label className="flex items-center gap-1.5 text-xs font-medium text-neutral-400">
          <input
            className="accent-violet-500"
            type="radio"
            name={`${label}-calendar`}
            checked={value.calendarType === 'LUNAR'}
            onChange={() => set('calendarType', 'LUNAR')}
          />
          음력
        </label>
        <label className="flex items-center gap-1.5 text-xs font-medium text-neutral-400">
          <input
            className="accent-violet-500"
            type="radio"
            name={`${label}-gender`}
            checked={value.gender === 'FEMALE'}
            onChange={() => set('gender', 'FEMALE')}
          />
          여성
        </label>
        <label className="flex items-center gap-1.5 text-xs font-medium text-neutral-400">
          <input
            className="accent-violet-500"
            type="radio"
            name={`${label}-gender`}
            checked={value.gender === 'MALE'}
            onChange={() => set('gender', 'MALE')}
          />
          남성
        </label>
      </div>
    </fieldset>
  );
}
