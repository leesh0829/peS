import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Pagination } from '@/components/list-controls'
import { apiFetch, getErrorMessage } from '@/lib/api'
import type { PageResponse } from '@/types'

type LabelEvent = { id: string; sequenceNumber: number; lotNumber: string; productCode: string;
  productName: string; producedQuantity: number; issuerName: string; reason: string | null; createdAt: string }

export function LotLabels({ lotId, canIssue }: { lotId: string; canIssue: boolean }) {
  const [page, setPage] = useState(0)
  const [reason, setReason] = useState('')
  const [preview, setPreview] = useState<LabelEvent | null>(null)
  const queryClient = useQueryClient()
  const history = useQuery({ queryKey: ['lot-labels', lotId, page],
    queryFn: () => apiFetch<PageResponse<LabelEvent>>(`/api/product-lots/${lotId}/labels?page=${page}`) })
  const mutation = useMutation({ mutationFn: (reprint: boolean) => apiFetch<LabelEvent>(`/api/product-lots/${lotId}/labels/${reprint ? 'reprint' : 'issue'}`, {
    method: 'POST', ...(reprint ? { body: JSON.stringify({ reason: reason.trim() }) } : {}),
  }), onSuccess: async event => {
    setPreview(event); setReason(''); setPage(0)
    await queryClient.invalidateQueries({ queryKey: ['lot-labels', lotId] })
  } })
  const issued = (history.data?.totalElements ?? 0) > 0
  return <section className="space-y-3 border-t pt-4">
    <h3 className="font-semibold">LOT 식별 라벨·발행 이력</h3>
    <p className="text-xs text-muted-foreground">발행은 출력용 데이터 생성 기록입니다. 실제 인쇄 성공·검사 합격·재고를 의미하지 않습니다.</p>
    {history.isPending ? <p>이력 확인 중…</p> : null}
    {history.isError ? <p role="alert">{getErrorMessage(history.error)}</p> : null}
    {canIssue && history.data ? <div className="space-y-2">
      {issued ? <><Label htmlFor="label-reason">재출력 사유</Label><Input id="label-reason" maxLength={500} value={reason} onChange={event => setReason(event.target.value)} /></> : null}
      <Button type="button" disabled={mutation.isPending || (issued && !reason.trim())} onClick={() => mutation.mutate(issued)}>{issued ? '재출력 기록 생성' : '최초 라벨 발행'}</Button>
    </div> : null}
    {mutation.isError ? <p role="alert" className="text-destructive">{getErrorMessage(mutation.error)}</p> : null}
    {history.data?.content.map(event => <div key={event.id} className="flex flex-wrap items-center gap-2 text-sm">
      <span>#{event.sequenceNumber} · {event.issuerName} · {new Date(event.createdAt).toLocaleString('ko-KR')} · {event.reason ?? '최초 발행'}</span>
      <Button size="sm" variant="outline" onClick={() => setPreview(event)}>라벨 보기</Button>
    </div>)}
    {history.data?.totalElements === 0 ? <p className="text-sm">발행 이력이 없습니다.</p> : null}
    {history.data ? <Pagination page={page} totalPages={history.data.totalPages} totalElements={history.data.totalElements} onPageChange={setPage} /> : null}
    {preview ? <div className="space-y-1 rounded-lg border bg-background p-4 text-sm">
      <p className="font-mono font-semibold">{preview.lotNumber}</p><p>{preview.productCode} · {preview.productName}</p>
      <p>생산수량 {preview.producedQuantity} EACH · 식별용 / 품질·재고 증명 아님</p>
      <p>발행 #{preview.sequenceNumber} · {preview.issuerName}</p>
      <p className="text-xs text-muted-foreground">미리보기는 기존 발행 기록 조회이며 새 재출력 기록을 만들지 않습니다.</p>
    </div> : null}
  </section>
}
