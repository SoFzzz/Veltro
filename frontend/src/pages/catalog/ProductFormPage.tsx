import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { productApi, categoryApi } from '../../api/catalog';
import { ProductScanner, type ScannedProductData } from '../../components/catalog';
import type { Category } from '../../types';
import type { AxiosError } from 'axios';

const productSchema = z.object({
  name: z.string().min(1, 'El nombre es requerido'),
  barcode: z.string().optional(),
  sku: z.string().optional(),
  description: z.string().optional(),
  costPrice: z.string().min(1, 'El precio de costo es requerido'),
  salePrice: z.string().min(1, 'El precio de venta es requerido'),
  categoryId: z.string().optional(),
  minStockInfo: z.string().optional(),
  minStockWarning: z.string().optional(),
  minStockCritical: z.string().optional(),
}).refine((data) => {
  const info = parseInt(data.minStockInfo || '0') || 0;
  const warning = parseInt(data.minStockWarning || '0') || 0;
  const critical = parseInt(data.minStockCritical || '0') || 0;
  return critical <= warning && warning <= info;
}, {
  message: 'Los umbrales deben cumplir: Crítico <= Advertencia <= Informativo',
  path: ['minStockCritical'],
});

type ProductFormData = z.infer<typeof productSchema>;

export function ProductFormPage() {
  const navigate = useNavigate();
  const { id } = useParams<{ id: string }>();
  const isEditing = Boolean(id);

  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Scanner state
  const [showScanner, setShowScanner] = useState(false);
  const [scanWarning, setScanWarning] = useState<string | null>(null);
  const [scanSuccess, setScanSuccess] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    formState: { errors },
  } = useForm<ProductFormData>({
    resolver: zodResolver(productSchema),
  });

  useEffect(() => {
    loadCategories();
    if (isEditing && id) {
      loadProduct(parseInt(id));
    }
  }, [id]);

  // Clear scan messages after delay
  useEffect(() => {
    if (scanWarning) {
      const t = setTimeout(() => setScanWarning(null), 6000);
      return () => clearTimeout(t);
    }
  }, [scanWarning]);

  useEffect(() => {
    if (scanSuccess) {
      const t = setTimeout(() => setScanSuccess(null), 4000);
      return () => clearTimeout(t);
    }
  }, [scanSuccess]);

  const loadCategories = async () => {
    try {
      const data = await categoryApi.getAll();
      setCategories(data);
    } catch (err) {
      console.error('Error loading categories:', err);
    }
  };

  const loadProduct = async (productId: number) => {
    setIsLoading(true);
    try {
      const product = await productApi.getById(productId);
      reset({
        name: product.name,
        barcode: product.barcode || '',
        sku: product.sku || '',
        description: product.description || '',
        costPrice: product.costPrice,
        salePrice: product.salePrice,
        categoryId: product.categoryId?.toString() || '',
        minStockInfo: product.minStockInfo?.toString() || '0',
        minStockWarning: product.minStockWarning?.toString() || '0',
        minStockCritical: product.minStockCritical?.toString() || '0',
      });
    } catch (err) {
      setError('Error al cargar el producto');
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  /** Handle scanner result — auto-fill form fields */
  const handleScanResult = (data: ScannedProductData) => {
    setScanWarning(null);
    setScanSuccess(null);

    if (data.existsInDb) {
      // Product already exists — warn the user
      setScanWarning(
        `Este producto ya existe en el sistema (ID: ${data.existingProductId}). ` +
        `Si deseas editarlo, ve a la lista de productos.`
      );

      if (!isEditing) {
        // Still fill the fields so user can see what was found
        if (data.name) setValue('name', data.name);
        if (data.barcode) setValue('barcode', data.barcode);
        if (data.description) setValue('description', data.description);
        if (data.suggestedPrice) setValue('salePrice', data.suggestedPrice);
      }
      return;
    }

    // Product is new — fill fields
    let filled: string[] = [];

    if (data.barcode) {
      setValue('barcode', data.barcode);
      filled.push('código de barras');
    }
    if (data.name) {
      setValue('name', data.name);
      filled.push('nombre');
    }
    if (data.description) {
      setValue('description', data.description);
      filled.push('descripción');
    }
    if (data.suggestedPrice) {
      setValue('salePrice', data.suggestedPrice);
      filled.push('precio de venta');
    }

    if (data.source === 'ai' && data.confidence !== undefined) {
      const pct = Math.round(data.confidence * 100);
      setScanSuccess(
        `IA completó: ${filled.join(', ')} (${pct}% confianza). Verifica los datos antes de guardar.`
      );
    } else if (data.source === 'barcode-new') {
      setScanSuccess(`Código de barras registrado. Completa los demás campos.`);
    } else if (filled.length > 0) {
      setScanSuccess(`Campos completados: ${filled.join(', ')}.`);
    }
  };

  const onSubmit = async (data: ProductFormData) => {
    setIsSaving(true);
    setError(null);

    const productData = {
      name: data.name,
      barcode: data.barcode || undefined,
      sku: data.sku || undefined,
      description: data.description || undefined,
      costPrice: data.costPrice,
      salePrice: data.salePrice,
      categoryId: data.categoryId ? parseInt(data.categoryId) : undefined,
      minStockInfo: data.minStockInfo ? parseInt(data.minStockInfo) : 0,
      minStockWarning: data.minStockWarning ? parseInt(data.minStockWarning) : 0,
      minStockCritical: data.minStockCritical ? parseInt(data.minStockCritical) : 0,
    };

    try {
      if (isEditing && id) {
        await productApi.update(parseInt(id), productData);
      } else {
        await productApi.create(productData);
      }
      navigate('/catalog/products');
    } catch (err) {
      const axiosError = err as AxiosError<{ message: string }>;
      setError(axiosError.response?.data?.message || 'Error al guardar el producto');
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="text-[var(--text-secondary)]">Cargando...</div>
      </div>
    );
  }

  return (
    <div className="max-w-3xl mx-auto">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-[var(--text-primary)] tracking-tight">
          {isEditing ? 'Editar Producto' : 'Nuevo Producto'}
        </h1>
        <p className="mt-1 text-[var(--text-secondary)] text-sm">
          {isEditing ? 'Actualiza los datos del producto seleccionado.' : 'Completa el formulario para registrar un nuevo producto en el catálogo.'}
        </p>
      </div>

      {error && (
        <div className="p-4 rounded-xl mb-6 bg-red-50 border border-red-200 text-red-600 shadow-sm">
          {error}
        </div>
      )}

      {/* Scanner Section — toggle button + scanner */}
      <div className="mb-6">
        {!showScanner ? (
          <button
            type="button"
            onClick={() => setShowScanner(true)}
            className="w-full py-3 px-4 rounded-xl border-2 border-dashed border-[var(--border-default)] bg-[var(--surface-secondary)] text-[var(--text-secondary)] hover:border-[var(--primary-base)] hover:text-[var(--primary-base)] transition-colors flex items-center justify-center gap-2"
          >
            <span className="text-lg">📷</span>
            <span className="text-sm font-medium">Escanear producto con cámara o IA</span>
          </button>
        ) : (
          <div className="card p-4">
            <ProductScanner
              onResult={handleScanResult}
              onClose={() => setShowScanner(false)}
            />
          </div>
        )}

        {/* Scan warning (product exists) */}
        {scanWarning && (
          <div className="mt-3 p-3 rounded-lg bg-amber-50 border border-amber-200 text-amber-700 text-sm flex items-start gap-2">
            <span className="shrink-0 mt-0.5">⚠️</span>
            <span>{scanWarning}</span>
          </div>
        )}

        {/* Scan success */}
        {scanSuccess && (
          <div className="mt-3 p-3 rounded-lg bg-green-50 border border-green-200 text-green-700 text-sm flex items-start gap-2">
            <span className="shrink-0 mt-0.5">✓</span>
            <span>{scanSuccess}</span>
          </div>
        )}
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="card p-8 space-y-6">
        <div>
          <label htmlFor="name" className="block text-sm font-semibold text-[var(--text-primary)] mb-1.5">
            Nombre *
          </label>
          <input
             id="name"
             type="text"
             {...register('name')}
             className="input-base"
             placeholder="Ej. Teclado Mecánico Keychron"
           />
          {errors.name && (
            <p className="mt-1.5 text-sm font-medium text-red-500">{errors.name.message}</p>
          )}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label htmlFor="barcode" className="block text-sm font-semibold text-[var(--text-primary)] mb-1.5">
              Código de Barras
            </label>
            <input
               id="barcode"
               type="text"
               {...register('barcode')}
               className="input-base font-mono text-sm"
               placeholder="Ej: 7750000000000"
             />
          </div>

          <div>
            <label htmlFor="sku" className="block text-sm font-semibold text-[var(--text-primary)] mb-1.5">
              SKU
            </label>
            <input
               id="sku"
               type="text"
               {...register('sku')}
               className="input-base font-mono text-sm"
               placeholder="Código interno"
             />
          </div>
        </div>

        <div>
          <label htmlFor="description" className="block text-sm font-semibold text-[var(--text-primary)] mb-1.5">
            Descripción
          </label>
          <textarea
            id="description"
            rows={3}
            {...register('description')}
            className="input-base resize-none"
            placeholder="Descripción detallada del producto..."
          />
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label htmlFor="costPrice" className="block text-sm font-semibold text-[var(--text-primary)] mb-1.5">
              Precio de Costo (S/) *
            </label>
             <input
               id="costPrice"
               type="number"
               step="0.01"
               {...register('costPrice')}
               className="input-base tabular-data"
               placeholder="0.00"
             />
            {errors.costPrice && (
              <p className="mt-1.5 text-sm font-medium text-red-500">{errors.costPrice.message}</p>
            )}
          </div>

          <div>
            <label htmlFor="salePrice" className="block text-sm font-semibold text-[var(--text-primary)] mb-1.5">
              Precio de Venta (S/) *
            </label>
             <input
               id="salePrice"
               type="number"
               step="0.01"
               {...register('salePrice')}
               className="input-base tabular-data"
               placeholder="0.00"
             />
            {errors.salePrice && (
              <p className="mt-1.5 text-sm font-medium text-red-500">{errors.salePrice.message}</p>
            )}
          </div>
        </div>

        <div>
          <label htmlFor="categoryId" className="block text-sm font-semibold text-[var(--text-primary)] mb-1.5">
            Categoría
          </label>
          <select
            id="categoryId"
            {...register('categoryId')}
            className="input-base"
          >
            <option value="">Sin categoría</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </div>

        {/* Stock Threshold Section */}
        <div className="pt-6 border-t border-[var(--border-light)]">
          <h3 className="text-lg font-semibold text-[var(--text-primary)] mb-1">
            Umbrales de Stock
          </h3>
          <p className="text-sm text-[var(--text-secondary)] mb-4">
            Define los niveles de alerta para el inventario. El sistema notificará cuando el stock alcance estos umbrales.
          </p>
          
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div>
              <label htmlFor="minStockInfo" className="block text-sm font-semibold text-[var(--text-primary)] mb-1.5">
                <span className="inline-flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-blue-500"></span>
                  Informativo
                </span>
              </label>
              <input
                id="minStockInfo"
                type="number"
                min="0"
                {...register('minStockInfo')}
                className="input-base tabular-data"
                placeholder="0"
              />
              <p className="mt-1 text-xs text-[var(--text-secondary)]">Stock bajo</p>
            </div>

            <div>
              <label htmlFor="minStockWarning" className="block text-sm font-semibold text-[var(--text-primary)] mb-1.5">
                <span className="inline-flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-amber-500"></span>
                  Advertencia
                </span>
              </label>
              <input
                id="minStockWarning"
                type="number"
                min="0"
                {...register('minStockWarning')}
                className="input-base tabular-data"
                placeholder="0"
              />
              <p className="mt-1 text-xs text-[var(--text-secondary)]">Reordenar pronto</p>
            </div>

            <div>
              <label htmlFor="minStockCritical" className="block text-sm font-semibold text-[var(--text-primary)] mb-1.5">
                <span className="inline-flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-red-500"></span>
                  Crítico
                </span>
              </label>
              <input
                id="minStockCritical"
                type="number"
                min="0"
                {...register('minStockCritical')}
                className="input-base tabular-data"
                placeholder="0"
              />
              <p className="mt-1 text-xs text-[var(--text-secondary)]">Reorden urgente</p>
              {errors.minStockCritical && (
                <p className="mt-1.5 text-sm font-medium text-red-500">{errors.minStockCritical.message}</p>
              )}
            </div>
          </div>
        </div>

         <div className="flex justify-end space-x-3 pt-6 mt-6 border-t border-[var(--border-light)]">
           <button
             type="button"
             onClick={() => navigate('/catalog/products')}
             className="btn-secondary"
           >
             Cancelar
           </button>
           <button
             type="submit"
             disabled={isSaving}
             className="btn-primary"
           >
             {isSaving ? 'Guardando...' : isEditing ? 'Actualizar Producto' : 'Crear Producto'}
           </button>
         </div>
      </form>
    </div>
  );
}
