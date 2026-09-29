import { zodResolver } from '@hookform/resolvers/zod'
import { Factory } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { getErrorMessage } from '@/lib/api'

const loginSchema = z.object({
  username: z.string().min(1, '아이디를 입력해 주세요.'),
  password: z.string().min(1, '비밀번호를 입력해 주세요.'),
})

type LoginValues = z.infer<typeof loginSchema>

type LoginPageProps = {
  onLogin: (values: LoginValues) => Promise<void>
}

export function LoginPage({ onLogin }: LoginPageProps) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    setError,
  } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { username: '', password: '' },
  })

  const submit = handleSubmit(async (values) => {
    try {
      await onLogin(values)
    } catch (error) {
      setError('root', { message: getErrorMessage(error) })
    }
  })

  return (
    <main className="grid min-h-screen bg-slate-50 lg:grid-cols-[1.1fr_0.9fr]">
      <section className="hidden bg-slate-950 p-12 text-white lg:flex lg:flex-col lg:justify-between">
        <div className="flex items-center gap-3">
          <span className="flex size-10 items-center justify-center rounded-xl bg-white text-slate-950">
            <Factory aria-hidden="true" className="size-5" />
          </span>
          <span className="font-heading text-xl font-semibold">peS</span>
        </div>
        <div className="max-w-xl">
          <p className="mb-4 text-sm tracking-widest text-slate-400 uppercase">Production Execution System</p>
          <h1 className="font-heading text-5xl leading-tight font-semibold">작은 공장의 생산 흐름을 명확하게</h1>
          <p className="mt-6 text-lg leading-8 text-slate-300">
            생산계획, 작업지시, 생산실적을 실제 데이터로 연결하는 MES 포트폴리오입니다.
          </p>
        </div>
        <p className="text-sm text-slate-500">React · Spring Boot · PostgreSQL</p>
      </section>

      <section className="flex items-center justify-center p-6">
        <Card className="w-full max-w-md shadow-sm">
          <CardHeader>
            <CardTitle className="text-2xl">로그인</CardTitle>
            <CardDescription>개발 환경에서 발급된 시연 계정으로 접속하세요.</CardDescription>
          </CardHeader>
          <CardContent>
            <form className="space-y-5" onSubmit={submit}>
              <div className="space-y-2">
                <Label htmlFor="username">아이디</Label>
                <Input autoComplete="username" id="username" {...register('username')} />
                {errors.username ? <p className="text-sm text-destructive">{errors.username.message}</p> : null}
              </div>
              <div className="space-y-2">
                <Label htmlFor="password">비밀번호</Label>
                <Input autoComplete="current-password" id="password" type="password" {...register('password')} />
                {errors.password ? <p className="text-sm text-destructive">{errors.password.message}</p> : null}
              </div>
              {errors.root ? (
                <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{errors.root.message}</p>
              ) : null}
              <Button className="w-full" disabled={isSubmitting} size="lg" type="submit">
                {isSubmitting ? '로그인 중…' : '로그인'}
              </Button>
              <div className="rounded-lg bg-slate-100 p-3 text-xs leading-5 text-slate-600">
                개발 계정: admin / manager / worker<br />
                공통 비밀번호: pes-demo-1234
              </div>
            </form>
          </CardContent>
        </Card>
      </section>
    </main>
  )
}
