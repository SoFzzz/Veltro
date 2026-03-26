import { useEffect, useRef, useState, useCallback, useMemo } from 'react';
import { useZxing } from 'react-zxing';
import { DecodeHintType, BarcodeFormat } from '@zxing/library';
import { useCartStore } from '../../stores/cartStore';
import {
  getProductByBarcode,
  searchProducts,
  checkAiAvailable,
  type Product,
  type SuggestedProduct,
} from '../../api/pos';
import { AiIdentificationModal } from './AiIdentificationModal';

type ScannerMode = 'camera' | 'manual';

export const ScannerContainer: React.FC = () => {
  const barcodeInputRef = useRef<HTMLInputElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Scanner state
  const [mode, setMode] = useState<ScannerMode>('camera');
  const [barcodeValue, setBarcodeValue] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<Product[]>([]);
  const [showResults, setShowResults] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);
  const [searchLoading, setSearchLoading] = useState(false);

  // Camera / AI state
  const [cameraReady, setCameraReady] = useState(false);
  const [aiAvailable, setAiAvailable] = useState(false);
  const [showAiButton, setShowAiButton] = useState(false);
  const [showAiModal, setShowAiModal] = useState(false);
  const [capturedBlob, setCapturedBlob] = useState<Blob | null>(null);
  const [capturedPreview, setCapturedPreview] = useState<string | undefined>();
  const [lastBarcode, setLastBarcode] = useState<string | null>(null);

  const addToCart = useCartStore((state) => state.add);

  // ----- ZXing decode hints: restrict to retail barcode formats + TRY_HARDER -----
  const hints = useMemo(() => {
    const map = new Map<DecodeHintType, any>();
    map.set(DecodeHintType.POSSIBLE_FORMATS, [
      BarcodeFormat.EAN_13,
      BarcodeFormat.EAN_8,
      BarcodeFormat.UPC_A,
      BarcodeFormat.UPC_E,
      BarcodeFormat.CODE_128,
      BarcodeFormat.CODE_39,
      BarcodeFormat.ITF,
      BarcodeFormat.CODABAR,
    ]);
    map.set(DecodeHintType.TRY_HARDER, true);
    return map;
  }, []);

  // ----- react-zxing camera barcode scanning -----
  const { ref: videoRef } = useZxing({
    paused: mode !== 'camera' || showAiModal,
    hints,
    timeBetweenDecodingAttempts: 150,
    constraints: {
      audio: false,
      video: {
        facingMode: 'environment',
        width: { ideal: 1280 },
        height: { ideal: 720 },
      },
    },
    onResult(result) {
      const text = result.getText();
      if (text && text !== lastBarcode) {
        setLastBarcode(text);
        handleBarcodeScan(text);
      }
    },
    onError(err) {
      // Ignore NotFound (no barcode in frame) — only log real errors
      if (err?.name !== 'NotFoundException') {
        console.warn('Scanner error:', err?.message);
      }
    },
  });

  // Robust camera readiness detection: poll video readyState since onPlaying
  // may not fire reliably with react-zxing's stream management
  useEffect(() => {
    if (mode !== 'camera') {
      setCameraReady(false);
      return;
    }
    const interval = setInterval(() => {
      const video = videoRef.current;
      if (video && video.readyState >= 2 && video.videoWidth > 0) {
        setCameraReady(true);
        clearInterval(interval);
      }
    }, 200);
    return () => clearInterval(interval);
  }, [mode, videoRef]);

  // Reset lastBarcode after a delay to allow re-scanning same code
  useEffect(() => {
    if (lastBarcode) {
      const t = setTimeout(() => setLastBarcode(null), 3000);
      return () => clearTimeout(t);
    }
  }, [lastBarcode]);

  // Check AI availability on mount
  useEffect(() => {
    checkAiAvailable()
      .then((res) => setAiAvailable(res.data.available))
      .catch(() => setAiAvailable(false));
  }, []);

  // 3-second timer: if camera is active and no barcode scanned, show AI button
  useEffect(() => {
    if (mode !== 'camera' || !cameraReady || !aiAvailable) {
      setShowAiButton(false);
      return;
    }

    const timer = setTimeout(() => {
      setShowAiButton(true);
    }, 3000);

    return () => clearTimeout(timer);
  }, [mode, cameraReady, aiAvailable, lastBarcode]); // reset on each scan

  // Auto-focus barcode input in manual mode
  useEffect(() => {
    if (mode === 'manual') {
      barcodeInputRef.current?.focus();
    }
  }, [mode]);

  // Close search dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setShowResults(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Clear messages after delay
  useEffect(() => {
    if (successMsg) {
      const t = setTimeout(() => setSuccessMsg(null), 2500);
      return () => clearTimeout(t);
    }
  }, [successMsg]);

  useEffect(() => {
    if (error) {
      const t = setTimeout(() => setError(null), 5000);
      return () => clearTimeout(t);
    }
  }, [error]);

  // ----- Handlers -----

  /** Handle barcode scan (from camera or manual input) */
  const handleBarcodeScan = useCallback(
    async (barcode: string) => {
      const code = barcode.trim();
      if (!code) return;

      setError(null);
      setLoading(true);
      try {
        const response = await getProductByBarcode(code);
        const product = response.data;
        addToCart(product, 1);
        setSuccessMsg(`+ ${product.name}`);
        setBarcodeValue('');
        barcodeInputRef.current?.focus();
      } catch {
        setError(`Producto no encontrado: ${code}`);
      } finally {
        setLoading(false);
      }
    },
    [addToCart]
  );

  /** Handle manual barcode submit (Enter key or button) */
  const handleBarcodeSubmit = useCallback(() => {
    handleBarcodeScan(barcodeValue);
  }, [barcodeValue, handleBarcodeScan]);

  const handleBarcodeKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      handleBarcodeSubmit();
    }
  };

  /** Debounced product name search */
  const searchTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const handleSearchChange = (value: string) => {
    setSearchQuery(value);
    if (searchTimeoutRef.current) clearTimeout(searchTimeoutRef.current);

    if (!value.trim()) {
      setSearchResults([]);
      setShowResults(false);
      return;
    }

    searchTimeoutRef.current = setTimeout(async () => {
      setSearchLoading(true);
      try {
        const results = await searchProducts(value);
        setSearchResults(results);
        setShowResults(results.length > 0);
      } catch {
        setSearchResults([]);
      } finally {
        setSearchLoading(false);
      }
    }, 300);
  };

  /** Select product from name search dropdown */
  const handleSelectProduct = (product: Product) => {
    addToCart(product, 1);
    setSuccessMsg(`+ ${product.name}`);
    setSearchQuery('');
    setSearchResults([]);
    setShowResults(false);
    barcodeInputRef.current?.focus();
  };

  /** Capture frame from video for AI identification */
  const handleCaptureForAi = useCallback(() => {
    const video = videoRef.current;
    if (!video) return;

    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth || 640;
    canvas.height = video.videoHeight || 480;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

    // Generate preview URL
    const previewUrl = canvas.toDataURL('image/jpeg', 0.8);
    setCapturedPreview(previewUrl);

    // Generate blob for upload
    canvas.toBlob(
      (blob) => {
        if (blob) {
          setCapturedBlob(blob);
          setShowAiModal(true);
        }
      },
      'image/jpeg',
      0.85
    );
  }, [videoRef]);

  /** Handle AI suggestion selected */
  const handleAiProductSelected = useCallback(
    async (suggestion: SuggestedProduct) => {
      setShowAiModal(false);
      setCapturedBlob(null);
      setCapturedPreview(undefined);

      // If the AI matched an existing product by ID, try fetching by barcode or ID
      if (suggestion.barcode) {
        try {
          const res = await getProductByBarcode(suggestion.barcode);
          addToCart(res.data, 1);
          setSuccessMsg(`+ ${res.data.name} (IA)`);
          return;
        } catch {
          // fall through to name search
        }
      }

      // Try finding by product name
      if (suggestion.productName) {
        try {
          const results = await searchProducts(suggestion.productName);
          if (results.length > 0) {
            // Pick the first match
            addToCart(results[0], 1);
            setSuccessMsg(`+ ${results[0].name} (IA)`);
            return;
          }
        } catch {
          // fall through
        }
      }

      setError(
        `IA identificó "${suggestion.productName}" pero no se encontró en el inventario.`
      );
    },
    [addToCart]
  );

  const handleAiModalClose = () => {
    setShowAiModal(false);
    setCapturedBlob(null);
    setCapturedPreview(undefined);
  };

  return (
    <div className="space-y-4">
      {/* Mode Toggle */}
      <div className="flex gap-2">
        <button
          onClick={() => setMode('camera')}
          className={`flex-1 py-2 px-4 text-sm font-medium rounded-lg border transition-colors ${
            mode === 'camera'
              ? 'bg-[var(--primary-base)] text-white border-[var(--primary-base)]'
              : 'bg-[var(--surface-primary)] text-[var(--text-secondary)] border-[var(--border-default)] hover:bg-[var(--surface-secondary)]'
          }`}
        >
          📷 Cámara
        </button>
        <button
          onClick={() => setMode('manual')}
          className={`flex-1 py-2 px-4 text-sm font-medium rounded-lg border transition-colors ${
            mode === 'manual'
              ? 'bg-[var(--primary-base)] text-white border-[var(--primary-base)]'
              : 'bg-[var(--surface-primary)] text-[var(--text-secondary)] border-[var(--border-default)] hover:bg-[var(--surface-secondary)]'
          }`}
        >
          ⌨️ Manual
        </button>
      </div>

      {/* Camera Scanner */}
      {mode === 'camera' && (
        <div className="space-y-3">
          <div className="relative rounded-xl overflow-hidden border border-[var(--border-default)] bg-black">
            <video
              ref={videoRef}
              className="w-full h-48 sm:h-56 object-cover"
              muted
              playsInline
            />
            {/* Scanning overlay guide */}
            {cameraReady && (
              <div className="absolute inset-0 pointer-events-none flex items-center justify-center">
                <div className="w-3/5 h-1/3 border-2 border-white/40 rounded-lg" />
              </div>
            )}
            {/* Loading camera */}
            {!cameraReady && (
              <div className="absolute inset-0 flex items-center justify-center bg-black/70">
                <div className="text-center text-white">
                  <div className="animate-spin rounded-full h-8 w-8 border-4 border-white border-t-transparent mx-auto mb-2" />
                  <p className="text-sm">Iniciando cámara...</p>
                </div>
              </div>
            )}
          </div>

          {/* AI identification button — appears after 3s with no barcode */}
          {showAiButton && (
            <button
              onClick={handleCaptureForAi}
              disabled={!cameraReady}
              className="w-full py-3 px-4 bg-gradient-to-r from-purple-600 to-indigo-600 text-white font-medium rounded-lg hover:from-purple-700 hover:to-indigo-700 transition-all shadow-md flex items-center justify-center gap-2"
            >
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M9.75 3.104v5.714a2.25 2.25 0 01-.659 1.591L5 14.5M9.75 3.104c-.251.023-.501.05-.75.082m.75-.082a24.301 24.301 0 014.5 0m0 0v5.714c0 .597.237 1.17.659 1.591L19.8 15.3M14.25 3.104c.251.023.501.05.75.082M19.8 15.3l-1.57.393A9.065 9.065 0 0112 15a9.065 9.065 0 00-6.23.693L5 14.5m14.8.8l1.402 1.402c1.232 1.232.65 3.318-1.067 3.611A48.309 48.309 0 0112 21c-2.773 0-5.491-.235-8.135-.687-1.718-.293-2.3-2.379-1.067-3.61L5 14.5" />
              </svg>
              Identificar con IA
            </button>
          )}

          <p className="text-xs text-[var(--text-muted)] text-center">
            Apunta la cámara al código de barras del producto.
            {aiAvailable && ' Si no se detecta, podrás usar IA para identificar el producto.'}
          </p>
        </div>
      )}

      {/* Manual Barcode Input */}
      {mode === 'manual' && (
        <div>
          <label htmlFor="barcode-input" className="block text-sm font-medium text-[var(--text-secondary)] mb-1.5">
            Código de barras
          </label>
          <div className="flex gap-2">
            <input
              ref={barcodeInputRef}
              id="barcode-input"
              type="text"
              value={barcodeValue}
              onChange={(e) => setBarcodeValue(e.target.value)}
              onKeyDown={handleBarcodeKeyDown}
              placeholder="Escanea o escribe el código de barras..."
              autoComplete="off"
              className="flex-1 px-4 py-3 text-base rounded-lg border border-[var(--border-default)] bg-[var(--surface-primary)] text-[var(--text-primary)] placeholder:text-[var(--text-muted)] focus:outline-none focus:ring-2 focus:ring-[var(--primary-base)] focus:border-[var(--primary-base)]"
              disabled={loading}
            />
            <button
              onClick={handleBarcodeSubmit}
              disabled={loading || !barcodeValue.trim()}
              className="btn-primary px-5 py-3 text-base whitespace-nowrap"
            >
              {loading ? 'Buscando...' : 'Agregar'}
            </button>
          </div>
          <p className="text-xs text-[var(--text-muted)] mt-1">
            Los escáneres USB envían el código automáticamente. Presiona Enter para buscar manualmente.
          </p>
        </div>
      )}

      {/* Product Name Search — available in both modes */}
      <div className="relative" ref={dropdownRef}>
        <label htmlFor="product-search" className="block text-sm font-medium text-[var(--text-secondary)] mb-1.5">
          Buscar producto por nombre
        </label>
        <input
          ref={searchInputRef}
          id="product-search"
          type="text"
          value={searchQuery}
          onChange={(e) => handleSearchChange(e.target.value)}
          onFocus={() => {
            if (searchResults.length > 0) setShowResults(true);
          }}
          placeholder="Escribe el nombre del producto..."
          autoComplete="off"
          className="w-full px-4 py-3 text-base rounded-lg border border-[var(--border-default)] bg-[var(--surface-primary)] text-[var(--text-primary)] placeholder:text-[var(--text-muted)] focus:outline-none focus:ring-2 focus:ring-[var(--primary-base)] focus:border-[var(--primary-base)]"
        />
        {searchLoading && (
          <div className="absolute right-3 top-[42px] text-[var(--text-muted)]">
            <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24" fill="none">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
          </div>
        )}

        {/* Search Results Dropdown */}
        {showResults && searchResults.length > 0 && (
          <div className="absolute z-50 w-full mt-1 max-h-64 overflow-y-auto rounded-lg border border-[var(--border-default)] bg-[var(--surface-primary)] shadow-lg">
            {searchResults.map((product) => (
              <button
                key={product.id}
                onClick={() => handleSelectProduct(product)}
                className="w-full text-left px-4 py-3 hover:bg-[var(--surface-secondary)] transition-colors border-b border-[var(--border-light)] last:border-b-0"
              >
                <div className="flex justify-between items-start">
                  <div className="min-w-0 flex-1">
                    <p className="font-medium text-sm text-[var(--text-primary)] truncate">
                      {product.name}
                    </p>
                    <p className="text-xs text-[var(--text-muted)]">
                      SKU: {product.sku} | Cod: {product.barcode} | {product.categoryName}
                    </p>
                  </div>
                  <span className="ml-3 text-sm font-semibold text-[var(--primary-base)] whitespace-nowrap">
                    S/ {parseFloat(product.salePrice).toFixed(2)}
                  </span>
                </div>
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Success Message */}
      {successMsg && (
        <div className="p-3 rounded-lg bg-green-50 border border-green-200 text-green-700 text-sm font-medium animate-pulse">
          {successMsg}
        </div>
      )}

      {/* Error Message */}
      {error && (
        <div className="p-3 rounded-lg bg-red-50 border border-red-200 text-red-600 text-sm flex items-center justify-between">
          <span>{error}</span>
          <button
            onClick={() => setError(null)}
            className="ml-2 text-red-400 hover:text-red-600 shrink-0"
          >
            ✕
          </button>
        </div>
      )}

      {/* AI Identification Modal */}
      {showAiModal && capturedBlob && (
        <AiIdentificationModal
          imageBlob={capturedBlob}
          previewUrl={capturedPreview}
          onClose={handleAiModalClose}
          onProductSelected={handleAiProductSelected}
        />
      )}
    </div>
  );
};
