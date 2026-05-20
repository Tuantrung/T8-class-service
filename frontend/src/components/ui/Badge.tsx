import type { ReactNode } from 'react'
import { cn } from '../../lib/utils'

interface BadgeProps {
  children: ReactNode
  variant?: 'success' | 'warning' | 'danger' | 'info' | 'neutral'
  className?: string
}

const variantClasses: Record<NonNullable<BadgeProps['variant']>, string> = {
  success: 'bg-green-100 text-green-700',
  warning: 'bg-yellow-100 text-yellow-700',
  danger: 'bg-red-100 text-red-700',
  info: 'bg-blue-100 text-blue-700',
  neutral: 'bg-gray-100 text-gray-600',
}

export function Badge({ children, variant = 'neutral', className }: BadgeProps) {
  return (
    <span className={cn('px-2 py-0.5 rounded text-xs font-medium', variantClasses[variant], className)}>
      {children}
    </span>
  )
}

export function StatusBadge({ status }: { status: string }) {
  const map: Record<string, BadgeProps['variant']> = {
    ACTIVE: 'success',
    ARCHIVED: 'neutral',
    DRAFT: 'warning',
    ISSUED: 'info',
    PAID: 'success',
    scheduled: 'info',
    completed: 'success',
    cancelled: 'danger',
    PRESENT: 'success',
    ABSENT: 'danger',
    LATE: 'warning',
  }
  return <Badge variant={map[status] ?? 'neutral'}>{status}</Badge>
}
