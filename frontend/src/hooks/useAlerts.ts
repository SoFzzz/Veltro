import { useEffect, useRef } from 'react';
import { useAlertStore } from '../stores/alertStore';
import { getAlerts, type Alert } from '../api/inventory';

export const useAlerts = (pollInterval: number = 30000) => {
  const { setAlerts, addAlert } = useAlertStore();
  const pollingIntervalRef = useRef<NodeJS.Timeout | null>(null);
  const previousCountRef = useRef<number>(0);

  useEffect(() => {
    const fetchAlerts = async () => {
      try {
        const response = await getAlerts(0);
        const currentAlerts = response.data.content;

        // Check if new critical alert appeared
        const unreadCount = currentAlerts.filter((a: Alert) => !a.resolved).length;
        const criticalCount = currentAlerts.filter(
          (a: Alert) => a.severity === 'CRITICAL' && !a.resolved
        ).length;

        if (criticalCount > 0 && unreadCount > previousCountRef.current) {
          const newAlerts = currentAlerts.filter(
            (a: Alert) => a.severity === 'CRITICAL' && !a.resolved
          );
          newAlerts.forEach((alert: Alert) => {
            // Trigger notification (can be toast or browser notification)
            console.log('🚨 Critical Alert:', alert.message);
          });
        }

        previousCountRef.current = unreadCount;
        setAlerts(currentAlerts);
      } catch (error) {
        console.error('Error fetching alerts:', error);
      }
    };

    // Initial fetch
    fetchAlerts();

    // Set up polling
    pollingIntervalRef.current = setInterval(fetchAlerts, pollInterval);

    return () => {
      if (pollingIntervalRef.current) {
        clearInterval(pollingIntervalRef.current);
      }
    };
  }, [pollInterval, setAlerts]);

  return { addAlert };
};
