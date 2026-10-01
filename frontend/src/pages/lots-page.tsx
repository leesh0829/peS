import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { DataTable } from '@/components/data-table'
import type { DataColumnDef } from '@/components/data-table-config'
import { ListToolbar, Pagination } from '@/components/list-controls'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { apiFetch, getErrorMessage } from '@/lib/api'
import type { CurrentUser, Inspection, MaterialInput, MaterialLot, MaterialTrace, PageResponse, ProductLot, ProductTrace } from '@/types'

const materialSchema = z.object({
  materialCode: z.string().regex(/^[A-Z0-9_-]{2,30}$/, '대문자·숫자·_·-를 사용해 2~30자로 입력하세요.'),
  materialName: z.string().trim().min(1, '자재명을 입력하세요.').max(100),
  receivedQuantity: z.number().int().min(1, '등록수량은 1 이상이어야 합니다.'),
})
type MaterialValues = z.infer<typeof materialSchema>
const emptyMaterials: MaterialLot[] = []
const emptyProducts: ProductLot[] = []

function MaterialDialog({ onClose }: { onClose: () => void }) {
  const queryClient = useQueryClient()
  const { register, handleSubmit, setError, formState: { errors } } = useForm<MaterialValues>({
    resolver: zodResolver(materialSchema), defaultValues: { materialCode: '', materialName: '', receivedQuantity: 1 },
  })
  const mutation = useMutation({ mutationFn: (values: MaterialValues) => apiFetch<MaterialLot>('/api/material-lots', {
    method: 'POST', body: JSON.stringify(values),
  }), onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['material-lots'] })
    onClose()
  }, onError: error => setError('root', { message: getErrorMessage(error) }) })
  return <Dialog open onOpenChange={open => { if (!open) onClose() }}><DialogContent>
    <DialogHeader><DialogTitle>자재 LOT 등록</DialogTitle><DialogDescription>가상 자재의 최초 수량을 EACH 단위로 기록합니다. 재고 수불 기능은 아닙니다.</DialogDescription></DialogHeader>
    <form className="space-y-4" onSubmit={handleSubmit(values => mutation.mutate(values))}>
      <div className="space-y-2"><Label htmlFor="material-code">자재코드</Label><Input id="material-code" {...register('materialCode')} />
        {errors.materialCode ? <p role="alert" className="text-sm text-destructive">{errors.materialCode.message}</p> : null}</div>
      <div className="space-y-2"><Label htmlFor="material-name">자재명</Label><Input id="material-name" {...register('materialName')} />
        {errors.materialName ? <p role="alert" className="text-sm text-destructive">{errors.materialName.message}</p> : null}</div>
      <div className="space-y-2"><Label htmlFor="material-received">등록수량 (EACH)</Label><Input id="material-received" min={1} type="number" {...register('receivedQuantity', { valueAsNumber: true })} />
        {errors.receivedQuantity ? <p role="alert" className="text-sm text-destructive">{errors.receivedQuantity.message}</p> : null}</div>
      {errors.root ? <p role="alert" className="text-sm text-destructive">{errors.root.message}</p> : null}
      <Button disabled={mutation.isPending} type="submit">자재 LOT 저장</Button>
    </form>
  </DialogContent></Dialog>
}

function Inputs({ inputs }: { inputs: MaterialInput[] }) {
  return <ul className="space-y-2 text-sm">{inputs.map(input => <li key={input.id}>
    {input.materialLot.lotNumber} · {input.materialLot.materialName} → {input.workOrderNumber} · 투입 {input.inputQuantity}개
  </li>)}{inputs.length === 0 ? <li className="text-muted-foreground">연결된 자재 투입이 없습니다.</li> : null}</ul>
}

