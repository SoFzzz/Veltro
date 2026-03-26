import React, { useState, useRef } from 'react';
import type { SuggestedProduct } from '../../api/pos';
import { aiScanProduct } from '../../api/pos';
import { useFocusTrap } from '../../hooks/useFocusTrap';

interface AiIdentificationModalProps {
  /** Blob captured from canvas.toBlob() — sent as multipart to backend */
  imageBlob: Blob;
  /** Optional preview data-URL for displaying the captured frame */
  previewUrl?: string;
  onClose: () => void;
  /** Called when the user selects a suggestion. Receives productId (if matched) and productName */
  onProductSelected: (suggestion: SuggestedProduct) => void;
}

/**
 * AiIdentificationModal — sends a camera frame to POST /scanner/ai
 * and displays AI-suggested products with confidence scores.
 */
export const AiIdentificationModal: React.FC<AiIdentificationModalProps> = ({
  imageBlob,
  previewUrl,
  onClose,
  onProductSelected,
}) => {
  const modalRef = useRef<HTMLDivElement>(null);
  const [suggestions, setSuggestions] = useState<SuggestedProduct[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [processingTime, setProcessingTime] = useState<number | null>(null);
  const [strategyUsed, setStrategyUsed] = useState<string | null>(null);

  useFocusTrap(modalRef, onClose, true);

  // Send the image to the backend on mount
  React.useEffect(() => {
    let cancelled = false;

    const identify = async () => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await aiScanProduct(imageBlob, `ai-scan-${Date.now()}.jpg`);
        if (cancelled) return;

        const data = response.data;
        setSuggestions(data.suggestions);
        setProcessingTime(data.processingTimeMs);
        setStrategyUsed(data.strategyUsed);

        if (data.suggestions.length === 0) {
          setError('La IA no pudo identificar ningún producto. Intenta con otra imagen.');
        }
      } catch (err: unknown) {
        if (cancelled) return;
        // 501 = AI not configured on backend
        const status = (err as { response?: { status?: number } })?.response?.status;
        if (status === 501 || status === 503) {
          setError('El servicio de IA no está disponible en este momento.');
        } else {
          setError(
            err instanceof Error
              ? err.message
              : 'Error al identificar el producto. Intenta de nuevo.'
          );
        }
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    };

    identify();
    return () => { cancelled = true; };
  }, [imageBlob]);

  /** Confidence 0.0–1.0 → percentage display + color */
  const getConfidenceStyle = (confidence: number) => {
    const pct = Math.round(confidence * 100);
    if (pct >= 75) return { pct, color: 'text-[#038E57]', bg: 'bg-green-50', border: 'border-green-200' };
    if (pct >= 45) return { pct, color: 'text-[#FFAC00]', bg: 'bg-yellow-50', border: 'border-yellow-200' };
    return { pct, color: 'text-[#FF2E21]', bg: 'bg-red-50', border: 'border-red-200' };
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4" role="presentation">
      <div
        ref={modalRef}
        className="bg-[var(--surface-primary)] rounded-xl shadow-xl max-w-2xl w-full max-h-[90vh] flex flex-col"
        role="dialog"
        aria-modal="true"
        aria-labelledby="ai-modal-title"
        tabIndex={-1}
      >
        {/* Header */}
        <div className="px-6 py-4 border-b border-[var(--border-light)] flex items-center justify-between shrink-0">
          <div>
            <h2 id="ai-modal-title" className="text-lg font-bold text-[var(--text-primary)]">
              Identificación con IA
            </h2>
            <p className="text-xs text-[var(--text-muted)] mt-0.5">
              Productos sugeridos a partir de la imagen capturada
            </p>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-[var(--text-muted)] hover:bg-[var(--surface-secondary)] transition-colors"
            aria-label="Cerrar"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto px-6 py-4 space-y-4">
          {/* Preview thumbnail */}
          {previewUrl && (
            <div className="flex justify-center">
              <img
                src={previewUrl}
                alt="Imagen capturada"
                className="w-40 h-28 object-cover rounded-lg border border-[var(--border-default)]"
              />
            </div>
          )}

          {/* Loading state */}
          {isLoading && (
            <div className="flex flex-col items-center justify-center py-12 gap-3" aria-live="polite">
              <div className="animate-spin rounded-full h-10 w-10 border-4 border-[var(--primary-base)] border-t-transparent" />
              <p className="text-sm text-[var(--text-secondary)]">Analizando imagen con IA...</p>
            </div>
          )}

          {/* Error state */}
          {error && !isLoading && (
            <div className="p-4 bg-red-50 border border-red-200 rounded-lg" role="alert">
              <p className="text-sm text-red-700">{error}</p>
            </div>
          )}

          {/* Empty state */}
          {!isLoading && !error && suggestions.length === 0 && (
            <div className="text-center py-8">
              <p className="text-[var(--text-muted)]">No se encontraron productos</p>
            </div>
          )}

          {/* Suggestions list */}
          {!isLoading && suggestions.length > 0 && (
            <div className="space-y-3">
              {suggestions.map((s, idx) => {
                const { pct, color, bg, border } = getConfidenceStyle(s.confidence);
                return (
                  <div
                    key={s.productId ?? `suggestion-${idx}`}
                    className={`p-4 rounded-lg border ${border} ${bg} hover:shadow-sm transition-shadow`}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex-1 min-w-0">
                        <h3 className="font-semibold text-[var(--text-primary)] truncate">
                          {s.productName}
                        </h3>
                        <div className="flex flex-wrap gap-x-3 gap-y-0.5 mt-1 text-xs text-[var(--text-muted)]">
                          {s.barcode && <span>Código: {s.barcode}</span>}
                          {s.suggestedPrice && <span>Precio: S/ {parseFloat(s.suggestedPrice).toFixed(2)}</span>}
                          {s.productId && <span>ID: {s.productId}</span>}
                        </div>
                      </div>

                      <div className="flex flex-col items-end gap-2 shrink-0">
                        <div className={`text-2xl font-bold ${color}`} aria-label={`${pct}% de confianza`}>
                          {pct}%
                        </div>
                        <button
                          onClick={() => onProductSelected(s)}
                          className="px-4 py-1.5 bg-[var(--primary-base)] text-white text-sm font-medium rounded-lg hover:bg-[var(--primary-hover)] transition-colors"
                          aria-label={`Seleccionar ${s.productName}`}
                        >
                          Seleccionar
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* Processing info */}
          {!isLoading && processingTime !== null && (
            <p className="text-xs text-[var(--text-muted)] text-center">
              Procesado en {(processingTime / 1000).toFixed(1)}s
              {strategyUsed && <> · Estrategia: {strategyUsed}</>}
            </p>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-[var(--border-light)] flex justify-end shrink-0">
          <button
            onClick={onClose}
            className="btn-secondary px-5 py-2"
          >
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
};
