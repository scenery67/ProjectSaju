import PageTitle from '../components/PageTitle';
import PersonaCard from '../components/PersonaCard';
import { PERSONAS } from '../data/personas';

export default function HomePage() {
  return (
    <main className="flex flex-1 flex-col gap-6 px-4 pb-6 pt-5">
      <section className="flex flex-col gap-1.5">
        <PageTitle>사주 상품 한눈에 보기</PageTitle>
        <p className="text-sm text-neutral-500">
          지금 마음이 궁금한 주제를 골라보세요
        </p>
      </section>

      <section className="grid grid-cols-2 gap-4">
        {PERSONAS.map((persona) => (
          <PersonaCard key={persona.id} persona={persona} />
        ))}
      </section>
    </main>
  );
}
