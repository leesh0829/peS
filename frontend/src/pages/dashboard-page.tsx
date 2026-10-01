import { useQuery } from '@tanstack/react-query'
import { CheckCircle2, ClipboardList, Factory, Gauge, PackageCheck, TriangleAlert } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { apiFetch, getErrorMessage } from '@/lib/api'
import type { DashboardSummary } from '@/types'

function formatRate(value: number | null) {
  return value === null ? '-' : `${value.toLocaleString()}%`
}

export function DashboardPage() {
  const summaryQuery = useQuery({
    queryKey: ['dashboard-summary'],
    queryFn: () => apiFetch<DashboardSummary>('/api/dashboard/summary'),
  })

  if (summaryQuery.isError) {
    return <p className="rounded-xl bg-red-50 p-4 text-red-700">{getErrorMessage(summaryQuery.error)}</p>
  }

  const summary = summaryQuery.data
  const cards = [
    { label: '확정 계획', value: summary ? `${summary.confirmedPlanCount.toLocaleString()}건` : '-', icon: ClipboardList },
    { label: '계획수량', value: summary ? summary.quantities.planned.toLocaleString() : '-', icon: Factory },
    { label: '누적 생산', value: summary ? summary.quantities.produced.toLocaleString() : '-', icon: PackageCheck },
    { label: '계획 달성률', value: summary ? formatRate(summary.planAchievementRate) : '-', icon: Gauge },
    { label: '양품률', value: summary ? formatRate(summary.goodRate) : '-', icon: CheckCircle2 },
    { label: '누적 불량', value: summary ? summary.quantities.defect.toLocaleString() : '-', icon: TriangleAlert },
  ]

  return (
    <section>
      <div className="mb-8">
        <p className="text-sm font-medium text-slate-500">Dashboard</p>
        <h1 className="mt-1 font-heading text-3xl font-semibold tracking-tight">생산 현황</h1>
        <p className="mt-2 text-slate-600">확정된 계획부터 작업지시와 누적 생산실적까지 전체 수량을 집계합니다.</p>
      </div>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {cards.map((card) => (
          <Card key={card.label}>
            <CardHeader className="flex-row items-center justify-between">
              <CardTitle className="text-sm text-muted-foreground">{card.label}</CardTitle>
              <card.icon aria-hidden="true" className="size-4 text-slate-400" />
            </CardHeader>
            <CardContent>
              <p className="font-heading text-2xl font-semibold tabular-nums">{card.value}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="mt-6 grid gap-6 xl:grid-cols-[1fr_1.4fr]">
        <Card>
          <CardHeader>
            <CardTitle>작업지시 상태</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-sm text-muted-foreground">대기</span>
              <Badge variant="secondary">{summary?.workOrders.waiting.toLocaleString() ?? '-'}건</Badge>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-muted-foreground">작업 중</span>
              <Badge>{summary?.workOrders.inProgress.toLocaleString() ?? '-'}건</Badge>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-muted-foreground">완료</span>
              <Badge variant="outline">{summary?.workOrders.completed.toLocaleString() ?? '-'}건</Badge>
            </div>
            <div className="border-t pt-4">
              <div className="flex items-center justify-between font-medium">
                <span>전체 작업지시</span>
                <span>{summary?.workOrders.total.toLocaleString() ?? '-'}건</span>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>수량 정합성</CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>구분</TableHead>
                  <TableHead className="text-right">수량</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                <TableRow><TableCell>확정 계획수량</TableCell><TableCell className="text-right tabular-nums">{summary?.quantities.planned.toLocaleString() ?? '-'}</TableCell></TableRow>
                <TableRow><TableCell>작업지시수량</TableCell><TableCell className="text-right tabular-nums">{summary?.quantities.ordered.toLocaleString() ?? '-'}</TableCell></TableRow>
                <TableRow><TableCell>생산수량</TableCell><TableCell className="text-right tabular-nums">{summary?.quantities.produced.toLocaleString() ?? '-'}</TableCell></TableRow>
                <TableRow><TableCell>양품수량</TableCell><TableCell className="text-right tabular-nums">{summary?.quantities.good.toLocaleString() ?? '-'}</TableCell></TableRow>
                <TableRow><TableCell>불량수량</TableCell><TableCell className="text-right tabular-nums">{summary?.quantities.defect.toLocaleString() ?? '-'}</TableCell></TableRow>
              </TableBody>
            </Table>
            <p className="mt-4 text-xs text-muted-foreground">생산수량은 양품수량과 불량수량의 합이며, 작업 완료는 재고 반영을 의미하지 않습니다.</p>
          </CardContent>
        </Card>
      </div>
    </section>
  )
}
