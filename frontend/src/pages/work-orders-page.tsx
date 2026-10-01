import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2, Play } from 'lucide-react'
import { useMemo, useState } from 'react'

import { DataTable } from '@/components/data-table'
import { type DataColumnDef } from '@/components/data-table-config'
import { ListToolbar, Pagination } from '@/components/list-controls'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { apiFetch, getErrorMessage } from '@/lib/api'
import type { CurrentUser, PageResponse, WorkOrder } from '@/types'

const emptyWorkOrders: WorkOrder[] = []
const statusOptions = [
  { value: '', label: '전체 상태' },
  { value: 'WAITING', label: '대기' },
  { value: 'IN_PROGRESS', label: '작업 중' },
  { value: 'COMPLETED', label: '완료' },
]

const statusLabels = {
  WAITING: '대기',
  IN_PROGRESS: '작업 중',
  COMPLETED: '완료',
} as const
const dateTimeFormatter = new Intl.DateTimeFormat('ko-KR', { dateStyle: 'short', timeStyle: 'short' })

function formatDateTime(value: string | null) {
  return value ? dateTimeFormatter.format(new Date(value)) : '-'
}

export function WorkOrdersPage({ user }: { user: CurrentUser }) {
  const queryClient = useQueryClient()
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [page, setPage] = useState(0)

  const params = new URLSearchParams({ page: String(page), size: '20' })
  if (search.trim()) params.set('search', search.trim())
  if (statusFilter) params.set('status', statusFilter)

  const workOrdersQuery = useQuery({
    queryKey: ['work-orders', search, statusFilter, page],
    queryFn: () => apiFetch<PageResponse<WorkOrder>>(`/api/work-orders?${params}`),
  })
  const {
    mutate: startWorkOrder,
    isPending: isStarting,
    isError: isStartError,
    error: startError,
  } = useMutation({
    mutationFn: (workOrderId: string) => apiFetch<WorkOrder>(`/api/work-orders/${workOrderId}/start`, { method: 'POST' }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['work-orders'] }),
  })
  const {
    mutate: completeWorkOrder,
    isPending: isCompleting,
    isError: isCompleteError,
    error: completeError,
  } = useMutation({
    mutationFn: (workOrderId: string) => apiFetch<WorkOrder>(`/api/work-orders/${workOrderId}/complete`, { method: 'POST' }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['work-orders'] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard-summary'] }),
      ])
    },
  })

  const columns = useMemo<DataColumnDef<WorkOrder>[]>(() => {
    const result: DataColumnDef<WorkOrder>[] = [
      { accessorKey: 'workOrderNumber', header: '작업지시번호' },
      { id: 'plan', header: '계획번호', cell: ({ row }) => row.original.productionPlan.planNumber },
      { id: 'product', header: '품목', cell: ({ row }) => `${row.original.product.code} · ${row.original.product.name}` },
      { id: 'process', header: '공정', cell: ({ row }) => `${row.original.productionProcess.code} · ${row.original.productionProcess.name}` },
      { id: 'worker', header: '작업자', cell: ({ row }) => row.original.assignedWorker.displayName },
      { accessorKey: 'targetQuantity', header: '지시수량', cell: ({ row }) => row.original.targetQuantity.toLocaleString() },
      { accessorKey: 'producedQuantity', header: '생산', cell: ({ row }) => row.original.producedQuantity.toLocaleString() },
      { accessorKey: 'goodQuantity', header: '양품', cell: ({ row }) => row.original.goodQuantity.toLocaleString() },
      { accessorKey: 'defectQuantity', header: '불량', cell: ({ row }) => row.original.defectQuantity.toLocaleString() },
      { accessorKey: 'remainingQuantity', header: '잔여', cell: ({ row }) => row.original.remainingQuantity.toLocaleString() },
      {
        accessorKey: 'status',
        header: '상태',
        cell: ({ row }) => (
          <Badge variant={row.original.status === 'WAITING' ? 'secondary' : row.original.status === 'IN_PROGRESS' ? 'default' : 'outline'}>
            {statusLabels[row.original.status]}
          </Badge>
        ),
      },
      { accessorKey: 'startedAt', header: '시작시각', cell: ({ row }) => formatDateTime(row.original.startedAt) },
    ]

    if (user.role === 'WORKER') {
      result.push({
        id: 'actions',
        header: '',
        cell: ({ row }) => {
          const workOrder = row.original
          if (workOrder.status === 'WAITING' && workOrder.assignedWorker.id === user.id) {
            return (
              <Button disabled={isStarting} onClick={() => startWorkOrder(workOrder.id)} size="sm">
                <Play aria-hidden="true" /> 작업 시작
              </Button>
            )
          }
          if (workOrder.status === 'IN_PROGRESS' && workOrder.remainingQuantity === 0) {
            return (
              <Button disabled={isCompleting} onClick={() => completeWorkOrder(workOrder.id)} size="sm" variant="outline">
                <CheckCircle2 aria-hidden="true" /> 작업 완료
              </Button>
            )
          }
          return null
        },
      })
    }
    return result
  }, [completeWorkOrder, isCompleting, isStarting, startWorkOrder, user.id, user.role])

  const data = workOrdersQuery.data

  return (
    <section>
      <div className="mb-8">
        <p className="text-sm font-medium text-slate-500">Execution</p>
        <h1 className="mt-1 font-heading text-3xl font-semibold tracking-tight">{user.role === 'WORKER' ? '내 작업지시' : '작업지시'}</h1>
        <p className="mt-2 text-slate-600">
          {user.role === 'WORKER' ? '나에게 배정된 작업지시를 확인하고 작업을 시작합니다.' : '계획별 작업지시와 작업 시작 상태를 확인합니다.'}
        </p>
      </div>
      <ListToolbar
        activeFilter={statusFilter}
        filterLabel="작업 상태"
        filterOptions={statusOptions}
        onActiveFilterChange={(value) => { setStatusFilter(value); setPage(0) }}
        onSearchChange={(value) => { setSearch(value); setPage(0) }}
        search={search}
        searchPlaceholder="작업지시번호, 계획번호 또는 품목"
      />
      {isStartError ? <p className="mb-4 rounded-xl bg-red-50 p-4 text-red-700">{getErrorMessage(startError)}</p> : null}
      {isCompleteError ? <p className="mb-4 rounded-xl bg-red-50 p-4 text-red-700">{getErrorMessage(completeError)}</p> : null}
      {workOrdersQuery.isError ? (
        <p className="rounded-xl bg-red-50 p-4 text-red-700">{getErrorMessage(workOrdersQuery.error)}</p>
      ) : (
        <DataTable columns={columns} data={data?.content ?? emptyWorkOrders} emptyMessage={workOrdersQuery.isPending ? '불러오는 중…' : '조회할 작업지시가 없습니다.'} />
      )}
      {data ? <Pagination onPageChange={setPage} page={data.page} totalElements={data.totalElements} totalPages={data.totalPages} /> : null}
    </section>
  )
}
