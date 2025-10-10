import React, { createContext, useCallback, useContext, useMemo, useState } from 'react';

export type ToastKind = 'success' | 'error' | 'info';
export type Toast = { id: string; kind: ToastKind; message: string; ttl: number };

type ToastCtx = {
  toasts: Toast[];
  show: (message: string, kind?: ToastKind, ttlMs?: number) => void;
  success: (message: string, ttlMs?: number) => void;
  error: (message: string, ttlMs?: number) => void;
};

const ToastContext = createContext<ToastCtx | null>(null);

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const dismiss = useCallback((id: string) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  }, []);

  const show = useCallback((message: string, kind: ToastKind = 'info', ttlMs = 2500) => {
    const id = Math.random().toString(36).slice(2);
    const toast: Toast = { id, kind, message, ttl: Date.now() + ttlMs };
    setToasts(prev => [...prev, toast]);
    window.setTimeout(() => dismiss(id), ttlMs);
  }, [dismiss]);

  const value = useMemo(() => ({
    toasts,
    show,
    success: (m: string, t?: number) => show(m, 'success', t),
    error: (m: string, t?: number) => show(m, 'error', t)
  }), [toasts, show]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-container">
        {toasts.map(t => (
          <div key={t.id} className={`toast ${t.kind}`}>
            {t.message}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within ToastProvider');
  return ctx;
}

