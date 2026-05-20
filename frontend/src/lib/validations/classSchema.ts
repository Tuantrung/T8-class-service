import { z } from 'zod'

export const classSchema = z.object({
  name: z.string().min(1, 'Class name is required'),
  subject: z.string().optional(),
  teacherId: z.string().uuid('Teacher ID must be a valid UUID'),
  ratePerSession: z.coerce.number().min(0, 'Rate must be 0 or greater'),
})

export type ClassFormValues = z.infer<typeof classSchema>
