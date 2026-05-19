import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useNavigate } from 'react-router-dom'
import { authApi } from '../../api/endpoints/auth'
import { useAuthStore } from '../../store/authStore'
import { useState } from 'react'

const schema = z.object({
  centerName: z.string().min(1, 'Center name required'),
  adminEmail: z.string().email('Invalid email'),
  adminPassword: z.string().min(8, 'Minimum 8 characters'),
  adminFullName: z.string().min(1, 'Full name required'),
})

type RegisterForm = z.infer<typeof schema>

export default function RegisterPage() {
  const { setAuth } = useAuthStore()
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<RegisterForm>({
    resolver: zodResolver(schema),
  })

  const onSubmit = async (data: RegisterForm) => {
    setError(null)
    try {
      const res = await authApi.registerTenant(data)
      setAuth(res.accessToken, res.user)
      navigate('/', { replace: true })
    } catch {
      setError('Registration failed. Please try again.')
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="w-full max-w-sm bg-white rounded-lg shadow-md p-8">
        <h2 className="text-2xl font-bold text-gray-900 mb-6">Register your center</h2>

        {error && (
          <div className="mb-4 p-3 bg-red-50 text-red-700 rounded text-sm">{error}</div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Center Name</label>
            <input {...register('centerName')}
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm" />
            {errors.centerName && <p className="text-red-500 text-xs mt-1">{errors.centerName.message}</p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Admin Email</label>
            <input {...register('adminEmail')} type="email"
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm" />
            {errors.adminEmail && <p className="text-red-500 text-xs mt-1">{errors.adminEmail.message}</p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Admin Full Name</label>
            <input {...register('adminFullName')}
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm" />
            {errors.adminFullName && <p className="text-red-500 text-xs mt-1">{errors.adminFullName.message}</p>}
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Password</label>
            <input {...register('adminPassword')} type="password"
              className="w-full border border-gray-300 rounded-md px-3 py-2 text-sm" />
            {errors.adminPassword && <p className="text-red-500 text-xs mt-1">{errors.adminPassword.message}</p>}
          </div>
          <button type="submit" disabled={isSubmitting}
            className="w-full bg-blue-600 text-white py-2 rounded-md text-sm font-medium hover:bg-blue-700 disabled:opacity-50">
            {isSubmitting ? 'Registering...' : 'Register'}
          </button>
        </form>

        <p className="mt-4 text-center text-sm text-gray-500">
          Already have an account?{' '}
          <a href="/login" className="text-blue-600 hover:underline">Sign in</a>
        </p>
      </div>
    </div>
  )
}
