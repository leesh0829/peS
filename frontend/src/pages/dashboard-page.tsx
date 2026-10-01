import { Boxes, ClipboardList, Route } from 'lucide-react'

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'

const cards = [
  { label: '등록 품목', value: '기준정보 준비', icon: Boxes },
  { label: '등록 공정', value: '기준정보 준비', icon: Route },
  { label: '계획 · 작업지시', value: '계획 실행 준비', icon: ClipboardList },
]

export function DashboardPage() {
  return (
    <section>
      <div className="mb-8">
        <p className="text-sm font-medium text-slate-500">Dashboard</p>
        <h1 className="mt-1 font-heading text-3xl font-semibold tracking-tight">생산 현황</h1>
        <p className="mt-2 text-slate-600">생산계획을 확정하고 작업지시를 배정해 실행 상태를 확인합니다.</p>
      </div>
      <div className="grid gap-4 md:grid-cols-3">
        {cards.map((card) => (
          <Card key={card.label}>
            <CardHeader className="flex-row items-center justify-between">
              <CardTitle className="text-sm text-muted-foreground">{card.label}</CardTitle>
              <card.icon aria-hidden="true" className="size-4 text-slate-400" />
            </CardHeader>
            <CardContent>
              <p className="text-xl font-semibold">{card.value}</p>
            </CardContent>
          </Card>
        ))}
      </div>
    </section>
  )
}
