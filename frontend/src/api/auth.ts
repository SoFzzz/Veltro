import apiClient from './client';
import type { LoginRequest, LoginResponse, RegisterRequest, User } from '../types';

export const authApi = {
  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    const response = await apiClient.post<LoginResponse>('/auth/login', credentials);
    return response.data;
  },

  register: async (data: RegisterRequest): Promise<{ success: boolean; message: string }> => {
    const response = await apiClient.post<{ success: boolean; message: string }>('/auth/register', data);
    return response.data;
  },

  createWorker: async (data: RegisterRequest): Promise<{ success: boolean; message: string; username: string; role: string }> => {
    const response = await apiClient.post<{ success: boolean; message: string; username: string; role: string }>('/auth/workers', data);
    return response.data;
  },

  logout: async (): Promise<void> => {
    await apiClient.post('/auth/logout');
  },

  getCurrentUser: async (): Promise<User> => {
    // Backend has no GET /auth/me endpoint.
    // User info is extracted from the JWT token at login time.
    // This method is kept for interface compatibility but should not be called.
    throw new Error('GET /auth/me is not implemented on the backend. Use login response data instead.');
  },

  changePassword: async (currentPassword: string, newPassword: string): Promise<void> => {
    await apiClient.put('/auth/change-password', { currentPassword, newPassword });
  },
};
