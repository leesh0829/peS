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
import type { PageResponse, Product, ProductUnit } from '@/types'

const productSchema = z.object({
  code: z.string().regex(/^[A-Z0-9_-]{2,30}$/, "대문자, 숫자, '_', '-'를 사용해 2~30자로 입력해 주세요."),
  name: z.string().min(1, '품목명을 입력해 주세요.').max(100),
  unit: z.enum(['EACH', 'KILOGRAM']),
  active: z.boolean(),
})

type ProductValues = z.infer<typeof productSchema>

const unitLabels: Record<ProductUnit, string> = {
  EACH: 'EA',
  KILOGRAM: 'kg',
}

const emptyProducts: Product[] = []

type ProductDialogProps = {
  product: Product | null
  open: boolean
  onOpenChange: (open: boolean) => void
}

function ProductDialog({ product, open, onOpenChange }: ProductDialogProps) {
  const queryClient = useQueryClient()
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
    setError,
  } = useForm<ProductValues>({
    resolver: zodResolver(productSchema),
    defaultValues: { code: '', name: '', unit: 'EACH', active: true },
  })

  useEffect(() => {
    reset(
      product
        ? { code: product.code, name: product.name, unit: product.unit, active: product.active }
        : { code: '', name: '', unit: 'EACH', active: true },
    )
  }, [product, reset])

  const mutation = useMutation({
    mutationFn: (values: ProductValues) =>
      product
        ? apiFetch<Product>(`/api/products/${product.id}`, {
            method: 'PUT',
            body: JSON.stringify({ name: values.name, unit: values.unit, active: values.active, version: product.version }),
          })
        : apiFetch<Product>('/api/products', {
            method: 'POST',
            body: JSON.stringify({ code: values.code, name: values.name, unit: values.unit }),
          }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['products'] })
      onOpenChange(false)
    },
    onError: (error) => setError('root', { message: getErrorMessage(error) }),
  })

  return (
    <Dialog onOpenChange={onOpenChange} open={open}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{product ? '품목 수정' : '품목 등록'}</DialogTitle>
          <DialogDescription>생산계획에서 사용할 품목 기준정보입니다.</DialogDescription>
        </DialogHeader>
        <form className="space-y-4" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
          <div className="space-y-2">
            <Label htmlFor="product-code">품목 코드</Label>
            <Input disabled={Boolean(product)} id="product-code" {...register('code')} />
            {errors.code ? <p className="text-sm text-destructive">{errors.code.message}</p> : null}
          </div>
          <div className="space-y-2">
            <Label htmlFor="product-name">품목명</Label>
            <Input id="product-name" {...register('name')} />
            {errors.name ? <p className="text-sm text-destructive">{errors.name.message}</p> : null}
          </div>
          <div className="space-y-2">
            <Label htmlFor="product-unit">단위</Label>
            <select className="h-8 w-full rounded-lg border border-input bg-background px-3 text-sm" id="product-unit" {...register('unit')}>
              <option value="EACH">EA</option>
              <option value="KILOGRAM">kg</option>
            </select>
          </div>
          {product ? (
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

type ProductsPageProps = {
  canManage: boolean
}

export function ProductsPage({ canManage }: ProductsPageProps) {
  const [search, setSearch] = useState('')
  const [activeFilter, setActiveFilter] = useState('')
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<Product | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)

  const params = new URLSearchParams({ page: String(page), size: '20' })
  if (search.trim()) params.set('search', search.trim())
  if (activeFilter) params.set('active', activeFilter)

  const productsQuery = useQuery({
    queryKey: ['products', search, activeFilter, page],
    queryFn: () => apiFetch<PageResponse<Product>>(`/api/products?${params}`),
  })

  const columns = useMemo<DataColumnDef<Product>[]>(() => {
    const result: DataColumnDef<Product>[] = [
      { accessorKey: 'code', header: '품목 코드' },
      { accessorKey: 'name', header: '품목명' },
      { accessorKey: 'unit', header: '단위', cell: ({ row }) => unitLabels[row.original.unit] },
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

  const data = productsQuery.data

  return (
    <section>
      <div className="mb-8">
        <p className="text-sm font-medium text-slate-500">Master Data</p>
        <h1 className="mt-1 font-heading text-3xl font-semibold tracking-tight">품목 관리</h1>
        <p className="mt-2 text-slate-600">가상 공장에서 생산할 품목과 기본 단위를 관리합니다.</p>
      </div>
      <ListToolbar
        activeFilter={activeFilter}
        createLabel={canManage ? '품목 등록' : undefined}
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
      {productsQuery.isError ? (
        <p className="rounded-xl bg-red-50 p-4 text-red-700">{getErrorMessage(productsQuery.error)}</p>
      ) : (
        <DataTable columns={columns} data={data?.content ?? emptyProducts} emptyMessage={productsQuery.isPending ? '불러오는 중…' : '등록된 품목이 없습니다.'} />
      )}
      {data ? <Pagination onPageChange={setPage} page={data.page} totalElements={data.totalElements} totalPages={data.totalPages} /> : null}
      <ProductDialog onOpenChange={setDialogOpen} open={dialogOpen} product={selected} />
    </section>
  )
}
