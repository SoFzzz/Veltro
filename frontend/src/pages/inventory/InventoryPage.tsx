import { useState, useEffect } from 'react';
import { 
  getInventory, 
  recordStockEntry, 
  recordStockExit, 
  recordStockAdjustment,
  getInventoryMovements,
  type InventoryItem,
  type InventoryMovement,
  type PageResponse
} from '../../api/inventory';

type ModalType = 'entry' | 'exit' | 'adjustment' | 'history' | null;

export function InventoryPage() {
  const [inventory, setInventory] = useState<InventoryItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');
  
  // Modal state
  const [modalType, setModalType] = useState<ModalType>(null);
  const [selectedItem, setSelectedItem] = useState<InventoryItem | null>(null);
  const [modalLoading, setModalLoading] = useState(false);
  const [movements, setMovements] = useState<InventoryMovement[]>([]);
  
  // Form state
  const [quantity, setQuantity] = useState('');
  const [reason, setReason] = useState('');
  const [newStock, setNewStock] = useState('');

  useEffect(() => {
    loadInventory();
  }, [page]);

  const loadInventory = async () => {
    try {
      setLoading(true);
      const response = await getInventory(page, 20);
      const data = response.data as PageResponse<InventoryItem>;
      setInventory(data.content);
      setTotalPages(data.totalPages);
      setError(null);
    } catch (err) {
      setError('Error al cargar el inventario');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const openModal = async (type: ModalType, item: InventoryItem) => {
    setSelectedItem(item);
    setModalType(type);
    setQuantity('');
    setReason('');
    setNewStock(item.currentStock.toString());
    
    if (type === 'history') {
      try {
        setModalLoading(true);
        const response = await getInventoryMovements(item.productId);
        setMovements(response.data.content);
      } catch (err) {
        console.error('Error loading movements:', err);
      } finally {
        setModalLoading(false);
      }
    }
  };

  const closeModal = () => {
    setModalType(null);
    setSelectedItem(null);
    setMovements([]);
  };

  const handleEntry = async () => {
    if (!selectedItem || !quantity || !reason) return;
    
    try {
      setModalLoading(true);
      await recordStockEntry(selectedItem.productId, {
        quantity: parseInt(quantity),
        reason,
      });
      await loadInventory();
      closeModal();
    } catch (err) {
      setError('Error al registrar entrada');
    } finally {
      setModalLoading(false);
    }
  };

  const handleExit = async () => {
    if (!selectedItem || !quantity || !reason) return;
    
    try {
      setModalLoading(true);
      await recordStockExit(selectedItem.productId, {
        quantity: parseInt(quantity),
        reason,
      });
      await loadInventory();
      closeModal();
    } catch (err: any) {
      if (err.response?.status === 422) {
        setError('Stock insuficiente para esta salida');
      } else {
        setError('Error al registrar salida');
      }
    } finally {
      setModalLoading(false);
    }
  };

  const handleAdjustment = async () => {
    if (!selectedItem || !newStock || !reason) return;
    
    try {
      setModalLoading(true);
      await recordStockAdjustment(selectedItem.productId, {
        newStock: parseInt(newStock),
        reason,
      });
      await loadInventory();
      closeModal();
    } catch (err) {
      setError('Error al ajustar stock');
    } finally {
      setModalLoading(false);
    }
  };

  const filteredInventory = inventory.filter(item =>
    item.productName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const getStockStatus = (item: InventoryItem) => {
    if (item.currentStock <= 0) return { color: 'bg-red-100 text-red-800', text: 'Sin stock' };
    if (item.currentStock <= item.minStock) return { color: 'bg-yellow-100 text-yellow-800', text: 'Stock bajo' };
    if (item.maxStock > 0 && item.currentStock >= item.maxStock) return { color: 'bg-blue-100 text-blue-800', text: 'Stock alto' };
    return { color: 'bg-green-100 text-green-800', text: 'Normal' };
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('es-MX', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getMovementTypeLabel = (type: string) => {
    const labels: Record<string, { text: string; color: string }> = {
      ENTRY: { text: 'Entrada', color: 'text-green-600' },
      EXIT: { text: 'Salida', color: 'text-red-600' },
      ADJUSTMENT: { text: 'Ajuste', color: 'text-blue-600' },
      SALE: { text: 'Venta', color: 'text-purple-600' },
    };
    return labels[type] || { text: type, color: 'text-gray-600' };
  };

  return (
    <div className="p-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Inventario</h1>
          <p className="text-gray-600">Gestión de stock y movimientos</p>
        </div>
        
        {/* Search */}
        <div className="w-full sm:w-64">
          <input
            type="text"
            placeholder="Buscar producto..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#038E57] focus:border-transparent"
          />
        </div>
      </div>

      {/* Error */}
      {error && (
        <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg">
          <p className="text-red-600">{error}</p>
          <button onClick={() => setError(null)} className="text-sm text-red-500 underline mt-1">
            Cerrar
          </button>
        </div>
      )}

      {/* Loading */}
      {loading ? (
        <div className="flex justify-center items-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#038E57]"></div>
        </div>
      ) : (
        <>
          {/* Table */}
          <div className="bg-white rounded-lg shadow overflow-hidden">
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Producto
                    </th>
                    <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Stock Actual
                    </th>
                    <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Mín / Máx
                    </th>
                    <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Estado
                    </th>
                    <th className="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Acciones
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {filteredInventory.map((item) => {
                    const status = getStockStatus(item);
                    return (
                      <tr key={item.id} className="hover:bg-gray-50">
                        <td className="px-6 py-4">
                          <div className="text-sm font-medium text-gray-900">{item.productName}</div>
                          <div className="text-xs text-gray-500">ID: {item.productId}</div>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-center">
                          <span className="text-lg font-semibold text-gray-900">{item.currentStock}</span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-center text-sm text-gray-500">
                          {item.minStock} / {item.maxStock || '∞'}
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-center">
                          <span className={`px-2 py-1 text-xs font-medium rounded-full ${status.color}`}>
                            {status.text}
                          </span>
                        </td>
                        <td className="px-6 py-4 whitespace-nowrap text-center">
                          <div className="flex justify-center gap-2 flex-wrap">
                            <button
                              onClick={() => openModal('entry', item)}
                              className="px-2 py-1 text-xs bg-green-100 text-green-700 rounded hover:bg-green-200"
                              title="Entrada"
                            >
                              + Entrada
                            </button>
                            <button
                              onClick={() => openModal('exit', item)}
                              className="px-2 py-1 text-xs bg-red-100 text-red-700 rounded hover:bg-red-200"
                              title="Salida"
                            >
                              - Salida
                            </button>
                            <button
                              onClick={() => openModal('adjustment', item)}
                              className="px-2 py-1 text-xs bg-blue-100 text-blue-700 rounded hover:bg-blue-200"
                              title="Ajuste"
                            >
                              Ajustar
                            </button>
                            <button
                              onClick={() => openModal('history', item)}
                              className="px-2 py-1 text-xs bg-gray-100 text-gray-700 rounded hover:bg-gray-200"
                              title="Historial"
                            >
                              Historial
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {filteredInventory.length === 0 && (
              <div className="p-8 text-center text-gray-500">
                No se encontraron productos en el inventario
              </div>
            )}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex justify-center gap-2 mt-4">
              <button
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-4 py-2 border rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
              >
                Anterior
              </button>
              <span className="px-4 py-2 text-gray-600">
                Página {page + 1} de {totalPages}
              </span>
              <button
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="px-4 py-2 border rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
              >
                Siguiente
              </button>
            </div>
          )}
        </>
      )}

      {/* Modal */}
      {modalType && selectedItem && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg shadow-xl w-full max-w-md max-h-[90vh] overflow-y-auto">
            <div className="p-6">
              {/* Modal Header */}
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-lg font-semibold text-gray-900">
                  {modalType === 'entry' && 'Registrar Entrada'}
                  {modalType === 'exit' && 'Registrar Salida'}
                  {modalType === 'adjustment' && 'Ajustar Stock'}
                  {modalType === 'history' && 'Historial de Movimientos'}
                </h2>
                <button
                  onClick={closeModal}
                  className="text-gray-400 hover:text-gray-600"
                >
                  <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>

              <div className="mb-4 p-3 bg-gray-50 rounded-lg">
                <p className="font-medium text-gray-900">{selectedItem.productName}</p>
                <p className="text-sm text-gray-500">Stock actual: {selectedItem.currentStock}</p>
              </div>

              {/* Entry Form */}
              {modalType === 'entry' && (
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Cantidad</label>
                    <input
                      type="number"
                      min="1"
                      value={quantity}
                      onChange={(e) => setQuantity(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#038E57]"
                      placeholder="Cantidad a agregar"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Razón</label>
                    <input
                      type="text"
                      value={reason}
                      onChange={(e) => setReason(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#038E57]"
                      placeholder="Ej: Compra a proveedor"
                    />
                  </div>
                  <button
                    onClick={handleEntry}
                    disabled={modalLoading || !quantity || !reason}
                    className="w-full py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50"
                  >
                    {modalLoading ? 'Procesando...' : 'Registrar Entrada'}
                  </button>
                </div>
              )}

              {/* Exit Form */}
              {modalType === 'exit' && (
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Cantidad</label>
                    <input
                      type="number"
                      min="1"
                      max={selectedItem.currentStock}
                      value={quantity}
                      onChange={(e) => setQuantity(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#038E57]"
                      placeholder="Cantidad a retirar"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Razón</label>
                    <input
                      type="text"
                      value={reason}
                      onChange={(e) => setReason(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#038E57]"
                      placeholder="Ej: Merma, devolución"
                    />
                  </div>
                  <button
                    onClick={handleExit}
                    disabled={modalLoading || !quantity || !reason}
                    className="w-full py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50"
                  >
                    {modalLoading ? 'Procesando...' : 'Registrar Salida'}
                  </button>
                </div>
              )}

              {/* Adjustment Form */}
              {modalType === 'adjustment' && (
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Nuevo Stock</label>
                    <input
                      type="number"
                      min="0"
                      value={newStock}
                      onChange={(e) => setNewStock(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#038E57]"
                      placeholder="Nueva cantidad total"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Razón del ajuste</label>
                    <input
                      type="text"
                      value={reason}
                      onChange={(e) => setReason(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-[#038E57]"
                      placeholder="Ej: Conteo físico, corrección"
                    />
                  </div>
                  <button
                    onClick={handleAdjustment}
                    disabled={modalLoading || !newStock || !reason}
                    className="w-full py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
                  >
                    {modalLoading ? 'Procesando...' : 'Ajustar Stock'}
                  </button>
                </div>
              )}

              {/* History */}
              {modalType === 'history' && (
                <div>
                  {modalLoading ? (
                    <div className="flex justify-center py-8">
                      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[#038E57]"></div>
                    </div>
                  ) : movements.length === 0 ? (
                    <p className="text-center text-gray-500 py-8">No hay movimientos registrados</p>
                  ) : (
                    <div className="space-y-3 max-h-80 overflow-y-auto">
                      {movements.map((mov) => {
                        const typeInfo = getMovementTypeLabel(mov.movementType);
                        return (
                          <div key={mov.id} className="p-3 bg-gray-50 rounded-lg">
                            <div className="flex justify-between items-start">
                              <span className={`font-medium ${typeInfo.color}`}>{typeInfo.text}</span>
                              <span className="text-xs text-gray-500">{formatDate(mov.createdAt)}</span>
                            </div>
                            <div className="text-sm text-gray-600 mt-1">
                              {mov.previousStock} → {mov.newStock} ({mov.quantity > 0 ? '+' : ''}{mov.newStock - mov.previousStock})
                            </div>
                            <div className="text-xs text-gray-500 mt-1">{mov.reason}</div>
                            <div className="text-xs text-gray-400">Por: {mov.createdBy}</div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
