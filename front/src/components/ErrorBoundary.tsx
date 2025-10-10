import React from 'react';

type State = { hasError: boolean; error?: any };

export class ErrorBoundary extends React.Component<{ children: React.ReactNode }, State> {
  state: State = { hasError: false };
  static getDerivedStateFromError(error: any) { return { hasError: true, error }; }
  componentDidCatch(error: any, info: any) { console.error('ErrorBoundary', error, info); }
  render() {
    if (this.state.hasError) {
      return <div className="container"><div className="card"><div className="card-body">
        <h3 className="card-title">문제가 발생했습니다</h3>
        <div className="muted">{String(this.state.error)}</div>
      </div></div></div>;
    }
    return this.props.children;
  }
}

