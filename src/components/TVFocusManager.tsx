import React, { useState, useEffect, useCallback, createContext, useContext } from 'react';
import { isKey, TVKeys } from '../lib/TVUtils';

interface FocusContextType {
  focusedId: string | null;
  setFocusedId: (id: string | null) => void;
  register: (id: string, ref: React.RefObject<HTMLElement>) => void;
  unregister: (id: string) => void;
}

const FocusContext = createContext<FocusContextType | undefined>(undefined);

export const useFocus = () => {
  const context = useContext(FocusContext);
  if (!context) throw new Error('useFocus must be used within a FocusProvider');
  return context;
};

export const FocusProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [focusedId, setFocusedId] = useState<string | null>(null);
  const [elements, setElements] = useState<Map<string, React.RefObject<HTMLElement>>>(new Map());

  const register = useCallback((id: string, ref: React.RefObject<HTMLElement>) => {
    setElements(prev => {
      const next = new Map(prev);
      next.set(id, ref);
      return next;
    });
  }, []);

  const unregister = useCallback((id: string) => {
    setElements(prev => {
      const next = new Map(prev);
      next.delete(id);
      return next;
    });
  }, []);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // Back button handling (Tizen: 10009, webOS: 461, etc.)
      if (isKey(e, 'BACK')) {
        // If the user is typing in an input or textarea, don't intercept Backspace (8)
        const target = e.target as HTMLElement;
        const isInput = target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable;
        
        if (e.keyCode === 8 && isInput) {
          return; // Let the default backspace behavior happen (deleting text)
        }

        // If there's a modal or something, we might want to close it first
        // But for now, we'll just go back in history
        if (window.history.length > 1) {
          window.history.back();
          e.preventDefault();
        }
        return;
      }

      const focusableIds = Array.from(elements.keys());
      if (focusableIds.length === 0) return;

      const currentIndex = focusableIds.indexOf(focusedId || '');
      let nextIndex = -1;

      const currentEl = focusedId ? elements.get(focusedId)?.current : null;
      const currentRect = currentEl?.getBoundingClientRect();

      const findNearest = (direction: 'up' | 'down' | 'left' | 'right') => {
        if (!currentRect) return focusableIds[0];
        
        let bestId = null;
        let minDistance = Infinity;

        focusableIds.forEach(id => {
          if (id === focusedId) return;
          const el = elements.get(id)?.current;
          if (!el) return;
          const rect = el.getBoundingClientRect();

          let isCorrectDirection = false;
          let distance = 0;

          const centerX = rect.left + rect.width / 2;
          const centerY = rect.top + rect.height / 2;
          const currentCenterX = currentRect.left + currentRect.width / 2;
          const currentCenterY = currentRect.top + currentRect.height / 2;

          switch (direction) {
            case 'up':
              isCorrectDirection = centerY < currentCenterY;
              distance = Math.pow(currentCenterX - centerX, 2) + Math.pow(currentCenterY - centerY, 2) * 2;
              break;
            case 'down':
              isCorrectDirection = centerY > currentCenterY;
              distance = Math.pow(currentCenterX - centerX, 2) + Math.pow(currentCenterY - centerY, 2) * 2;
              break;
            case 'left':
              isCorrectDirection = centerX < currentCenterX;
              distance = Math.pow(currentCenterX - centerX, 2) * 2 + Math.pow(currentCenterY - centerY, 2);
              break;
            case 'right':
              isCorrectDirection = centerX > currentCenterX;
              distance = Math.pow(currentCenterX - centerX, 2) * 2 + Math.pow(currentCenterY - centerY, 2);
              break;
          }

          if (isCorrectDirection && distance < minDistance) {
            minDistance = distance;
            bestId = id;
          }
        });

        return bestId || focusedId;
      };

      let nextId = null;

      if (isKey(e, 'DOWN')) {
        nextId = findNearest('down');
      } else if (isKey(e, 'UP')) {
        nextId = findNearest('up');
      } else if (isKey(e, 'LEFT')) {
        nextId = findNearest('left');
      } else if (isKey(e, 'RIGHT')) {
        nextId = findNearest('right');
      } else if (isKey(e, 'ENTER')) {
        if (focusedId) {
          const el = elements.get(focusedId)?.current;
          el?.click();
        }
        return;
      } else {
        return;
      }

      if (nextId && nextId !== focusedId) {
        const nextRef = elements.get(nextId);
        setFocusedId(nextId);
        nextRef?.current?.focus();
        e.preventDefault();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [focusedId, elements]);

  return (
    <FocusContext.Provider value={{ focusedId, setFocusedId, register, unregister }}>
      {children}
    </FocusContext.Provider>
  );
};

export const Focusable: React.FC<{ id: string; children: (isFocused: boolean) => React.ReactNode }> = ({ id, children }) => {
  const { focusedId, setFocusedId, register, unregister } = useFocus();
  const ref = React.useRef<HTMLDivElement>(null);

  useEffect(() => {
    register(id, ref);
    return () => unregister(id);
  }, [id, register, unregister]);

  const isFocused = focusedId === id;

  return (
    <div 
      ref={ref} 
      tabIndex={0} 
      onFocus={() => setFocusedId(id)}
      className={`outline-none transition-all duration-300 ${isFocused ? 'ring-4 ring-primary scale-105 z-10' : ''}`}
    >
      {children(isFocused)}
    </div>
  );
};
