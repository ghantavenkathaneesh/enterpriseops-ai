import axios from 'axios';
import { useCallback, useEffect, useState } from 'react';

export const TOKEN_KEY = 'eo_token';
export const api = axios.create({ baseURL: '/api/v1' });

api.interceptors.request.use((cfg) => {
  const t = localStorage.getItem(TOKEN_KEY);
  if (t) cfg.headers.Authorization = `Bearer ${t}`;
  return cfg;
});
api.interceptors.response.use(
  (r) => r,
  (err) => {
    if (axios.isAxiosError(err) && err.response?.status === 401 && !location.pathname.startsWith('/login')) {
      localStorage.removeItem(TOKEN_KEY);
      location.href = '/login';
    }
    return Promise.reject(err);
  },
);

export function errorMessage(e: unknown): string {
  if (axios.isAxiosError(e)) {
    const d = e.response?.data as { error?: string; fieldErrors?: Record<string, string> } | undefined;
    if (d?.fieldErrors) return Object.values(d.fieldErrors).join(', ');
    if (d?.error) return d.error;
    if (e.response) return `Request failed (${e.response.status})`;
    return 'Cannot reach the server';
  }
  return 'Unexpected error';
}

/** Loads data and exposes loading/error state plus a reload function. */
export function useFetch<T>(fn: () => Promise<T>, deps: unknown[]) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const run = useCallback(() => {
    setLoading(true);
    setError(null);
    fn().then(setData).catch((e) => setError(errorMessage(e))).finally(() => setLoading(false));
  }, deps);
  useEffect(() => { run(); }, [run]);
  return { data, loading, error, reload: run };
}

export async function downloadCsv(path: string, filename: string) {
  const res = await api.get(path, { responseType: 'blob' });
  const url = URL.createObjectURL(res.data as Blob);
  const a = document.createElement('a');
  a.href = url; a.download = filename; a.click();
  URL.revokeObjectURL(url);
}
