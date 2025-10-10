import React from 'react';

type Props = React.InputHTMLAttributes<HTMLInputElement> & { label?: string; hint?: string };

export function Input({ label, hint, className = '', ...rest }: Props) {
  return (
    <div className="field">
      {label && <label className="label">{label}</label>}
      <input className={`input ${className}`} {...rest} />
      {hint && <span className="label">{hint}</span>}
    </div>
  );
}

type TAProps = React.TextareaHTMLAttributes<HTMLTextAreaElement> & { label?: string; hint?: string };
export function Textarea({ label, hint, className = '', ...rest }: TAProps) {
  return (
    <div className="field">
      {label && <label className="label">{label}</label>}
      <textarea className={`textarea ${className}`} {...rest} />
      {hint && <span className="label">{hint}</span>}
    </div>
  );
}

