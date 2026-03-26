import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { categoryApi } from '../../api/catalog';
import type { Category } from '../../types';
import { CategoryTree } from '../../components/catalog/CategoryTree';
import { useAuthStore } from '../../stores/authStore';
import type { AxiosError } from 'axios';

const categorySchema = z.object({
  name: z.string().min(1, 'El nombre es requerido'),
  description: z.string().optional(),
  parentCategoryId: z.string().optional(),
});

type CategoryFormData = z.infer<typeof categorySchema>;

export function CategoryPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [flatCategories, setFlatCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [editingCategory, setEditingCategory] = useState<Category | null>(null);
  const [showForm, setShowForm] = useState(false);
  const { hasRole } = useAuthStore();

  const canEdit = hasRole(['ADMIN', 'WAREHOUSE']);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CategoryFormData>({
    resolver: zodResolver(categorySchema),
  });

  useEffect(() => {
    loadCategories();
  }, []);

  const loadCategories = async () => {
    setIsLoading(true);
    try {
      const [tree, flat] = await Promise.all([
        categoryApi.getTree(),
        categoryApi.getAll(),
      ]);
      setCategories(tree);
      setFlatCategories(flat);
    } catch (err) {
      setError('Error al cargar las categorías');
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleNew = () => {
    setEditingCategory(null);
    reset({ name: '', description: '', parentCategoryId: '' });
    setShowForm(true);
  };

  const handleEdit = (category: Category) => {
    setEditingCategory(category);
    reset({
      name: category.name,
      description: category.description || '',
      parentCategoryId: category.parentCategoryId?.toString() || '',
    });
    setShowForm(true);
  };

  const handleDelete = async (category: Category) => {
    if (!window.confirm(`¿Está seguro de desactivar la categoría "${category.name}"?`)) {
      return;
    }
    try {
      await categoryApi.delete(category.id);
      loadCategories();
    } catch (err) {
      setError('Error al desactivar la categoría');
      console.error(err);
    }
  };

  const handleCancel = () => {
    setShowForm(false);
    setEditingCategory(null);
    reset();
  };

  const onSubmit = async (data: CategoryFormData) => {
    setIsSaving(true);
    setError(null);

    const categoryData = {
      name: data.name,
      description: data.description || undefined,
      parentCategoryId: data.parentCategoryId ? parseInt(data.parentCategoryId) : undefined,
    };

    try {
      if (editingCategory) {
        await categoryApi.update(editingCategory.id, categoryData);
      } else {
        await categoryApi.create(categoryData);
      }
      setShowForm(false);
      setEditingCategory(null);
      reset();
      loadCategories();
    } catch (err) {
      const axiosError = err as AxiosError<{ message: string }>;
      setError(axiosError.response?.data?.message || 'Error al guardar la categoría');
    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="text-[var(--text-secondary)]">Cargando categorías...</div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto grid grid-cols-1 lg:grid-cols-2 gap-8">
      {/* Category Tree */}
      <div>
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
          <h1 className="text-2xl font-bold text-[var(--text-primary)] tracking-tight">Categorías</h1>
          {canEdit && (
            <button
               onClick={handleNew}
               className="btn-primary"
             >
               Nueva Categoría
             </button>
          )}
        </div>

        {error && !showForm && (
          <div className="p-4 rounded-xl mb-6 bg-red-50 border border-red-200 text-red-600 shadow-sm">
            {error}
          </div>
        )}

        <div className="card p-6">
          <CategoryTree
            categories={categories}
            onEdit={canEdit ? handleEdit : undefined}
            onDelete={canEdit ? handleDelete : undefined}
          />
        </div>
      </div>

      {/* Category Form */}
      {showForm && canEdit && (
        <div className="lg:sticky lg:top-6">
          <h2 className="text-xl font-bold text-[var(--text-primary)] tracking-tight mb-6">
            {editingCategory ? 'Editar Categoría' : 'Nueva Categoría'}
          </h2>

          {error && (
            <div className="p-4 rounded-xl mb-6 bg-red-50 border border-red-200 text-red-600 shadow-sm">
              {error}
            </div>
          )}

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
                 placeholder="Ej. Accesorios"
               />
              {errors.name && (
                <p className="mt-1.5 text-sm font-medium text-red-500">{errors.name.message}</p>
              )}
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
                placeholder="Descripción de la categoría"
              />
            </div>

            {/* Categoría Padre - Oculto para evitar errores de FK
                Las categorías se crean como raíz por defecto */}
            <input type="hidden" {...register('parentCategoryId')} value="" />

             <div className="flex justify-end space-x-3 pt-6 mt-6 border-t border-[var(--border-light)]">
               <button
                 type="button"
                 onClick={handleCancel}
                 className="btn-secondary"
               >
                 Cancelar
               </button>
               <button
                 type="submit"
                 disabled={isSaving}
                 className="btn-primary"
               >
                 {isSaving ? 'Guardando...' : editingCategory ? 'Actualizar' : 'Crear'}
               </button>
             </div>
          </form>
        </div>
      )}
    </div>
  );
}
