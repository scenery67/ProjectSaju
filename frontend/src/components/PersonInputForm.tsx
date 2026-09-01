import type { PersonReadingInput } from '../types/saju';

interface Props {
  label: string;
  value: PersonReadingInput;
  onChange: (value: PersonReadingInput) => void;
}

// Reusable birth-info form used for both the "self" and "partner" inputs.
// 본인/상대방 입력에 공통으로 쓰는 생년월일 입력 폼.
export default function PersonInputForm({ label, value, onChange }: Props) {
  const set = <K extends keyof PersonReadingInput>(
    key: K,
    v: PersonReadingInput[K],
  ) => onChange({ ...value, [key]: v });

  return (
    <fieldset className="flex flex-col gap-3 rounded-xl border border-neutral-200 p-4">
      <legend className="px-1 text-sm font-semibold text-neutral-700">
        {label}
      </legend>

      <label className="flex flex-col gap-1 text-xs text-neutral-500">
        이름
        <input
          className="rounded-lg border border-neutral-300 px-3 py-2 text-sm text-neutral-800"
          value={value.name}
          onChange={(e) => set('name', e.target.value)}
          required
          maxLength={20}
        />
      </label>

      <div className="flex gap-2">
        <label className="flex flex-1 flex-col gap-1 text-xs text-neutral-500">
          생년월일
          <input
            type="date"
            className="rounded-lg border border-neutral-300 px-3 py-2 text-sm text-neutral-800"
            value={value.birthDate}
            onChange={(e) => set('birthDate', e.target.value)}
            required
          />
        </label>
        <label className="flex w-28 flex-col gap-1 text-xs text-neutral-500">
          태어난 시간
          <input
            type="time"
            className="rounded-lg border border-neutral-300 px-3 py-2 text-sm text-neutral-800"
            value={value.birthTime ?? ''}
            onChange={(e) => set('birthTime', e.target.value || null)}
          />
        </label>
      </div>

      <div className="flex gap-4">
        <label className="flex items-center gap-1 text-xs text-neutral-600">
          <input
            type="radio"
            name={`${label}-calendar`}
            checked={value.calendarType === 'SOLAR'}
            onChange={() => set('calendarType', 'SOLAR')}
          />
          양력
        </label>
        <label className="flex items-center gap-1 text-xs text-neutral-600">
          <input
            type="radio"
            name={`${label}-calendar`}
            checked={value.calendarType === 'LUNAR'}
            onChange={() => set('calendarType', 'LUNAR')}
          />
          음력
        </label>
        <label className="flex items-center gap-1 text-xs text-neutral-600">
          <input
            type="radio"
            name={`${label}-gender`}
            checked={value.gender === 'FEMALE'}
            onChange={() => set('gender', 'FEMALE')}
          />
          여성
        </label>
        <label className="flex items-center gap-1 text-xs text-neutral-600">
          <input
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
