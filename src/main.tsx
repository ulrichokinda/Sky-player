import 'core-js/stable';
import 'regenerator-runtime/runtime';

// Basic polyfills for very old Smart TVs (Pre-ES6)
if (typeof (Object as any).assign !== 'function') {
  (Object as any).assign = function(target: any, ...sources: any[]) {
    if (target == null) throw new TypeError('Cannot convert undefined or null to object');
    const to = Object(target);
    for (let index = 1; index < arguments.length; index++) {
      const nextSource = arguments[index];
      if (nextSource != null) {
        for (const nextKey in nextSource) {
          if (Object.prototype.hasOwnProperty.call(nextSource, nextKey)) {
            to[nextKey] = nextSource[nextKey];
          }
        }
      }
    }
    return to;
  };
}

if (typeof (window as any).Promise !== 'function') {
  // If Promise is missing, we are in trouble, but core-js should have fixed it.
  console.warn("Promise is missing, hope core-js handles it");
}

import {StrictMode, Component, ReactNode} from 'react';
import {createRoot} from 'react-dom/client';
import App from './App.tsx';
import './index.css';
import { FocusProvider } from './components/TVFocusManager';

// Extra safety for queueMicrotask which React 18+ relies heavily on
if (typeof (window as any).queueMicrotask !== 'function') {
  (window as any).queueMicrotask = function (callback: () => void) {
    Promise.resolve().then(callback).catch(e => setTimeout(() => { throw e; }));
  };
}

class ErrorBoundary extends Component<{children: ReactNode}, {hasError: boolean, error: Error | null}> {
  constructor(props: {children: ReactNode}) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: any) {
    console.error("Uncaught error:", error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{ padding: 20, color: 'white', backgroundColor: 'red', minHeight: '100vh', fontFamily: 'monospace' }}>
          <h2>Something went wrong.</h2>
          <details style={{ whiteSpace: 'pre-wrap' }}>
            {this.state.error && this.state.error.toString()}
            <br />
            {this.state.error && this.state.error.stack}
          </details>
        </div>
      );
    }
    return this.props.children;
  }
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ErrorBoundary>
      <FocusProvider>
        <App />
      </FocusProvider>
    </ErrorBoundary>
  </StrictMode>,
);