function TraceDialog({ selection, onClose }: { selection: { kind: 'material' | 'product'; id: string }; onClose: () => void }) {
  const material = useQuery({ queryKey: ['lot-trace', 'material', selection.id], enabled: selection.kind === 'material',
    queryFn: () => apiFetch<MaterialTrace>(`/api/material-lots/${selection.id}/trace`) })
  const product = useQuery({ queryKey: ['lot-trace', 'product', selection.id], enabled: selection.kind === 'product',
    queryFn: () => apiFetch<ProductTrace>(`/api/product-lots/${selection.id}/trace`) })
  const active = selection.kind === 'material' ? material : product
  const inspection = useQuery({ queryKey: ['lot-inspection', selection.id], enabled: selection.kind === 'product',
    queryFn: () => apiFetch<{ inspection: Inspection | null }>(`/api/product-lots/${selection.id}/inspection`) })
  return <Dialog open onOpenChange={open => { if (!open) onClose() }}><DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-2xl">
    <DialogHeader><DialogTitle>LOT 계보 상세</DialogTitle><DialogDescription>작업지시 단위 연결입니다. 개별 부품의 자재 배분·검사 합격·가용재고를 의미하지 않습니다.</DialogDescription></DialogHeader>
    {active.isPending ? <p>불러오는 중…</p> : null}
    {active.isError ? <p role="alert" className="text-destructive">{getErrorMessage(active.error)}</p> : null}
    {selection.kind === 'material' && material.data ? <>
      <h3 className="font-semibold">{material.data.materialLot.lotNumber} · 등록 {material.data.materialLot.receivedQuantity}개</h3>
      <Inputs inputs={material.data.inputs} />
      <h3 className="font-semibold">연결된 생산 LOT</h3>
      <ul className="space-y-2 text-sm">{material.data.productLots.map(lot => <li key={lot.id}>{lot.lotNumber} · {lot.result.workOrder.workOrderNumber} · 생산 {lot.result.producedQuantity} / 양품 {lot.result.goodQuantity} / 불량 {lot.result.defectQuantity}</li>)}</ul>
      {material.data.productLots.length === 0 ? <p className="text-sm text-muted-foreground">생성된 생산 LOT가 없습니다.</p> : null}
    </> : null}
    {selection.kind === 'product' && product.data ? <>
      <h3 className="font-semibold">{product.data.productLot.lotNumber}</h3>
      <p className="text-sm">{product.data.productLot.result.product.name} · {product.data.productLot.result.workOrder.workOrderNumber}</p>
      <p className="text-sm">생산 {product.data.productLot.result.producedQuantity} / 양품 {product.data.productLot.result.goodQuantity} / 불량 {product.data.productLot.result.defectQuantity}</p>
      <h3 className="font-semibold">연결된 자재 LOT</h3><Inputs inputs={product.data.materials} />
      <h3 className="font-semibold">검사 결과</h3>
      {inspection.isPending ? <p className="text-sm">검사 확인 중…</p> : null}
      {inspection.isError ? <p role="alert" className="text-destructive">{getErrorMessage(inspection.error)}</p> : null}
      {inspection.data?.inspection ? <>
        <p className="text-sm">{inspection.data.inspection.judgement === 'PASS' ? '합격' : '불합격'} · 검사 {inspection.data.inspection.inspectedQuantity} / 합격 {inspection.data.inspection.acceptedQuantity} / 불량 {inspection.data.inspection.rejectedQuantity}</p>
        <ul className="text-sm">{inspection.data.inspection.defects.map(defect => <li key={defect.defectCode.id}>{defect.defectCode.code} · {defect.defectCode.name} · {defect.quantity}개</li>)}</ul>
      </> : inspection.data ? <p className="text-sm text-muted-foreground">미검사</p> : null}
    </> : null}
    <p className="text-xs text-muted-foreground">작업자는 본인에게 배정된 작업지시의 연결만 조회합니다.</p>
  </DialogContent></Dialog>
}

