import React, { createContext, useContext, useMemo, useState } from 'react';

type LoadingCtx = {
  active: boolean;
  start: () => void;
  stop: () => void;
  withLoading: <T>(fn: () => Promise<T>) => Promise<T>;
};

const LoadingContext = createContext<LoadingCtx | null>(null);

export function LoadingProvider({ children }: { children: React.ReactNode }) {
  const [count, setCount] = useState(0);
  const start = () => setCount(c => c + 1);
  const stop = () => setCount(c => Math.max(0, c - 1));
  const withLoading = async <T,>(fn: () => Promise<T>) => { start(); try { return await fn(); } finally { stop(); } };
  const value = useMemo(() => ({ active: count > 0, start, stop, withLoading }), [count]);
  return (
    <LoadingContext.Provider value={value}>
      {children}
      {count > 0 && (
        <div className="overlay"><div className="spinner" /></div>
      )}
    </LoadingContext.Provider>
  );
}

export function useLoading() {
  const ctx = useContext(LoadingContext);
  if (!ctx) throw new Error('useLoading must be used within LoadingProvider');
  return ctx;
}

