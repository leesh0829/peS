import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Check, ClipboardPlus } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import { DataTable } from '@/components/data-table'
import { type DataColumnDef } from '@/components/data-table-config'
import { ListToolbar, Pagination } from '@/components/list-controls'
import { Badge } from '@/components/ui/badge'
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
import type { PageResponse, ProductionPlan, ProductionProcess, Product, WorkerOption, WorkOrder } from '@/types'

const planSchema = z.object({
  productId: z.string().min(1, '품목을 선택해 주세요.'),
  dueDate: z.string().min(1, '납기일을 입력해 주세요.'),
  targetQuantity: z.number().int().min(1, '목표수량은 1 이상이어야 합니다.'),
})

const workOrderSchema = z.object({
  lotTrackingEnabled: z.boolean(),
  productionProcessId: z.string().min(1, '공정을 선택해 주세요.'),
  assignedWorkerId: z.string().min(1, '작업자를 선택해 주세요.'),
  targetQuantity: z.number().int().min(1, '지시수량은 1 이상이어야 합니다.'),
})

type PlanValues = z.infer<typeof planSchema>
type WorkOrderValues = z.infer<typeof workOrderSchema>

const emptyPlans: ProductionPlan[] = []
const today = (() => {
  const date = new Date()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
})()
const statusOptions = [
  { value: '', label: '전체 상태' },
  { value: 'DRAFT', label: '작성 중' },
  { value: 'CONFIRMED', label: '확정' },
]

function PlanDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const queryClient = useQueryClient()
  const productsQuery = useQuery({
    queryKey: ['products', 'active-options'],
    queryFn: () => apiFetch<PageResponse<Product>>('/api/products?active=true&page=0&size=100'),
    enabled: open,
  })
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
    setError,
  } = useForm<PlanValues>({
    resolver: zodResolver(planSchema),
    defaultValues: { productId: '', dueDate: '', targetQuantity: 1 },
  })

  useEffect(() => {
    if (open) reset({ productId: '', dueDate: '', targetQuantity: 1 })
  }, [open, reset])

  const mutation = useMutation({
    mutationFn: (values: PlanValues) => apiFetch<ProductionPlan>('/api/production-plans', {
      method: 'POST',
      body: JSON.stringify(values),
    }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['production-plans'] })
      onOpenChange(false)
    },
    onError: (error) => setError('root', { message: getErrorMessage(error) }),
  })

  return (
    <Dialog onOpenChange={onOpenChange} open={open}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>생산계획 등록</DialogTitle>
          <DialogDescription>품목별 목표수량과 납기일을 등록합니다. 저장 후 확정해야 작업지시를 만들 수 있습니다.</DialogDescription>
        </DialogHeader>
        <form className="space-y-4" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
          <div className="space-y-2">
            <Label htmlFor="plan-product">품목</Label>
            <select className="h-8 w-full rounded-lg border border-input bg-background px-3 text-sm" id="plan-product" {...register('productId')}>
              <option value="">품목 선택</option>
              {(productsQuery.data?.content ?? []).map((product) => (
                <option key={product.id} value={product.id}>{product.code} · {product.name}</option>
              ))}
            </select>
            {errors.productId ? <p className="text-sm text-destructive">{errors.productId.message}</p> : null}
          </div>
          <div className="space-y-2">
            <Label htmlFor="plan-due-date">납기일</Label>
            <Input id="plan-due-date" min={today} type="date" {...register('dueDate')} />
            {errors.dueDate ? <p className="text-sm text-destructive">{errors.dueDate.message}</p> : null}
          </div>
          <div className="space-y-2">
            <Label htmlFor="plan-target-quantity">목표수량</Label>
            <Input id="plan-target-quantity" min={1} type="number" {...register('targetQuantity', { valueAsNumber: true })} />
            {errors.targetQuantity ? <p className="text-sm text-destructive">{errors.targetQuantity.message}</p> : null}
          </div>
          {errors.root ? <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{errors.root.message}</p> : null}
          <DialogFooter>
            <Button disabled={mutation.isPending} type="submit">{mutation.isPending ? '저장 중…' : '저장'}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

function WorkOrderDialog({
  plan,
  open,
  onOpenChange,
}: {
  plan: ProductionPlan | null
  open: boolean
  onOpenChange: (open: boolean) => void
}) {
  const queryClient = useQueryClient()
  const processesQuery = useQuery({
    queryKey: ['processes', 'active-options'],
    queryFn: () => apiFetch<PageResponse<ProductionProcess>>('/api/processes?active=true&page=0&size=100'),
    enabled: open,
  })
  const workersQuery = useQuery({
    queryKey: ['workers', 'active-options'],
    queryFn: () => apiFetch<WorkerOption[]>('/api/workers'),
    enabled: open,
  })
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
    setError,
  } = useForm<WorkOrderValues>({
    resolver: zodResolver(workOrderSchema),
    defaultValues: { productionProcessId: '', assignedWorkerId: '', targetQuantity: 1, lotTrackingEnabled: false },
  })

  useEffect(() => {
    if (open) reset({ productionProcessId: '', assignedWorkerId: '', targetQuantity: plan?.remainingQuantity ?? 1, lotTrackingEnabled: false })
  }, [open, plan, reset])

  const mutation = useMutation({
    mutationFn: (values: WorkOrderValues) => apiFetch<WorkOrder>('/api/work-orders', {
      method: 'POST',
      body: JSON.stringify({ ...values, productionPlanId: plan?.id }),
    }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['production-plans'] }),
        queryClient.invalidateQueries({ queryKey: ['work-orders'] }),
      ])
      onOpenChange(false)
    },
    onError: (error) => setError('root', { message: getErrorMessage(error) }),
  })

  return (
    <Dialog onOpenChange={onOpenChange} open={open}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>작업지시 등록</DialogTitle>
          <DialogDescription>
            {plan ? `${plan.planNumber} · 잔여 ${plan.remainingQuantity.toLocaleString()} ${plan.product.unit === 'EACH' ? 'EA' : 'kg'}` : ''}
          </DialogDescription>
        </DialogHeader>
        <form className="space-y-4" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
          <div className="space-y-2">
            <Label htmlFor="order-process">공정</Label>
            <select className="h-8 w-full rounded-lg border border-input bg-background px-3 text-sm" id="order-process" {...register('productionProcessId')}>
              <option value="">공정 선택</option>
              {(processesQuery.data?.content ?? []).map((process) => (
                <option key={process.id} value={process.id}>{process.code} · {process.name}</option>
              ))}
            </select>
            {errors.productionProcessId ? <p className="text-sm text-destructive">{errors.productionProcessId.message}</p> : null}
          </div>
          <div className="space-y-2">
            <Label htmlFor="order-worker">담당 작업자</Label>
            <select className="h-8 w-full rounded-lg border border-input bg-background px-3 text-sm" id="order-worker" {...register('assignedWorkerId')}>
              <option value="">작업자 선택</option>
              {(workersQuery.data ?? []).map((worker) => (
                <option key={worker.id} value={worker.id}>{worker.displayName} ({worker.username})</option>
              ))}
            </select>
            {errors.assignedWorkerId ? <p className="text-sm text-destructive">{errors.assignedWorkerId.message}</p> : null}
          </div>
          <div className="space-y-2">
            <Label htmlFor="order-target-quantity">지시수량</Label>
            <Input id="order-target-quantity" max={plan?.remainingQuantity} min={1} type="number" {...register('targetQuantity', { valueAsNumber: true })} />
            {errors.targetQuantity ? <p className="text-sm text-destructive">{errors.targetQuantity.message}</p> : null}
          </div>
          <Label className="flex items-center gap-2" htmlFor="order-lot-tracking">
            <input id="order-lot-tracking" type="checkbox" {...register('lotTrackingEnabled')} />
            LOT 추적 사용 (EACH 품목, 자재 1개 → 부품 1개)
          </Label>
          {errors.root ? <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{errors.root.message}</p> : null}
          <DialogFooter>
            <Button disabled={mutation.isPending || !plan} type="submit">{mutation.isPending ? '저장 중…' : '작업지시 저장'}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

export function ProductionPlansPage() {
  const queryClient = useQueryClient()
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage] = useState(0)
  const [planDialogOpen, setPlanDialogOpen] = useState(false)
  const [selectedPlan, setSelectedPlan] = useState<ProductionPlan | null>(null)
  const [workOrderDialogOpen, setWorkOrderDialogOpen] = useState(false)

  const params = new URLSearchParams({ page: String(page), size: '20' })
  if (search.trim()) params.set('search', search.trim())
  if (statusFilter) params.set('status', statusFilter)

  const plansQuery = useQuery({
    queryKey: ['production-plans', search, statusFilter, page],
    queryFn: () => apiFetch<PageResponse<ProductionPlan>>(`/api/production-plans?${params}`),
  })
  const {
    mutate: confirmPlan,
    isPending: isConfirming,
    isError: isConfirmError,
    error: confirmError,
  } = useMutation({
    mutationFn: (planId: string) => apiFetch<ProductionPlan>(`/api/production-plans/${planId}/confirm`, { method: 'POST' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['production-plans'] }),
  })

  const columns = useMemo<DataColumnDef<ProductionPlan>[]>(() => [
    { accessorKey: 'planNumber', header: '계획번호' },
    { id: 'product', header: '품목', cell: ({ row }) => `${row.original.product.code} · ${row.original.product.name}` },
    { accessorKey: 'dueDate', header: '납기일' },
    { accessorKey: 'targetQuantity', header: '목표', cell: ({ row }) => row.original.targetQuantity.toLocaleString() },
    { accessorKey: 'allocatedQuantity', header: '지시', cell: ({ row }) => row.original.allocatedQuantity.toLocaleString() },
    { accessorKey: 'remainingQuantity', header: '잔여', cell: ({ row }) => row.original.remainingQuantity.toLocaleString() },
    {
      accessorKey: 'status',
      header: '상태',
      cell: ({ row }) => <Badge variant={row.original.status === 'CONFIRMED' ? 'default' : 'secondary'}>{row.original.status === 'CONFIRMED' ? '확정' : '작성 중'}</Badge>,
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => row.original.status === 'DRAFT' ? (
        <Button disabled={isConfirming} onClick={() => confirmPlan(row.original.id)} size="sm" variant="outline">
          <Check aria-hidden="true" /> 확정
        </Button>
      ) : row.original.remainingQuantity > 0 ? (
        <Button onClick={() => {
          setSelectedPlan(row.original)
          setWorkOrderDialogOpen(true)
        }} size="sm" variant="outline">
          <ClipboardPlus aria-hidden="true" /> 작업지시
        </Button>
      ) : <span className="text-xs text-slate-400">배정 완료</span>,
    },
  ], [confirmPlan, isConfirming])

  const data = plansQuery.data

  return (
    <section>
      <div className="mb-8">
        <p className="text-sm font-medium text-slate-500">Planning</p>
        <h1 className="mt-1 font-heading text-3xl font-semibold tracking-tight">생산계획</h1>
        <p className="mt-2 text-slate-600">품목별 목표를 확정하고, 계획수량 안에서 작업지시를 나누어 배정합니다.</p>
      </div>
      <ListToolbar
        activeFilter={statusFilter}
        createLabel="생산계획 등록"
        filterLabel="계획 상태"
        filterOptions={statusOptions}
        onActiveFilterChange={(value) => { setStatusFilter(value); setPage(0) }}
        onCreate={() => setPlanDialogOpen(true)}
        onSearchChange={(value) => { setSearch(value); setPage(0) }}
        search={search}
        searchPlaceholder="계획번호, 품목 코드 또는 품목명"
      />
      {isConfirmError ? <p className="mb-4 rounded-xl bg-red-50 p-4 text-red-700">{getErrorMessage(confirmError)}</p> : null}
      {plansQuery.isError ? (
        <p className="rounded-xl bg-red-50 p-4 text-red-700">{getErrorMessage(plansQuery.error)}</p>
      ) : (
        <DataTable columns={columns} data={data?.content ?? emptyPlans} emptyMessage={plansQuery.isPending ? '불러오는 중…' : '등록된 생산계획이 없습니다.'} />
      )}
      {data ? <Pagination onPageChange={setPage} page={data.page} totalElements={data.totalElements} totalPages={data.totalPages} /> : null}
      <PlanDialog onOpenChange={setPlanDialogOpen} open={planDialogOpen} />
      <WorkOrderDialog onOpenChange={setWorkOrderDialogOpen} open={workOrderDialogOpen} plan={selectedPlan} />
    </section>
  )
}
