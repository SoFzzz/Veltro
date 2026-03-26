import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import type { Supplier, CreateSupplierRequest } from '../../api/purchasing';
import { getSuppliers, createSupplier, updateSupplier, deleteSupplier } from '../../api/purchasing';

// Validation schema matching backend CreateSupplierRequest
const supplierSchema = z.object({
  name: z.string().min(1, 'El nombre es requerido').max(200, 'Máximo 200 caracteres'),
  taxId: z.string().min(1, 'El RUC/Tax ID es requerido').max(50, 'Máximo 50 caracteres'),
  email: z.string().email('Email inválido').max(200).optional().or(z.literal('')),
  phone: z.string().max(20, 'Máximo 20 caracteres').optional().or(z.literal('')),
  address: z.string().max(500, 'Máximo 500 caracteres').optional().or(z.literal('')),
  notes: z.string().max(500, 'Máximo 500 caracteres').optional().or(z.literal('')),
});

type SupplierFormData = z.infer<typeof supplierSchema>;

export const SupplierPage: React.FC = () => {
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingSupplier, setEditingSupplier] = useState<Supplier | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<SupplierFormData>({
    resolver: zodResolver(supplierSchema),
  });

  const fetchSuppliers = async () => {
    setIsLoading(true);
    try {
      const response = await getSuppliers();
      setSuppliers(response.data);
    } catch (err) {
      console.error('Failed to fetch suppliers:', err);
      setError('Error al cargar proveedores');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchSuppliers();
  }, []);

  const handleOpenForm = (supplier?: Supplier) => {
    if (supplier) {
      setEditingSupplier(supplier);
      reset({
        name: supplier.name,
        taxId: supplier.taxId,
        email: supplier.email || '',
        phone: supplier.phone || '',
        address: supplier.address || '',
        notes: supplier.notes || '',
      });
    } else {
      setEditingSupplier(null);
      reset({
        name: '',
        taxId: '',
        email: '',
        phone: '',
        address: '',
        notes: '',
      });
    }
    setShowForm(true);
    setError(null);
  };

  const handleCloseForm = () => {
    setShowForm(false);
    setEditingSupplier(null);
    reset();
    setError(null);
  };

  const onSubmit = async (data: SupplierFormData) => {
    setIsSaving(true);
    setError(null);

    const supplierData: CreateSupplierRequest = {
      name: data.name,
      taxId: data.taxId,
      email: data.email || undefined,
      phone: data.phone || undefined,
      address: data.address || undefined,
      notes: data.notes || undefined,
    };

    try {
      if (editingSupplier) {
        await updateSupplier(editingSupplier.id, supplierData);
        setSuccessMessage('Proveedor actualizado correctamente');
      } else {
        await createSupplier(supplierData);
        setSuccessMessage('Proveedor creado correctamente');
      }
      handleCloseForm();
      fetchSuppliers();
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : 'Error al guardar el proveedor';
      setError(errorMessage);
    } finally {
      setIsSaving(false);
    }
  };

  const handleDelete = async (supplier: Supplier) => {
    if (!confirm(`¿Eliminar el proveedor "${supplier.name}"?`)) return;

    try {
      await deleteSupplier(supplier.id);
      setSuccessMessage('Proveedor eliminado correctamente');
      fetchSuppliers();
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err) {
      console.error('Failed to delete supplier:', err);
      setError('Error al eliminar el proveedor');
    }
  };

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-[var(--text-primary)] tracking-tight">
            Proveedores
          </h1>
          <p className="mt-1 text-[var(--text-secondary)] text-sm">
            Gestiona los proveedores para órdenes de compra
          </p>
        </div>
        <button
          onClick={() => handleOpenForm()}
          className="btn-primary"
        >
          + Nuevo Proveedor
        </button>
      </div>

      {/* Success Message */}
      {successMessage && (
        <div className="p-4 rounded-xl bg-green-50 border border-green-200 text-green-600 shadow-sm">
          {successMessage}
        </div>
      )}

      {/* Error Message */}
      {error && !showForm && (
        <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-red-600 shadow-sm">
          {error}
        </div>
      )}

      {/* Form Modal */}
      {showForm && (
        <div className="card p-6 mb-6">
          <h2 className="text-lg font-semibold text-[var(--text-primary)] mb-4">
            {editingSupplier ? 'Editar Proveedor' : 'Nuevo Proveedor'}
          </h2>

          {error && (
            <div className="p-4 rounded-xl mb-4 bg-red-50 border border-red-200 text-red-600 shadow-sm">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-semibold text-[var(--text-primary)] mb-1.5">
                  Nombre de Empresa *
                </label>
                <input
                  type="text"
                  {...register('name')}
                  className="input-base"
                  placeholder="Ej. Distribuidora ABC S.A.C."
                />
                {errors.name && (
                  <p className="mt-1 text-sm text-red-500">{errors.name.message}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-semibold text-[var(--text-primary)] mb-1.5">
                  RUC / Tax ID *
                </label>
                <input
                  type="text"
                  {...register('taxId')}
                  className="input-base font-mono"
                  placeholder="Ej. 20123456789"
                  disabled={!!editingSupplier}
                />
                {errors.taxId && (
                  <p className="mt-1 text-sm text-red-500">{errors.taxId.message}</p>
                )}
                {editingSupplier && (
                  <p className="mt-1 text-xs text-[var(--text-secondary)]">
                    El Tax ID no puede ser modificado
                  </p>
                )}
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-semibold text-[var(--text-primary)] mb-1.5">
                  Email
                </label>
                <input
                  type="email"
                  {...register('email')}
                  className="input-base"
                  placeholder="contacto@empresa.com"
                />
                {errors.email && (
                  <p className="mt-1 text-sm text-red-500">{errors.email.message}</p>
                )}
              </div>

              <div>
                <label className="block text-sm font-semibold text-[var(--text-primary)] mb-1.5">
                  Teléfono
                </label>
                <input
                  type="text"
                  {...register('phone')}
                  className="input-base"
                  placeholder="+51 999 888 777"
                />
                {errors.phone && (
                  <p className="mt-1 text-sm text-red-500">{errors.phone.message}</p>
                )}
              </div>
            </div>

            <div>
              <label className="block text-sm font-semibold text-[var(--text-primary)] mb-1.5">
                Dirección
              </label>
              <input
                type="text"
                {...register('address')}
                className="input-base"
                placeholder="Av. Principal 123, Lima"
              />
              {errors.address && (
                <p className="mt-1 text-sm text-red-500">{errors.address.message}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-semibold text-[var(--text-primary)] mb-1.5">
                Notas
              </label>
              <textarea
                {...register('notes')}
                rows={2}
                className="input-base resize-none"
                placeholder="Notas adicionales sobre el proveedor..."
              />
              {errors.notes && (
                <p className="mt-1 text-sm text-red-500">{errors.notes.message}</p>
              )}
            </div>

            <div className="flex justify-end space-x-3 pt-4 border-t border-[var(--border-light)]">
              <button
                type="button"
                onClick={handleCloseForm}
                className="btn-secondary"
              >
                Cancelar
              </button>
              <button
                type="submit"
                disabled={isSaving}
                className="btn-primary"
              >
                {isSaving ? 'Guardando...' : editingSupplier ? 'Actualizar' : 'Crear'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Suppliers Table */}
      <div className="card overflow-hidden">
        {isLoading ? (
          <div className="p-8 text-center text-[var(--text-secondary)]">
            Cargando proveedores...
          </div>
        ) : suppliers.length === 0 ? (
          <div className="p-8 text-center text-[var(--text-secondary)]">
            No hay proveedores registrados
          </div>
        ) : (
          <table className="w-full">
            <thead className="bg-[var(--surface-secondary)] border-b border-[var(--border-light)]">
              <tr>
                <th className="px-4 py-3 text-left text-sm font-semibold text-[var(--text-primary)]">
                  Empresa
                </th>
                <th className="px-4 py-3 text-left text-sm font-semibold text-[var(--text-primary)]">
                  RUC
                </th>
                <th className="px-4 py-3 text-left text-sm font-semibold text-[var(--text-primary)]">
                  Contacto
                </th>
                <th className="px-4 py-3 text-right text-sm font-semibold text-[var(--text-primary)]">
                  Acciones
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[var(--border-light)]">
              {suppliers.map((supplier) => (
                <tr key={supplier.id} className="hover:bg-[var(--surface-secondary)] transition-colors">
                  <td className="px-4 py-3">
                    <div className="font-medium text-[var(--text-primary)]">
                      {supplier.name}
                    </div>
                    {supplier.address && (
                      <div className="text-sm text-[var(--text-secondary)] truncate max-w-xs">
                        {supplier.address}
                      </div>
                    )}
                  </td>
                  <td className="px-4 py-3 font-mono text-sm text-[var(--text-secondary)]">
                    {supplier.taxId}
                  </td>
                  <td className="px-4 py-3">
                    {supplier.email && (
                      <div className="text-sm text-[var(--text-primary)]">{supplier.email}</div>
                    )}
                    {supplier.phone && (
                      <div className="text-sm text-[var(--text-secondary)]">{supplier.phone}</div>
                    )}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => handleOpenForm(supplier)}
                      className="text-sm text-[var(--primary)] hover:underline mr-3"
                    >
                      Editar
                    </button>
                    <button
                      onClick={() => handleDelete(supplier)}
                      className="text-sm text-red-500 hover:underline"
                    >
                      Eliminar
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};
