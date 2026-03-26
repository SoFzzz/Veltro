import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { productApi } from '../../api/catalog';
import type { Product, PageResponse } from '../../types';
import { useAuthStore } from '../../stores/authStore';

export function ProductListPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const { hasRole } = useAuthStore();

  const canEdit = hasRole(['ADMIN', 'WAREHOUSE']);

  useEffect(() => {
    loadProducts();
  }, [page]);

  const loadProducts = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response: PageResponse<Product> = await productApi.getAll(page, 10);
      setProducts(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err) {
      setError('Error al cargar los productos');
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('¿Está seguro de desactivar este producto?')) {
      return;
    }
    try {
      await productApi.delete(id);
      loadProducts();
    } catch (err) {
      setError('Error al desactivar el producto');
      console.error(err);
    }
  };

  const formatPrice = (price: string): string => {
    return new Intl.NumberFormat('es-PE', {
      style: 'currency',
      currency: 'PEN',
    }).format(parseFloat(price));
  };

  if (isLoading && products.length === 0) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="text-[var(--text-secondary)]">Cargando productos...</div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-[var(--text-primary)] tracking-tight">Productos</h1>
          <p className="mt-1 text-[var(--text-secondary)] text-sm">
            {totalElements} producto{totalElements !== 1 ? 's' : ''} en total
          </p>
        </div>
        {canEdit && (
          <Link
            to="/catalog/products/new"
            className="btn-primary"
          >
            Nuevo Producto
          </Link>
        )}
      </div>

      {error && (
        <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-red-600 shadow-sm">
          {error}
        </div>
      )}

      <div className="card overflow-x-auto">
        <table className="table-responsive w-full">
          <thead>
            <tr>
              <th>Producto</th>
              <th>Código de Barras</th>
              <th>Categoría</th>
              <th className="text-right">Precio Costo</th>
              <th className="text-right">Precio Venta</th>
              <th className="text-center">Estado</th>
              {canEdit && <th className="text-right">Acciones</th>}
            </tr>
          </thead>
          <tbody>
            {products.map((product) => (
              <tr key={product.id}>
                <td>
                  <div className="text-sm font-semibold text-[var(--text-primary)]">
                    {product.name}
                  </div>
                  {product.sku && (
                    <div className="text-xs text-[var(--text-tertiary)] mt-0.5 font-mono">SKU: {product.sku}</div>
                  )}
                </td>
                <td className="text-sm text-[var(--text-secondary)] font-mono">
                  {product.barcode || '-'}
                </td>
                <td className="text-sm text-[var(--text-secondary)]">
                  <span className="inline-flex items-center px-2 py-0.5 rounded-md text-xs font-medium bg-slate-100 text-slate-700">
                    {product.categoryName || 'Sin categoría'}
                  </span>
                </td>
                <td className="text-sm text-right tabular-data font-medium text-[var(--text-primary)]">
                  {formatPrice(product.costPrice)}
                </td>
                <td className="text-sm text-right tabular-data font-bold text-[var(--primary-base)]">
                  {formatPrice(product.salePrice)}
                </td>
                <td className="text-center">
                  <span
                    className={`px-2.5 py-1 inline-flex text-xs leading-5 font-semibold rounded-full ${
                      product.active 
                        ? 'bg-[var(--primary-faint)] text-[var(--primary-base)]' 
                        : 'bg-red-50 text-red-600'
                    }`}
                  >
                    {product.active ? 'Activo' : 'Inactivo'}
                  </span>
                </td>
                {canEdit && (
                  <td className="text-right text-sm font-medium space-x-3">
                    <Link
                      to={`/catalog/products/${product.id}/edit`}
                      className="text-[var(--primary-base)] hover:text-[var(--primary-dark)] transition-colors"
                    >
                      Editar
                    </Link>
                    <button
                       onClick={() => handleDelete(product.id)}
                       className="text-red-500 hover:text-red-700 transition-colors"
                     >
                       Desactivar
                     </button>
                  </td>
                )}
              </tr>
            ))}
            {products.length === 0 && (
              <tr>
                <td colSpan={canEdit ? 7 : 6} className="py-12 text-center text-[var(--text-tertiary)]">
                  No hay productos registrados
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex justify-between items-center mt-6 card p-4">
          <div className="text-sm text-[var(--text-secondary)] font-medium">
            Mostrando página <span className="text-[var(--text-primary)] font-bold">{page + 1}</span> de <span className="text-[var(--text-primary)] font-bold">{totalPages}</span>
          </div>
          <div className="flex space-x-2">
            <button
               onClick={() => setPage((p) => Math.max(0, p - 1))}
               disabled={page === 0}
               className="btn-secondary px-3 py-1.5 text-sm"
             >
               Anterior
             </button>
             <button
               onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
               disabled={page >= totalPages - 1}
               className="btn-secondary px-3 py-1.5 text-sm"
             >
               Siguiente
             </button>
          </div>
        </div>
      )}
    </div>
  );
}
