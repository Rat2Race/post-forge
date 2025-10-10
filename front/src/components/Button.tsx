import React from 'react';

type Props = React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'ghost' | 'danger' | 'success';
  block?: boolean;
};

export default function Button({ variant = 'primary', block, className = '', ...rest }: Props) {
  const cls = `btn ${variant === 'ghost' ? 'btn-ghost' : variant === 'danger' ? 'btn-danger' : variant === 'success' ? 'btn-success' : ''} ${block ? 'btn-block' : ''} ${className}`;
  return <button className={cls} {...rest} />;
}
