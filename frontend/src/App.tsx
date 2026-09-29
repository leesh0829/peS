import { useQuery } from '@tanstack/react-query'
import { Activity, Database, Factory, Server } from 'lucide-react'
import { Navigate, Route, Routes } from 'react-router'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

type HealthResponse = {
  status: string
  service: string
}

async function fetchApiHealth(): Promise<HealthResponse> {
  const response = await fetch('/api/health')

  if (!response.ok) {
    throw new Error(`API 상태 확인 실패 (${response.status})`)
  }

  return response.json() as Promise<HealthResponse>
}

function SystemStatusPage() {
  const healthQuery = useQuery({
    queryKey: ['api-health'],
    queryFn: fetchApiHealth,
    retry: 1,
  })

  const isConnected = healthQuery.data?.status === 'UP'

  return (
    <main className="min-h-screen bg-slate-50 text-slate-950">
      <div className="mx-auto flex min-h-screen max-w-6xl flex-col px-5 py-8 sm:px-8 lg:py-12">
        <header className="flex items-center justify-between border-b border-slate-200 pb-5">
          <div className="flex items-center gap-3">
            <span className="flex size-10 items-center justify-center rounded-xl bg-slate-950 text-white">
              <Factory aria-hidden="true" className="size-5" />
            </span>
            <div>
              <p className="font-heading text-xl font-semibold tracking-tight">peS</p>
              <p className="text-sm text-slate-500">production execution System</p>
            </div>
          </div>
          <Badge variant="outline">Phase 1</Badge>
        </header>

        <section className="grid flex-1 items-center gap-8 py-12 lg:grid-cols-[1.25fr_0.75fr]">
          <div className="max-w-2xl">
            <p className="mb-3 text-sm font-medium tracking-widest text-slate-500 uppercase">
              Small MES Portfolio
            </p>
            <h1 className="font-heading text-4xl leading-tight font-semibold tracking-tight sm:text-5xl">
              생산 흐름을 데이터로 연결하는 소형 MES
            </h1>
            <p className="mt-5 max-w-xl text-base leading-7 text-slate-600 sm:text-lg">
              가상 부품 공장의 생산계획부터 작업지시, 생산실적과 집계까지 단계적으로 구현합니다.
            </p>
          </div>

          <Card className="shadow-sm">
            <CardHeader className="border-b">
              <CardTitle className="flex items-center gap-2">
                <Activity aria-hidden="true" className="size-4" />
                실행 환경 상태
              </CardTitle>
              <CardDescription>프론트엔드에서 백엔드 API 연결을 확인합니다.</CardDescription>
              <CardAction>
                <Badge variant={isConnected ? 'default' : 'secondary'}>
                  {healthQuery.isPending ? '확인 중' : isConnected ? '정상' : '연결 필요'}
                </Badge>
              </CardAction>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="flex items-center justify-between rounded-lg bg-slate-100 px-4 py-3">
                <span className="flex items-center gap-2 text-slate-600">
                  <Server aria-hidden="true" className="size-4" /> API
                </span>
                <span className="font-medium">{healthQuery.data?.service ?? 'peS API'}</span>
              </div>
              <div className="flex items-center justify-between rounded-lg bg-slate-100 px-4 py-3">
                <span className="flex items-center gap-2 text-slate-600">
                  <Database aria-hidden="true" className="size-4" /> Database
                </span>
                <span className="font-medium">PostgreSQL</span>
              </div>
              {healthQuery.isError ? (
                <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
                  <p>백엔드가 아직 실행되지 않았습니다.</p>
                  <Button
                    className="mt-3"
                    onClick={() => void healthQuery.refetch()}
                    size="sm"
                    type="button"
                    variant="outline"
                  >
                    다시 확인
                  </Button>
                </div>
              ) : null}
            </CardContent>
          </Card>
        </section>

        <footer className="border-t border-slate-200 pt-5 text-sm text-slate-500">
          React · Spring Boot · PostgreSQL · Flyway
        </footer>
      </div>
    </main>
  )
}

export default function App() {
  return (
    <Routes>
      <Route element={<SystemStatusPage />} path="/" />
      <Route element={<Navigate replace to="/" />} path="*" />
    </Routes>
  )
}
