import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { authApi } from '../../api/auth';
import { useAuthStore } from '../../stores/authStore';
import type { AxiosError } from 'axios';
import type { ApiError } from '../../types';

const loginSchema = z.object({
  username: z.string().min(1, 'El usuario es requerido'),
  password: z.string().min(1, 'La contraseña es requerida'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { setAuth } = useAuthStore();
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const from = location.state?.from?.pathname || '/';

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginFormData) => {
    setError(null);
    setIsLoading(true);

    try {
      const response = await authApi.login(data);
      const user = {
        id: 0,
        username: response.username,
        email: '',
        role: response.role,
        businessId: response.businessId,
      };
      setAuth(user, response.accessToken, response.refreshToken);

      // Role-based redirect
      const redirectPath = getRedirectPathByRole(response.role);
      navigate(from !== '/' ? from : redirectPath, { replace: true });
    } catch (err) {
      const axiosError = err as AxiosError<ApiError>;
      if (axiosError.response?.status === 401) {
        setError('Usuario o contraseña incorrectos');
      } else if (axiosError.response?.data?.message) {
        setError(axiosError.response.data.message);
      } else {
        setError('Error al iniciar sesión. Intente nuevamente.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  const getRedirectPathByRole = (role: string): string => {
    switch (role) {
      case 'ADMIN':
        return '/dashboard';
      case 'CASHIER':
        return '/pos';
      case 'WAREHOUSE':
        return '/inventory';
      default:
        return '/';
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#FEFAF1] p-4">
      {/* 2. La Tarjeta (Card) */}
      <div className="w-full max-w-md bg-white rounded-2xl shadow-xl border border-gray-100 p-8 sm:p-10">
        
        {/* 5. Espaciado del Logo/Título */}
        <div className="mb-8 flex flex-col items-center gap-2">
          <div className="w-12 h-12 bg-[#038E57] rounded-xl flex items-center justify-center shadow-md mb-2">
             <span className="text-white font-bold text-xl">V</span>
          </div>
          <h1 className="text-3xl font-bold text-gray-900 tracking-tight">Veltro</h1>
          <p className="text-sm font-medium text-gray-500">Sistema de Gestión Comercial</p>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          {/* 3. Inputs y Formularios - Usuario */}
          <div className="flex flex-col gap-1.5 mb-4">
            <label 
              htmlFor="username" 
              className="text-sm font-semibold text-gray-700"
            >
              Usuario
            </label>
            <input
              id="username"
              type="text"
              autoComplete="username"
              {...register('username')}
              className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-gray-50 text-gray-900 outline-none focus:bg-white focus:ring-2 focus:ring-[#038E57]/20 focus:border-[#038E57] transition-all duration-200"
              placeholder="tu@usuario.com"
            />
            {errors.username && (
              <p className="text-xs font-medium text-red-500 mt-1">{errors.username.message}</p>
            )}
          </div>

          {/* 3. Inputs y Formularios - Contraseña */}
          <div className="flex flex-col gap-1.5 mb-4">
            <label 
              htmlFor="password" 
              className="text-sm font-semibold text-gray-700"
            >
              Contraseña
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              {...register('password')}
              className="w-full px-4 py-3 rounded-lg border border-gray-200 bg-gray-50 text-gray-900 outline-none focus:bg-white focus:ring-2 focus:ring-[#038E57]/20 focus:border-[#038E57] transition-all duration-200"
              placeholder="••••••••"
            />
            {errors.password && (
              <p className="text-xs font-medium text-red-500 mt-1">{errors.password.message}</p>
            )}
          </div>

          {/* Error Message */}
          {error && (
            <div className="mb-4 p-3 rounded-lg bg-red-50 border border-red-200">
              <p className="text-sm text-red-600 font-medium text-center">{error}</p>
            </div>
          )}

          {/* 4. El Botón "Iniciar Sesión" */}
          <button
            type="submit"
            disabled={isLoading}
            className="w-full mt-6 py-3 px-4 bg-[#038E57] hover:bg-[#027A4B] text-white font-bold rounded-lg shadow-md hover:shadow-lg transition-all duration-200 flex justify-center items-center outline-none border-none disabled:opacity-70 disabled:cursor-not-allowed"
          >
            {isLoading ? (
              <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
            ) : null}
            {isLoading ? 'Iniciando sesión...' : 'Iniciar Sesión'}
          </button>
        </form>

        <div className="mt-8 pt-6 border-t border-gray-100 text-center">
          <p className="text-sm text-gray-600">
            ¿No tienes cuenta?{' '}
            <Link to="/register" className="text-[#038E57] font-semibold hover:underline">
              Crear Cuenta
            </Link>
          </p>
          <p className="text-xs text-gray-400 font-medium mt-3">
            Acceso restringido a usuarios autorizados
          </p>
        </div>
      </div>
    </div>
  );
}
