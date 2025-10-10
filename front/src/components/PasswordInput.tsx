import React, { useState } from 'react';

type Props = React.InputHTMLAttributes<HTMLInputElement> & { label?: string; hint?: string };

export default function PasswordInput({ label, hint, className = '', ...rest }: Props) {
  const [show, setShow] = useState(false);
  return (
    <div className="field">
      {label && <label className="label">{label}</label>}
      <div className="input-wrap">
        <input className={`input ${className}`} type={show ? 'text' : 'password'} {...rest} />
        <button type="button" className="input-affix-btn" onClick={() => setShow(s => !s)} aria-label="비밀번호 표시">
          {show ? '숨기기' : '표시'}
        </button>
      </div>
      {hint && <span className="label">{hint}</span>}
    </div>
  );
}

