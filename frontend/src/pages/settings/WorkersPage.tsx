import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { authApi } from '../../api/auth';
import type { AxiosError } from 'axios';
import type { ApiError } from '../../types';

const workerSchema = z.object({
  username: z.string()
    .min(3, 'El usuario debe tener al menos 3 caracteres')
    .max(50, 'El usuario no puede exceder 50 caracteres'),
  email: z.string()
    .email('Ingrese un email válido'),
  password: z.string()
    .min(6, 'La contraseña debe tener al menos 6 caracteres')
    .max(100, 'La contraseña no puede exceder 100 caracteres'),
  role: z.enum(['WAREHOUSE', 'CASHIER'], {
    required_error: 'Seleccione un rol',
  }),
});

type WorkerFormData = z.infer<typeof workerSchema>;

interface CreatedWorker {
  username: string;
  role: string;
  createdAt: string;
}

export function WorkersPage() {
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [createdWorkers, setCreatedWorkers] = useState<CreatedWorker[]>([]);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<WorkerFormData>({
    resolver: zodResolver(workerSchema),
    defaultValues: {
      role: 'CASHIER',
    },
  });

  const onSubmit = async (data: WorkerFormData) => {
    setError(null);
    setSuccess(null);
    setIsLoading(true);

    try {
      const result = await authApi.createWorker({
        username: data.username,
        email: data.email,
        password: data.password,
        role: data.role,
      });

      setSuccess(`Usuario "${result.username}" creado exitosamente como ${getRoleLabel(result.role)}`);
      setCreatedWorkers(prev => [{
        username: result.username,
        role: result.role,
        createdAt: new Date().toLocaleString('es-ES'),
      }, ...prev]);
      reset();
      setShowForm(false);
    } catch (err) {
      const axiosError = err as AxiosError<ApiError>;
      if (axiosError.response?.status === 400) {
        setError(axiosError.response.data?.message || 'El usuario ya existe o datos inválidos');
      } else if (axiosError.response?.status === 403) {
        setError('No tienes permisos para crear usuarios');
      } else if (axiosError.response?.data?.message) {
        setError(axiosError.response.data.message);
      } else {
        setError('Error al crear usuario. Intente nuevamente.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const getRoleLabel = (role: string): string => {
    switch (role) {
      case 'CASHIER': return 'Cajero';
      case 'WAREHOUSE': return 'Almacén';
      case 'ADMIN': return 'Administrador';
      default: return role;
    }
  };

  const getRoleBadgeStyles = (role: string) => {
    switch (role) {
      case 'CASHIER': return 'bg-blue-100 text-blue-800';
      case 'WAREHOUSE': return 'bg-amber-100 text-amber-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Gestión de Empleados</h1>
          <p className="text-sm text-gray-500 mt-1">
            Crea y administra los usuarios de tu negocio
          </p>
        </div>
        <button
          onClick={() => { setShowForm(!showForm); setError(null); setSuccess(null); }}
          className="inline-flex items-center px-4 py-2.5 rounded-lg text-sm font-semibold text-white shadow-sm transition-all duration-200"
          style={{ backgroundColor: 'var(--primary-base)' }}
          onMouseEnter={e => e.currentTarget.style.backgroundColor = '#027A4B'}
          onMouseLeave={e => e.currentTarget.style.backgroundColor = 'var(--primary-base)'}
        >
          {showForm ? (
            <>
              <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
              Cancelar
            </>
          ) : (
            <>
              <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
              </svg>
              Nuevo Empleado
            </>
          )}
        </button>
      </div>

      {/* Success Message */}
      {success && (
        <div className="p-4 rounded-lg bg-green-50 border border-green-200 flex items-start gap-3">
          <svg className="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
          </svg>
          <p className="text-sm text-green-700 font-medium">{success}</p>
        </div>
      )}

      {/* Error Message */}
      {error && (
        <div className="p-4 rounded-lg bg-red-50 border border-red-200 flex items-start gap-3">
          <svg className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <p className="text-sm text-red-600 font-medium">{error}</p>
        </div>
      )}

      {/* Create Worker Form */}
      {showForm && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 p-6">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">Crear Nuevo Empleado</h2>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* Username */}
              <div className="flex flex-col gap-1.5">
                <label htmlFor="worker-username" className="text-sm font-semibold text-gray-700">
                  Nombre de Usuario
                </label>
                <input
                  id="worker-username"
                  type="text"
                  autoComplete="off"
                  {...register('username')}
                  className="w-full px-4 py-2.5 rounded-lg border border-gray-200 bg-gray-50 text-gray-900 outline-none focus:bg-white focus:ring-2 focus:ring-[#038E57]/20 focus:border-[#038E57] transition-all duration-200 text-sm"
                  placeholder="nombre_usuario"
                />
                {errors.username && (
                  <p className="text-xs font-medium text-red-500">{errors.username.message}</p>
                )}
              </div>

              {/* Email */}
              <div className="flex flex-col gap-1.5">
                <label htmlFor="worker-email" className="text-sm font-semibold text-gray-700">
                  Correo Electrónico
                </label>
                <input
                  id="worker-email"
                  type="email"
                  autoComplete="off"
                  {...register('email')}
                  className="w-full px-4 py-2.5 rounded-lg border border-gray-200 bg-gray-50 text-gray-900 outline-none focus:bg-white focus:ring-2 focus:ring-[#038E57]/20 focus:border-[#038E57] transition-all duration-200 text-sm"
                  placeholder="usuario@email.com"
                />
                {errors.email && (
                  <p className="text-xs font-medium text-red-500">{errors.email.message}</p>
                )}
              </div>

              {/* Password */}
              <div className="flex flex-col gap-1.5">
                <label htmlFor="worker-password" className="text-sm font-semibold text-gray-700">
                  Contraseña
                </label>
                <input
                  id="worker-password"
                  type="password"
                  autoComplete="new-password"
                  {...register('password')}
                  className="w-full px-4 py-2.5 rounded-lg border border-gray-200 bg-gray-50 text-gray-900 outline-none focus:bg-white focus:ring-2 focus:ring-[#038E57]/20 focus:border-[#038E57] transition-all duration-200 text-sm"
                  placeholder="min. 6 caracteres"
                />
                {errors.password && (
                  <p className="text-xs font-medium text-red-500">{errors.password.message}</p>
                )}
              </div>

              {/* Role */}
              <div className="flex flex-col gap-1.5">
                <label htmlFor="worker-role" className="text-sm font-semibold text-gray-700">
                  Rol
                </label>
                <select
                  id="worker-role"
                  {...register('role')}
                  className="w-full px-4 py-2.5 rounded-lg border border-gray-200 bg-gray-50 text-gray-900 outline-none focus:bg-white focus:ring-2 focus:ring-[#038E57]/20 focus:border-[#038E57] transition-all duration-200 text-sm"
                >
                  <option value="CASHIER">Cajero (Punto de venta)</option>
                  <option value="WAREHOUSE">Almacén (Inventario y compras)</option>
                </select>
                {errors.role && (
                  <p className="text-xs font-medium text-red-500">{errors.role.message}</p>
                )}
              </div>
            </div>

            <div className="flex justify-end pt-2">
              <button
                type="submit"
                disabled={isLoading}
                className="inline-flex items-center px-5 py-2.5 rounded-lg text-sm font-semibold text-white shadow-sm transition-all duration-200 disabled:opacity-70 disabled:cursor-not-allowed"
                style={{ backgroundColor: 'var(--primary-base)' }}
              >
                {isLoading && (
                  <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                  </svg>
                )}
                {isLoading ? 'Creando...' : 'Crear Empleado'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Recently Created Workers (session-only) */}
      {createdWorkers.length > 0 && (
        <div className="bg-white rounded-xl shadow-sm border border-gray-200 overflow-hidden">
          <div className="px-6 py-4 border-b border-gray-100">
            <h2 className="text-lg font-semibold text-gray-900">Empleados Creados en esta Sesión</h2>
          </div>
          <div className="divide-y divide-gray-100">
            {createdWorkers.map((worker, idx) => (
              <div key={idx} className="px-6 py-4 flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-full bg-gray-100 flex items-center justify-center text-sm font-semibold text-gray-600">
                    {worker.username.charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <p className="text-sm font-medium text-gray-900">{worker.username}</p>
                    <p className="text-xs text-gray-500">{worker.createdAt}</p>
                  </div>
                </div>
                <span className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold ${getRoleBadgeStyles(worker.role)}`}>
                  {getRoleLabel(worker.role)}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Info Card */}
      <div className="bg-blue-50 rounded-xl border border-blue-100 p-5">
        <div className="flex gap-3">
          <svg className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <div className="text-sm text-blue-800">
            <p className="font-semibold mb-1">Acerca de los roles</p>
            <ul className="space-y-1 text-blue-700">
              <li><strong>Cajero</strong> — Acceso al terminal POS, escaneo de productos y consulta de stock.</li>
              <li><strong>Almacén</strong> — Gestión de productos, inventario, alertas y órdenes de compra.</li>
            </ul>
            <p className="mt-2 text-xs text-blue-600">
              Los empleados comparten los mismos datos de tu negocio (productos, inventario, ventas) pero con acceso limitado según su rol.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
