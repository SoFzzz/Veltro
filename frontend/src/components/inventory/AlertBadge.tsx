import React from 'react';
import { useAlertStore } from '../../stores/alertStore';
import { useNavigate } from 'react-router-dom';

/**
 * AlertBadge - Display unread alerts count in header
 * Shows as red badge with count when alerts exist
 */
export const AlertBadge: React.FC = () => {
  const unreadCount = useAlertStore((state) => state.unreadCount);
  const navigate = useNavigate();

  if (unreadCount === 0) {
    return null;
  }

  const handleClick = () => {
    navigate('/alerts');
  };

  return (
    <button
      onClick={handleClick}
      className="relative inline-flex items-center justify-center w-10 h-10 rounded-full transition-colors"
      style={{
        backgroundColor: '#FF2E21',
        color: '#FFFFFF',
      }}
      onMouseEnter={(e) => {
        (e.currentTarget as HTMLButtonElement).style.filter = 'brightness(0.9)';
      }}
      onMouseLeave={(e) => {
        (e.currentTarget as HTMLButtonElement).style.filter = 'brightness(1)';
      }}
      aria-label={`${unreadCount} unread alerts`}
      title="Click to view alerts"
    >
      <svg
        className="w-5 h-5"
        fill="currentColor"
        viewBox="0 0 20 20"
        xmlns="http://www.w3.org/2000/svg"
      >
        <path d="M10.894 2.553a1 1 0 00-1.788 0l-7 14a1 1 0 001.169 1.409l5.951-1.429 5.951 1.429a1 1 0 001.169-1.409l-7-14z" />
      </svg>
      {unreadCount > 0 && (
        <span
          className="absolute top-0 right-0 inline-flex items-center justify-center px-2 py-1 text-xs font-bold leading-none rounded-full"
          style={{
            backgroundColor: '#FF2E21',
            color: '#FFFFFF',
            filter: 'brightness(0.85)',
          }}
        >
          {unreadCount > 99 ? '99+' : unreadCount}
        </span>
      )}
    </button>
  );
};
