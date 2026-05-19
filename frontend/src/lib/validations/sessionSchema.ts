import { z } from 'zod'

export const sessionSchema = z.object({
  classId: z.string().uuid('Class ID must be a valid UUID'),
  sessionDate: z.string().min(1, 'Session date is required'),
  startTime: z.string().optional(),
  endTime: z.string().optional(),
  topic: z.string().optional(),
  cancelledByTeacher: z.boolean().default(false),
})

export type SessionFormValues = z.infer<typeof sessionSchema>
