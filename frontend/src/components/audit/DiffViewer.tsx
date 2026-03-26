import React, { useMemo } from 'react';

interface DiffViewerProps {
  previousData: Record<string, unknown>;
  newData: Record<string, unknown>;
}

/**
 * DiffViewer - Display JSON diff between two objects in a side-by-side view
 */
export const DiffViewer: React.FC<DiffViewerProps> = ({ previousData, newData }) => {
  const allKeys = useMemo(() => {
    const keys = new Set([
      ...Object.keys(previousData),
      ...Object.keys(newData),
    ]);
    return Array.from(keys).sort();
  }, [previousData, newData]);

  const formatValue = (value: unknown): string => {
    if (value === null || value === undefined) {
      return 'null';
    }
    if (typeof value === 'object') {
      return JSON.stringify(value, null, 2);
    }
    return String(value);
  };

  const isChanged = (key: string): boolean => {
    const prev = previousData[key];
    const curr = newData[key];
    return JSON.stringify(prev) !== JSON.stringify(curr);
  };

  if (allKeys.length === 0) {
    return (
      <div className="text-center py-6" style={{ color: '#6B7280' }}>
        No changes detected
      </div>
    );
  }

  return (
    <div className="overflow-x-auto max-h-96 overflow-y-auto rounded-lg" style={{ border: '1px solid #E8E3DB' }}>
      <table className="w-full text-sm">
        <thead style={{ backgroundColor: '#F9F7F2', position: 'sticky', top: 0 }}>
          <tr>
            <th className="px-4 py-2 text-left font-semibold" style={{ color: '#1F2937', width: '33.33%' }}>
              Field
            </th>
            <th className="px-4 py-2 text-left font-semibold" style={{ color: '#1F2937', width: '33.33%' }}>
              Previous Value
            </th>
            <th className="px-4 py-2 text-left font-semibold" style={{ color: '#1F2937', width: '33.33%' }}>
              New Value
            </th>
          </tr>
        </thead>
        <tbody>
          {allKeys.map((key, idx) => {
            const prev = previousData[key];
            const curr = newData[key];
            const changed = isChanged(key);

            return (
              <tr
                key={key}
                style={{
                  backgroundColor: changed ? '#FFF4E6' : (idx % 2 === 0 ? '#FFFFFF' : '#F9F7F2'),
                  borderBottom: '1px solid #E8E3DB',
                  borderLeft: changed ? '4px solid #FF9500' : 'none',
                }}
              >
                <td className="px-4 py-2 font-medium" style={{ color: '#1F2937' }}>
                  {key}
                  {changed && (
                    <span className="ml-2 inline-block text-xs px-2 py-1 rounded" style={{ backgroundColor: '#FFF9E6', color: '#FF9500', border: '1px solid #FFAC00' }}>
                      Changed
                    </span>
                  )}
                </td>
                <td className="px-4 py-2 font-mono text-xs break-words max-w-xs" style={{ color: '#6B7280' }}>
                  <code className="p-1 rounded inline-block" style={{ backgroundColor: 'rgba(255,46,33,0.1)', color: '#FF2E21' }}>
                    {formatValue(prev)}
                  </code>
                </td>
                <td className="px-4 py-2 font-mono text-xs break-words max-w-xs" style={{ color: '#6B7280' }}>
                  <code className="p-1 rounded inline-block" style={{ backgroundColor: '#E8F4F0', color: '#10B981' }}>
                    {formatValue(curr)}
                  </code>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};
