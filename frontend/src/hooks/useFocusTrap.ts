import { useEffect, useRef, useCallback } from 'react';

/**
 * Hook for managing focus trap in modals
 * - Traps tab focus within the modal
 * - Restores focus to trigger element when closed
 * - Handles Escape key to close modal
 */
export const useFocusTrap = (
  ref: React.RefObject<HTMLDivElement | null>,
  onClose: () => void,
  isOpen: boolean
) => {
  const previousActiveElement = useRef<HTMLElement | null>(null);

  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      // Close modal on Escape
      if (e.key === 'Escape') {
        onClose();
        return;
      }

      // Tab focus trap
      if (e.key !== 'Tab' || !ref.current) return;

      const focusableElements = ref.current.querySelectorAll(
        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
      ) as NodeListOf<HTMLElement>;

      if (focusableElements.length === 0) return;

      const firstElement = focusableElements[0];
      const lastElement = focusableElements[focusableElements.length - 1];
      const activeElement = document.activeElement as HTMLElement;

      // Shift+Tab from first element → focus last
      if (e.shiftKey && activeElement === firstElement) {
        e.preventDefault();
        lastElement.focus();
      }
      // Tab from last element → focus first
      else if (!e.shiftKey && activeElement === lastElement) {
        e.preventDefault();
        firstElement.focus();
      }
    },
    [ref, onClose]
  );

  useEffect(() => {
    if (!isOpen) return;

    // Save the previously focused element
    previousActiveElement.current = document.activeElement as HTMLElement;

    // Focus the modal or first focusable element
    if (ref.current) {
      const focusableElements = ref.current.querySelectorAll(
        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
      ) as NodeListOf<HTMLElement>;

      if (focusableElements.length > 0) {
        focusableElements[0].focus();
      } else {
        ref.current.focus();
      }
    }

    // Add keyboard listener
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('keydown', handleKeyDown);

      // Restore focus to previously focused element
      if (previousActiveElement.current && previousActiveElement.current.focus) {
        previousActiveElement.current.focus();
      }
    };
  }, [isOpen, handleKeyDown, ref]);
};
