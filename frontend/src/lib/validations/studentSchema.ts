import { z } from 'zod'

export const studentSchema = z.object({
  fullName: z.string().min(1, 'Full name is required'),
  phone: z.string().optional(),
  parentPhone: z.string().optional(),
  notes: z.string().optional(),
})

export type StudentFormValues = z.infer<typeof studentSchema>