export function LotsPage({ user }: { user: CurrentUser }) {
  const [kind, setKind] = useState<'material' | 'product'>('material')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [registerOpen, setRegisterOpen] = useState(false)
  const [selection, setSelection] = useState<{ kind: 'material' | 'product'; id: string } | null>(null)
  const params = new URLSearchParams({ search, page: String(page), size: '20' })
  const materials = useQuery({ queryKey: ['material-lots', search, page], enabled: kind === 'material',
    queryFn: () => apiFetch<PageResponse<MaterialLot>>(`/api/material-lots?${params}`) })
  const products = useQuery({ queryKey: ['product-lots', search, page], enabled: kind === 'product',
    queryFn: () => apiFetch<PageResponse<ProductLot>>(`/api/product-lots?${params}`) })
  const materialColumns = useMemo<DataColumnDef<MaterialLot>[]>(() => [
    { accessorKey: 'lotNumber', header: '자재 LOT' }, { accessorKey: 'materialCode', header: '자재코드' },
    { accessorKey: 'materialName', header: '자재명' }, { accessorKey: 'receivedQuantity', header: '최초 등록수량 (EACH)' },
    { id: 'trace', header: '계보', cell: ({ row }) => <Button size="sm" variant="outline" onClick={() => setSelection({ kind: 'material', id: row.original.id })}>정방향 조회</Button> },
  ], [])
  const productColumns = useMemo<DataColumnDef<ProductLot>[]>(() => [
    { accessorKey: 'lotNumber', header: '생산 LOT' },
    { id: 'order', header: '작업지시', cell: ({ row }) => row.original.result.workOrder.workOrderNumber },
    { id: 'product', header: '품목', cell: ({ row }) => row.original.result.product.name },
    { id: 'quantity', header: '생산 / 양품 / 불량', cell: ({ row }) => `${row.original.result.producedQuantity} / ${row.original.result.goodQuantity} / ${row.original.result.defectQuantity}` },
    { id: 'trace', header: '계보', cell: ({ row }) => <Button size="sm" variant="outline" onClick={() => setSelection({ kind: 'product', id: row.original.id })}>역방향 조회</Button> },
  ], [])
  const active = kind === 'material' ? materials : products
  const data = active.data
  return <section>
    <div className="mb-8"><p className="text-sm text-muted-foreground">Lot Genealogy</p><h1 className="mt-1 text-3xl font-semibold">LOT 추적</h1>
      <p className="mt-2 text-muted-foreground">자재 LOT → 작업지시 → 생산실적 → 생산 LOT의 연결을 조회합니다.</p></div>
    <div className="mb-4 flex gap-2" aria-label="LOT 종류">
      {(['material', 'product'] as const).map(value => <Button key={value} aria-pressed={kind === value} variant={kind === value ? 'default' : 'outline'}
        onClick={() => { setKind(value); setSearch(''); setPage(0) }}>{value === 'material' ? '자재 LOT' : '생산 LOT'}</Button>)}
    </div>
    <ListToolbar search={search} onSearchChange={value => { setSearch(value); setPage(0) }} activeFilter="" onActiveFilterChange={() => undefined} showFilter={false}
      searchPlaceholder={kind === 'material' ? 'LOT 번호, 자재코드 또는 자재명' : 'LOT 번호, 작업지시번호 또는 품목코드'}
      onCreate={kind === 'material' && user.role !== 'WORKER' ? () => setRegisterOpen(true) : undefined} createLabel="자재 LOT 등록" />
    {active.isError ? <p role="alert" className="text-destructive">{getErrorMessage(active.error)}</p> : kind === 'material'
      ? <DataTable columns={materialColumns} data={materials.data?.content ?? emptyMaterials} emptyMessage={materials.isPending ? '불러오는 중…' : '등록된 자재 LOT가 없습니다.'} />
      : <DataTable columns={productColumns} data={products.data?.content ?? emptyProducts} emptyMessage={products.isPending ? '불러오는 중…' : '생성된 생산 LOT가 없습니다.'} />}
    {data ? <Pagination onPageChange={setPage} page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} /> : null}
    {registerOpen ? <MaterialDialog onClose={() => setRegisterOpen(false)} /> : null}
    {selection ? <TraceDialog key={`${selection.kind}-${selection.id}`} selection={selection} onClose={() => setSelection(null)} /> : null}
  </section>
}
