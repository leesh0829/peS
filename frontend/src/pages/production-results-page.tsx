import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'

import { DataTable } from '@/components/data-table'
import { type DataColumnDef } from '@/components/data-table-config'
import { ListToolbar, Pagination } from '@/components/list-controls'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { apiFetch, getErrorMessage } from '@/lib/api'
import type { CurrentUser, PageResponse, ProductionResult, WorkOrder } from '@/types'

const resultSchema = z.object({
  workOrderId: z.string().min(1, '작업지시를 선택해 주세요.'),
  producedQuantity: z.number().int().min(1, '생산수량은 1 이상이어야 합니다.'),
  goodQuantity: z.number().int().min(0, '양품수량은 0 이상이어야 합니다.'),
  defectQuantity: z.number().int().min(0, '불량수량은 0 이상이어야 합니다.'),
}).refine(
  (values) => values.producedQuantity === values.goodQuantity + values.defectQuantity,
  { path: ['producedQuantity'], message: '생산수량은 양품수량과 불량수량의 합이어야 합니다.' },
)

type ResultValues = z.infer<typeof resultSchema>

const emptyResults: ProductionResult[] = []
const dateTimeFormatter = new Intl.DateTimeFormat('ko-KR', { dateStyle: 'short', timeStyle: 'short' })

function formatDateTime(value: string) {
  return dateTimeFormatter.format(new Date(value))
}

function ProductionResultDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const queryClient = useQueryClient()
  const workOrdersQuery = useQuery({
    queryKey: ['work-orders', 'result-options'],
    queryFn: () => apiFetch<PageResponse<WorkOrder>>('/api/work-orders?status=IN_PROGRESS&page=0&size=100'),
    enabled: open,
  })
  const {
    control,
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<ResultValues>({
    resolver: zodResolver(resultSchema),
    defaultValues: { workOrderId: '', producedQuantity: 1, goodQuantity: 1, defectQuantity: 0 },
  })
  const selectedWorkOrderId = useWatch({ control, name: 'workOrderId' })
  const selectedWorkOrder = workOrdersQuery.data?.content.find((workOrder) => workOrder.id === selectedWorkOrderId)

  useEffect(() => {
    if (open) reset({ workOrderId: '', producedQuantity: 1, goodQuantity: 1, defectQuantity: 0 })
  }, [open, reset])

  const mutation = useMutation({
    mutationFn: ({ workOrderId, ...quantities }: ResultValues) => apiFetch<ProductionResult>(
      `/api/work-orders/${workOrderId}/results`,
      { method: 'POST', body: JSON.stringify(quantities) },
    ),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['production-results'] }),
        queryClient.invalidateQueries({ queryKey: ['work-orders'] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] }),
      ])
      onOpenChange(false)
    },
    onError: (error) => setError('root', { message: getErrorMessage(error) }),
  })

  return (
    <Dialog onOpenChange={onOpenChange} open={open}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>생산실적 등록</DialogTitle>
          <DialogDescription>작업 중인 지시에 증분 실적을 기록합니다. 생산수량은 양품과 불량의 합이어야 합니다.</DialogDescription>
        </DialogHeader>
        <form className="space-y-4" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
          <div className="space-y-2">
            <Label htmlFor="result-work-order">작업지시</Label>
            <select className="h-8 w-full rounded-lg border border-input bg-background px-3 text-sm" id="result-work-order" {...register('workOrderId')}>
              <option value="">작업지시 선택</option>
              {(workOrdersQuery.data?.content ?? []).map((workOrder) => (
                <option key={workOrder.id} value={workOrder.id}>
                  {workOrder.workOrderNumber} · {workOrder.product.name} · 잔여 {workOrder.remainingQuantity}
                </option>
              ))}
            </select>
            {errors.workOrderId ? <p className="text-sm text-destructive">{errors.workOrderId.message}</p> : null}
            {workOrdersQuery.isError ? <p className="text-sm text-destructive">{getErrorMessage(workOrdersQuery.error)}</p> : null}
            {workOrdersQuery.data?.content.length === 0 ? <p className="text-sm text-muted-foreground">실적을 등록할 작업 중 지시가 없습니다.</p> : null}
          </div>
          <div className="space-y-2">
            <Label htmlFor="result-produced">생산수량</Label>
            <Input id="result-produced" max={selectedWorkOrder?.remainingQuantity} min={1} type="number" {...register('producedQuantity', { valueAsNumber: true })} />
            {errors.producedQuantity ? <p className="text-sm text-destructive">{errors.producedQuantity.message}</p> : null}
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="result-good">양품수량</Label>
              <Input id="result-good" min={0} type="number" {...register('goodQuantity', { valueAsNumber: true })} />
              {errors.goodQuantity ? <p className="text-sm text-destructive">{errors.goodQuantity.message}</p> : null}
            </div>
            <div className="space-y-2">
              <Label htmlFor="result-defect">불량수량</Label>
              <Input id="result-defect" min={0} type="number" {...register('defectQuantity', { valueAsNumber: true })} />
              {errors.defectQuantity ? <p className="text-sm text-destructive">{errors.defectQuantity.message}</p> : null}
            </div>
          </div>
          {selectedWorkOrder ? (
            <p className="rounded-lg bg-muted p-3 text-sm text-muted-foreground">
              현재 누적 {selectedWorkOrder.producedQuantity.toLocaleString()} / 목표 {selectedWorkOrder.targetQuantity.toLocaleString()} · 등록 가능 {selectedWorkOrder.remainingQuantity.toLocaleString()}
            </p>
          ) : null}
          {errors.root ? <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{errors.root.message}</p> : null}
          <DialogFooter>
            <Button disabled={mutation.isPending || !selectedWorkOrder} type="submit">
              {mutation.isPending ? '등록 중…' : '실적 등록'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

export function ProductionResultsPage({ user }: { user: CurrentUser }) {
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [dialogOpen, setDialogOpen] = useState(false)
  const params = new URLSearchParams({ page: String(page), size: '20' })
  if (search.trim()) params.set('search', search.trim())

  const resultsQuery = useQuery({
    queryKey: ['production-results', search, page],
    queryFn: () => apiFetch<PageResponse<ProductionResult>>(`/api/production-results?${params}`),
  })
  const columns = useMemo<DataColumnDef<ProductionResult>[]>(() => [
    { id: 'recordedAt', header: '등록시각', cell: ({ row }) => formatDateTime(row.original.recordedAt) },
    { id: 'workOrder', header: '작업지시번호', cell: ({ row }) => row.original.workOrder.workOrderNumber },
    { id: 'product', header: '품목', cell: ({ row }) => `${row.original.product.code} · ${row.original.product.name}` },
    { accessorKey: 'producedQuantity', header: '생산', cell: ({ row }) => row.original.producedQuantity.toLocaleString() },
    { accessorKey: 'goodQuantity', header: '양품', cell: ({ row }) => row.original.goodQuantity.toLocaleString() },
    { accessorKey: 'defectQuantity', header: '불량', cell: ({ row }) => row.original.defectQuantity.toLocaleString() },
    { id: 'recordedBy', header: '등록자', cell: ({ row }) => row.original.recordedBy.displayName },
  ], [])
  const data = resultsQuery.data

  return (
    <section>
      <div className="mb-8">
        <p className="text-sm font-medium text-slate-500">Production Results</p>
        <h1 className="mt-1 font-heading text-3xl font-semibold tracking-tight">생산실적</h1>
        <p className="mt-2 text-slate-600">증분 생산실적과 양품·불량수량을 조회합니다.</p>
      </div>
      <ListToolbar
        activeFilter=""
        createLabel={user.role === 'WORKER' ? '생산실적 등록' : undefined}
        onActiveFilterChange={() => undefined}
        onCreate={user.role === 'WORKER' ? () => setDialogOpen(true) : undefined}
        onSearchChange={(value) => { setSearch(value); setPage(0) }}
        search={search}
        searchPlaceholder="작업지시번호, 계획번호 또는 품목"
        showFilter={false}
      />
      {resultsQuery.isError ? (
        <p className="rounded-xl bg-red-50 p-4 text-red-700">{getErrorMessage(resultsQuery.error)}</p>
      ) : (
        <DataTable columns={columns} data={data?.content ?? emptyResults} emptyMessage={resultsQuery.isPending ? '불러오는 중…' : '등록된 생산실적이 없습니다.'} />
      )}
      {data ? <Pagination onPageChange={setPage} page={data.page} totalElements={data.totalElements} totalPages={data.totalPages} /> : null}
      {user.role === 'WORKER' ? <ProductionResultDialog onOpenChange={setDialogOpen} open={dialogOpen} /> : null}
    </section>
  )
}
