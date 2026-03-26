import { useAuthStore } from '../stores/authStore';
import { authApi } from '../api/auth';
import type { LoginRequest } from '../types';

export const useAuth = () => {
  const {
    user,
    accessToken,
    isAuthenticated,
    setAuth,
    logout: logoutStore,
    hasRole,
  } = useAuthStore();

  const login = async (credentials: LoginRequest) => {
    const response = await authApi.login(credentials);
    const user = {
      id: 0,
      username: response.username,
      email: '',
      role: response.role,
      businessId: response.businessId,
    };
    setAuth(user, response.accessToken, response.refreshToken);
    return response;
  };

  const logout = () => {
    logoutStore();
  };

  return {
    user,
    accessToken,
    isAuthenticated,
    login,
    logout,
    hasRole,
  };
};
