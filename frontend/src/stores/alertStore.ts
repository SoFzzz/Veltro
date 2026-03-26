import { create } from 'zustand';
import type { Alert, AlertSeverity } from '../api/inventory';

interface AlertStoreState {
  alerts: Alert[];
  unreadCount: number;
  setAlerts: (alerts: Alert[]) => void;
  addAlert: (alert: Alert) => void;
  removeAlert: (alertId: number) => void;
  markAsRead: (alertId: number) => void;
  clearAll: () => void;
  getUnreadAlerts: () => Alert[];
  getAlertsBySeverity: (severity: AlertSeverity) => Alert[];
}

export const useAlertStore = create<AlertStoreState>((set, get) => ({
  alerts: [],
  unreadCount: 0,

  setAlerts: (alerts) => {
    const unreadCount = alerts.filter((a) => !a.resolved).length;
    set({ alerts, unreadCount });
  },

  addAlert: (alert) => {
    set((state) => {
      const newAlerts = [alert, ...state.alerts];
      const unreadCount = newAlerts.filter((a) => !a.resolved).length;
      return { alerts: newAlerts, unreadCount };
    });
  },

  removeAlert: (alertId) => {
    set((state) => {
      const newAlerts = state.alerts.filter((a) => a.id !== alertId);
      const unreadCount = newAlerts.filter((a) => !a.resolved).length;
      return { alerts: newAlerts, unreadCount };
    });
  },

  markAsRead: (alertId) => {
    set((state) => {
      const newAlerts = state.alerts.map((a) =>
        a.id === alertId ? { ...a, resolved: true } : a
      );
      const unreadCount = newAlerts.filter((a) => !a.resolved).length;
      return { alerts: newAlerts, unreadCount };
    });
  },

  clearAll: () => {
    set({ alerts: [], unreadCount: 0 });
  },

  getUnreadAlerts: () => {
    return get().alerts.filter((a) => !a.resolved);
  },

  getAlertsBySeverity: (severity: AlertSeverity) => {
    return get().alerts.filter((a) => a.severity === severity);
  },
}));
