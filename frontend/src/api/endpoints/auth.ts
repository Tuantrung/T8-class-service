import apiClient from '../client'
import type {
  LoginRequest,
  LoginResponse,
  RegisterTenantRequest,
  UserDto,
  ApiResponse,
} from '../types'

export const authApi = {
  login: (data: LoginRequest) =>
    apiClient.post<LoginResponse>('/api/auth/login', data).then((r) => r.data),

  registerTenant: (data: RegisterTenantRequest) =>
    apiClient.post<LoginResponse>('/api/auth/register-tenant', data).then((r) => r.data),

  getMe: () =>
    apiClient.get<ApiResponse<UserDto>>('/api/auth/me').then((r) => r.data.data),

  changePassword: (currentPassword: string, newPassword: string) =>
    apiClient.post('/api/auth/change-password', { currentPassword, newPassword }),

  createUser: (email: string, password: string, fullName: string, role: string) =>
    apiClient.post<ApiResponse<UserDto>>('/api/auth/users', { email, password, fullName, role }).then((r) => r.data.data),

  listUsers: () =>
    apiClient.get<ApiResponse<UserDto[]>>('/api/auth/users').then((r) => r.data.data),
}
