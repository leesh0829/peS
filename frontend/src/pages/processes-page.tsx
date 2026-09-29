import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Pencil } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'

import { type DataColumnDef } from '@/components/data-table-config'
import { DataTable } from '@/components/data-table'
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
import type { PageResponse, ProductionProcess } from '@/types'

const processSchema = z.object({
  code: z.string().regex(/^[A-Z0-9_-]{2,30}$/, "대문자, 숫자, '_', '-'를 사용해 2~30자로 입력해 주세요."),
  name: z.string().min(1, '공정명을 입력해 주세요.').max(100),
  description: z.string().max(500),
  active: z.boolean(),
})

type ProcessValues = z.infer<typeof processSchema>

const emptyProcesses: ProductionProcess[] = []

type ProcessDialogProps = {
  process: ProductionProcess | null
  open: boolean
  onOpenChange: (open: boolean) => void
}

function ProcessDialog({ process, open, onOpenChange }: ProcessDialogProps) {
  const queryClient = useQueryClient()
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
    setError,
  } = useForm<ProcessValues>({
    resolver: zodResolver(processSchema),
    defaultValues: { code: '', name: '', description: '', active: true },
  })

  useEffect(() => {
    reset(
      process
        ? { code: process.code, name: process.name, description: process.description ?? '', active: process.active }
        : { code: '', name: '', description: '', active: true },
    )
  }, [process, reset])

  const mutation = useMutation({
    mutationFn: (values: ProcessValues) =>
      process
        ? apiFetch<ProductionProcess>(`/api/processes/${process.id}`, {
            method: 'PUT',
            body: JSON.stringify({
              name: values.name,
              description: values.description,
              active: values.active,
              version: process.version,
            }),
          })
        : apiFetch<ProductionProcess>('/api/processes', {
            method: 'POST',
            body: JSON.stringify({ code: values.code, name: values.name, description: values.description }),
          }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['processes'] })
      onOpenChange(false)
    },
    onError: (error) => setError('root', { message: getErrorMessage(error) }),
  })

  return (
    <Dialog onOpenChange={onOpenChange} open={open}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{process ? '공정 수정' : '공정 등록'}</DialogTitle>
          <DialogDescription>작업지시에서 선택할 표준 공정 정보입니다.</DialogDescription>
        </DialogHeader>
        <form className="space-y-4" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
          <div className="space-y-2">
            <Label htmlFor="process-code">공정 코드</Label>
            <Input disabled={Boolean(process)} id="process-code" {...register('code')} />
            {errors.code ? <p className="text-sm text-destructive">{errors.code.message}</p> : null}
          </div>
          <div className="space-y-2">
            <Label htmlFor="process-name">공정명</Label>
            <Input id="process-name" {...register('name')} />
            {errors.name ? <p className="text-sm text-destructive">{errors.name.message}</p> : null}
          </div>
          <div className="space-y-2">
            <Label htmlFor="process-description">설명</Label>
            <textarea
              className="min-h-24 w-full rounded-lg border border-input bg-background px-3 py-2 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
              id="process-description"
              {...register('description')}
            />
            {errors.description ? <p className="text-sm text-destructive">{errors.description.message}</p> : null}
          </div>
          {process ? (
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" {...register('active')} /> 사용
            </label>
          ) : null}
          {errors.root ? <p className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{errors.root.message}</p> : null}
          <DialogFooter>
            <Button disabled={mutation.isPending} type="submit">
              {mutation.isPending ? '저장 중…' : '저장'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

type ProcessesPageProps = {
  canManage: boolean
}

export function ProcessesPage({ canManage }: ProcessesPageProps) {
  const [search, setSearch] = useState('')
  const [activeFilter, setActiveFilter] = useState('')
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<ProductionProcess | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)

  const params = new URLSearchParams({ page: String(page), size: '20' })
  if (search.trim()) params.set('search', search.trim())
  if (activeFilter) params.set('active', activeFilter)

  const processesQuery = useQuery({
    queryKey: ['processes', search, activeFilter, page],
    queryFn: () => apiFetch<PageResponse<ProductionProcess>>(`/api/processes?${params}`),
  })

  const columns = useMemo<DataColumnDef<ProductionProcess>[]>(() => {
    const result: DataColumnDef<ProductionProcess>[] = [
      { accessorKey: 'code', header: '공정 코드' },
      { accessorKey: 'name', header: '공정명' },
      { accessorKey: 'description', header: '설명', cell: ({ row }) => row.original.description || '-' },
      {
        accessorKey: 'active',
        header: '상태',
        cell: ({ row }) => <Badge variant={row.original.active ? 'default' : 'secondary'}>{row.original.active ? '사용' : '미사용'}</Badge>,
      },
    ]
    if (canManage) {
      result.push({
        id: 'actions',
        header: '',
        cell: ({ row }) => (
          <Button
            aria-label={`${row.original.name} 수정`}
            onClick={() => {
              setSelected(row.original)
              setDialogOpen(true)
            }}
            size="icon-sm"
            variant="ghost"
          >
            <Pencil aria-hidden="true" />
          </Button>
        ),
      })
    }
    return result
  }, [canManage])

  const data = processesQuery.data

  return (
    <section>
      <div className="mb-8">
        <p className="text-sm font-medium text-slate-500">Master Data</p>
        <h1 className="mt-1 font-heading text-3xl font-semibold tracking-tight">공정 관리</h1>
        <p className="mt-2 text-slate-600">가상 공장의 표준 생산 공정을 관리합니다.</p>
      </div>
      <ListToolbar
        activeFilter={activeFilter}
        createLabel={canManage ? '공정 등록' : undefined}
        onActiveFilterChange={(value) => {
          setActiveFilter(value)
          setPage(0)
        }}
        onCreate={canManage ? () => {
          setSelected(null)
          setDialogOpen(true)
        } : undefined}
        onSearchChange={(value) => {
          setSearch(value)
          setPage(0)
        }}
        search={search}
      />
      {processesQuery.isError ? (
        <p className="rounded-xl bg-red-50 p-4 text-red-700">{getErrorMessage(processesQuery.error)}</p>
      ) : (
        <DataTable columns={columns} data={data?.content ?? emptyProcesses} emptyMessage={processesQuery.isPending ? '불러오는 중…' : '등록된 공정이 없습니다.'} />
      )}
      {data ? <Pagination onPageChange={setPage} page={data.page} totalElements={data.totalElements} totalPages={data.totalPages} /> : null}
      <ProcessDialog onOpenChange={setDialogOpen} open={dialogOpen} process={selected} />
    </section>
  )
}
