// Thin fetch wrapper. Base URL comes from env so local/prod can differ without code changes.
// 얇은 fetch 래퍼. 배포 환경별 API 주소는 코드 수정 없이 env로 분리한다.
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

// OAuth2 login endpoints (/oauth2/authorization/{provider}) live at the
// backend's root, not under /api — derive it once from BASE_URL.
export const API_ROOT_URL = BASE_URL.replace(/\/api\/?$/, '');

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export async function apiGet<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`);
  if (!res.ok) throw new ApiError(res.status, `GET ${path} failed`);
  return res.json() as Promise<T>;
}

export async function apiPost<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new ApiError(res.status, `POST ${path} failed`);
  return res.json() as Promise<T>;
}
