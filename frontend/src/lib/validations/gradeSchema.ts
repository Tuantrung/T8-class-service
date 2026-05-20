import { z } from 'zod'

export const gradeSchema = z.object({
  classId: z.string().uuid(),
  studentId: z.string().uuid(),
  examName: z.string().min(1, 'Exam name is required'),
  examDate: z.string().optional(),
  score: z.coerce.number().optional(),
  maxScore: z.coerce.number().optional(),
  notes: z.string().optional(),
})

export type GradeFormValues = z.infer<typeof gradeSchema>
