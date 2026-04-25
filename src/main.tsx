import {StrictMode, Component, ReactNode} from 'react';
import {createRoot} from 'react-dom/client';
import App from './App.tsx';
import './index.css';
import { FocusProvider } from './components/TVFocusManager';

// Polyfill pour les anciens navigateurs (Smart TV)
if (typeof (window as any).queueMicrotask !== 'function') {
  Object.defineProperty(window, 'queueMicrotask', {
    value: function (callback: () => void) {
      Promise.resolve().then(callback).catch(e => setTimeout(() => { throw e; }));
    },
    writable: true,
    configurable: true
  });
}

if (typeof (Object as any).assign !== 'function') {
  (Object as any).assign = function (target: any, ...sources: any[]) {
    if (target == null) throw new TypeError('Cannot convert undefined or null to object');
    const to = Object(target);
    for (const source of sources) {
      if (source != null) {
        for (const key in source) {
          if (Object.prototype.hasOwnProperty.call(source, key)) to[key] = source[key];
        }
      }
    }
    return to;
  };
}

// Polyfills for Map, Set if missing
if (typeof (window as any).Map !== 'function') {
  (window as any).Map = class Map {
    _keys: any[] = [];
    _values: any[] = [];
    set(key: any, value: any) {
      const idx = this._keys.indexOf(key);
      if (idx === -1) {
        this._keys.push(key);
        this._values.push(value);
      } else {
        this._values[idx] = value;
      }
      return this;
    }
    get(key: any) {
      const idx = this._keys.indexOf(key);
      return idx === -1 ? undefined : this._values[idx];
    }
    has(key: any) { return this._keys.indexOf(key) !== -1; }
    delete(key: any) {
      const idx = this._keys.indexOf(key);
      if (idx !== -1) {
        this._keys.splice(idx, 1);
        this._values.splice(idx, 1);
        return true;
      }
      return false;
    }
  };
}
if (typeof (window as any).Set !== 'function') {
  (window as any).Set = class Set {
    _values: any[] = [];
    add(value: any) {
      if (!this.has(value)) this._values.push(value);
      return this;
    }
    has(value: any) { return this._values.indexOf(value) !== -1; }
    delete(value: any) {
      const idx = this._values.indexOf(value);
      if (idx !== -1) {
        this._values.splice(idx, 1);
        return true;
      }
      return false;
    }
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
