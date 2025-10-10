import React from 'react';

export function Card({ children }: { children: React.ReactNode }) {
  return <div className="card"><div className="card-body">{children}</div></div>;
}

export function CardTitle({ children }: { children: React.ReactNode }) {
  return <h3 className="card-title">{children}</h3>;
}

