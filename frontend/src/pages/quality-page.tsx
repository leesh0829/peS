import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { useFieldArray, useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import { DataTable } from '@/components/data-table'
import type { DataColumnDef } from '@/components/data-table-config'
import { ListToolbar, Pagination } from '@/components/list-controls'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { apiFetch, getErrorMessage } from '@/lib/api'
import type { CurrentUser, DefectCode, Inspection, PageResponse, ProductLot } from '@/types'

const codeSchema = z.object({
  code: z.string().regex(/^[A-Z0-9_-]{2,30}$/, '대문자·숫자·_·-를 사용해 2~30자로 입력하세요.'),
  name: z.string().trim().min(1, '불량명을 입력하세요.').max(100),
})
const quantity = z.number().int().min(0).max(2147483647)
const inspectionSchema = z.object({
  productLotId: z.string().min(1, '생산 LOT를 선택하세요.'),
  inspectedQuantity: quantity.min(1),
  acceptedQuantity: quantity,
  defects: z.array(z.object({ defectCodeId: z.string().min(1, '불량코드를 선택하세요.'), quantity: quantity.min(1) })).max(100),
  note: z.string().max(500),
}).superRefine((values, context) => {
  const rejected = values.defects.reduce((total, defect) => total + defect.quantity, 0)
  if (values.inspectedQuantity !== values.acceptedQuantity + rejected) {
    context.addIssue({ code: 'custom', path: ['acceptedQuantity'], message: '검사수량 = 합격수량 + 불량내역 합계여야 합니다.' })
  }
  if (new Set(values.defects.map(defect => defect.defectCodeId)).size !== values.defects.length) {
    context.addIssue({ code: 'custom', path: ['acceptedQuantity'], message: '동일 불량코드를 중복 선택할 수 없습니다.' })
  }
})
type InspectionValues = z.infer<typeof inspectionSchema>
const emptyCodes: DefectCode[] = []
const emptyInspections: Inspection[] = []
const timeFormat = new Intl.DateTimeFormat('ko-KR', { dateStyle: 'short', timeStyle: 'short' })

function CodeDialog({ onClose }: { onClose: () => void }) {
  const queryClient = useQueryClient()
  const { register, handleSubmit, setError, formState: { errors } } = useForm<z.infer<typeof codeSchema>>({
    resolver: zodResolver(codeSchema), defaultValues: { code: '', name: '' },
  })
  const mutation = useMutation({ mutationFn: (values: z.infer<typeof codeSchema>) => apiFetch<DefectCode>('/api/defect-codes', {
    method: 'POST', body: JSON.stringify(values),
  }), onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['defect-codes'] })
    onClose()
  }, onError: error => setError('root', { message: getErrorMessage(error) }) })
  return <Dialog open onOpenChange={open => { if (!open) onClose() }}><DialogContent>
    <DialogHeader><DialogTitle>불량코드 등록</DialogTitle><DialogDescription>가상 불량 유형을 등록합니다. 등록 후 이름 변경·삭제는 지원하지 않습니다.</DialogDescription></DialogHeader>
    <form className="space-y-4" onSubmit={handleSubmit(values => mutation.mutate(values))}>
      <div className="space-y-2"><Label htmlFor="defect-code">불량코드</Label><Input id="defect-code" {...register('code')} />
        {errors.code ? <p role="alert" className="text-sm text-destructive">{errors.code.message}</p> : null}</div>
      <div className="space-y-2"><Label htmlFor="defect-name">불량명</Label><Input id="defect-name" {...register('name')} />
        {errors.name ? <p role="alert" className="text-sm text-destructive">{errors.name.message}</p> : null}</div>
      {errors.root ? <p role="alert" className="text-sm text-destructive">{errors.root.message}</p> : null}
      <Button disabled={mutation.isPending} type="submit">불량코드 저장</Button>
    </form>
  </DialogContent></Dialog>
}

function InspectionDialog({ onClose }: { onClose: () => void }) {
  const queryClient = useQueryClient()
  const [lotSearch, setLotSearch] = useState('')
  const [codeSearch, setCodeSearch] = useState('')
  const candidates = useQuery({ queryKey: ['inspection-candidates', lotSearch],
    queryFn: () => apiFetch<PageResponse<ProductLot>>(`/api/inspection-candidates?size=100&search=${encodeURIComponent(lotSearch)}`) })
  const codes = useQuery({ queryKey: ['defect-codes', 'options', codeSearch],
    queryFn: () => apiFetch<PageResponse<DefectCode>>(`/api/defect-codes?size=100&search=${encodeURIComponent(codeSearch)}`) })
  const { control, register, setValue, handleSubmit, setError, formState: { errors } } = useForm<InspectionValues>({
    resolver: zodResolver(inspectionSchema), defaultValues: { productLotId: '', inspectedQuantity: 1, acceptedQuantity: 1, defects: [], note: '' },
  })
  const { fields, append, remove } = useFieldArray({ control, name: 'defects' })
  const selectedId = useWatch({ control, name: 'productLotId' })
  const selected = candidates.data?.content.find(lot => lot.id === selectedId)
  const mutation = useMutation({ mutationFn: ({ productLotId, ...values }: InspectionValues) => apiFetch<Inspection>(`/api/product-lots/${productLotId}/inspect`, {
    method: 'POST', body: JSON.stringify(values),
  }), onSuccess: async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['inspections'] }),
      queryClient.invalidateQueries({ queryKey: ['inspection-candidates'] }),
      queryClient.invalidateQueries({ queryKey: ['lot-inspection'] }),
    ])
    onClose()
  }, onError: error => setError('root', { message: getErrorMessage(error) }) })
  return <Dialog open onOpenChange={open => { if (!open) onClose() }}><DialogContent className="max-h-[85vh] overflow-y-auto sm:max-w-2xl">
    <DialogHeader><DialogTitle>전수검사 등록</DialogTitle><DialogDescription>완료된 작업지시의 생산 LOT를 한 번 검사합니다. 재검사와 재고 반영은 지원하지 않습니다.</DialogDescription></DialogHeader>
    <form className="space-y-4" onSubmit={handleSubmit(values => mutation.mutate(values))}>
      <div className="space-y-2"><Label htmlFor="inspection-lot-search">검사 대기 LOT 검색</Label>
        <Input id="inspection-lot-search" value={lotSearch} onChange={event => setLotSearch(event.target.value)} placeholder="LOT 또는 작업지시번호" />
        <Label htmlFor="inspection-lot">생산 LOT</Label>
        <select id="inspection-lot" className="h-8 w-full rounded-lg border border-input bg-background px-3 text-sm"
          {...register('productLotId', { onChange: event => {
            const lot = candidates.data?.content.find(item => item.id === event.target.value)
            if (lot) { setValue('inspectedQuantity', lot.result.producedQuantity); setValue('acceptedQuantity', lot.result.goodQuantity); setValue('defects', []) }
          } })}>
          <option value="">생산 LOT 선택</option>
          {(candidates.data?.content ?? []).map(lot => <option key={lot.id} value={lot.id}>{lot.lotNumber} · {lot.result.workOrder.workOrderNumber} · 생산 {lot.result.producedQuantity}</option>)}
        </select>
        {errors.productLotId ? <p role="alert" className="text-sm text-destructive">{errors.productLotId.message}</p> : null}
        {candidates.data?.content.length === 0 ? <p className="text-sm text-muted-foreground">검사 대기 LOT가 없습니다.</p> : null}
        {candidates.data && candidates.data.totalElements > 100 ? <p className="text-sm text-muted-foreground">최신 100건만 표시합니다. 검색으로 범위를 좁혀 주세요.</p> : null}
        {candidates.isError ? <p role="alert" className="text-destructive">{getErrorMessage(candidates.error)}</p> : null}
      </div>
      <div className="grid grid-cols-2 gap-4"><div className="space-y-2"><Label htmlFor="inspection-total">검사수량 (전수)</Label>
        <Input id="inspection-total" type="number" readOnly {...register('inspectedQuantity', { valueAsNumber: true })} /></div>
        <div className="space-y-2"><Label htmlFor="inspection-accepted">합격수량</Label>
          <Input id="inspection-accepted" type="number" min={0} max={selected?.result.goodQuantity} {...register('acceptedQuantity', { valueAsNumber: true })} />
          {errors.acceptedQuantity ? <p role="alert" className="text-sm text-destructive">{errors.acceptedQuantity.message}</p> : null}</div></div>
      {selected ? <p className="text-sm text-muted-foreground">생산 양품 {selected.result.goodQuantity} / 생산 불량 {selected.result.defectQuantity}. 기존 생산 불량을 합격 처리할 수 없습니다.</p> : null}
      <div className="space-y-2"><Label htmlFor="inspection-code-search">불량코드 검색</Label>
        <Input id="inspection-code-search" value={codeSearch} onChange={event => setCodeSearch(event.target.value)} />
        {codes.isError ? <p role="alert" className="text-destructive">{getErrorMessage(codes.error)}</p> : null}
        {codes.data && codes.data.totalElements > 100 ? <p className="text-sm text-muted-foreground">최신 100개 코드만 표시합니다. 검색으로 범위를 좁혀 주세요.</p> : null}
        {fields.map((field, index) => <div key={field.id} className="space-y-2 rounded-lg border p-3">
          <Label htmlFor={`inspection-code-${index}`}>불량코드 {index + 1}</Label>
          <select id={`inspection-code-${index}`} className="h-8 w-full rounded-lg border border-input bg-background px-3 text-sm" {...register(`defects.${index}.defectCodeId`)}>
            <option value="">불량코드 선택</option>{(codes.data?.content ?? []).map(code => <option key={code.id} value={code.id}>{code.code} · {code.name}</option>)}
          </select>
          {errors.defects?.[index]?.defectCodeId ? <p role="alert" className="text-sm text-destructive">{errors.defects[index]?.defectCodeId?.message}</p> : null}
          <Label htmlFor={`inspection-defect-quantity-${index}`}>불량수량 {index + 1}</Label>
          <Input id={`inspection-defect-quantity-${index}`} type="number" min={1} {...register(`defects.${index}.quantity`, { valueAsNumber: true })} />
          {errors.defects?.[index]?.quantity ? <p role="alert" className="text-sm text-destructive">{errors.defects[index]?.quantity?.message}</p> : null}
          <Button type="button" variant="outline" size="sm" onClick={() => remove(index)}>불량내역 {index + 1} 삭제</Button>
        </div>)}
        <Button type="button" variant="outline" disabled={fields.length >= 100} onClick={() => append({ defectCodeId: '', quantity: 1 })}>불량내역 추가</Button>
      </div>
      <div className="space-y-2"><Label htmlFor="inspection-note">검사 메모</Label><Input id="inspection-note" maxLength={500} {...register('note')} /></div>
      {errors.root ? <p role="alert" className="text-sm text-destructive">{errors.root.message}</p> : null}
      <Button disabled={mutation.isPending || !selected || candidates.isPending} type="submit">검사 확정</Button>
    </form>
  </DialogContent></Dialog>
}

function InspectionDetail({ inspection, onClose }: { inspection: Inspection; onClose: () => void }) {
  return <Dialog open onOpenChange={open => { if (!open) onClose() }}><DialogContent>
    <DialogHeader><DialogTitle>검사 결과 상세</DialogTitle><DialogDescription>{inspection.productLot.lotNumber} · 검사 결과는 재고 반영과 별개입니다.</DialogDescription></DialogHeader>
    <p>판정 {inspection.judgement === 'PASS' ? '합격' : '불합격'} · 검사 {inspection.inspectedQuantity} / 합격 {inspection.acceptedQuantity} / 불량 {inspection.rejectedQuantity}</p>
    <ul className="space-y-2 text-sm">{inspection.defects.map(defect => <li key={defect.defectCode.id}>{defect.defectCode.code} · {defect.defectCode.name} · {defect.quantity}개</li>)}</ul>
    <p className="text-sm">검사자 {inspection.inspectedBy.displayName} · {timeFormat.format(new Date(inspection.inspectedAt))}</p>
    {inspection.note ? <p className="text-sm">{inspection.note}</p> : null}
  </DialogContent></Dialog>
}

export function QualityPage({ user }: { user: CurrentUser }) {
  const [mode, setMode] = useState<'inspections' | 'codes'>('inspections')
  const [search, setSearch] = useState('')
  const [judgement, setJudgement] = useState('')
  const [page, setPage] = useState(0)
  const [dialog, setDialog] = useState<'inspection' | 'code' | null>(null)
  const [detail, setDetail] = useState<Inspection | null>(null)
  const params = new URLSearchParams({ search, page: String(page), size: '20' })
  if (judgement) params.set('judgement', judgement)
  const inspections = useQuery({ queryKey: ['inspections', search, judgement, page], enabled: mode === 'inspections',
    queryFn: () => apiFetch<PageResponse<Inspection>>(`/api/inspections?${params}`) })
  const codes = useQuery({ queryKey: ['defect-codes', search, page], enabled: mode === 'codes',
    queryFn: () => apiFetch<PageResponse<DefectCode>>(`/api/defect-codes?${params}`) })
  const inspectionColumns = useMemo<DataColumnDef<Inspection>[]>(() => [
    { id: 'lot', header: '생산 LOT', cell: ({ row }) => row.original.productLot.lotNumber },
    { accessorKey: 'inspectedQuantity', header: '검사' }, { accessorKey: 'acceptedQuantity', header: '합격' }, { accessorKey: 'rejectedQuantity', header: '불량' },
    { accessorKey: 'judgement', header: '판정', cell: ({ row }) => <Badge variant={row.original.judgement === 'PASS' ? 'outline' : 'secondary'}>{row.original.judgement === 'PASS' ? '합격' : '불합격'}</Badge> },
    { id: 'inspector', header: '검사자', cell: ({ row }) => row.original.inspectedBy.displayName },
    { id: 'time', header: '검사시각', cell: ({ row }) => timeFormat.format(new Date(row.original.inspectedAt)) },
    { id: 'detail', header: '상세', cell: ({ row }) => <Button size="sm" variant="outline" onClick={() => setDetail(row.original)}>검사 상세</Button> },
  ], [])
  const codeColumns = useMemo<DataColumnDef<DefectCode>[]>(() => [{ accessorKey: 'code', header: '불량코드' }, { accessorKey: 'name', header: '불량명' }], [])
  const active = mode === 'inspections' ? inspections : codes
  const data = active.data
  return <section>
    <div className="mb-8"><p className="text-sm text-muted-foreground">Quality Inspection</p><h1 className="mt-1 text-3xl font-semibold">검사·불량</h1><p className="mt-2 text-muted-foreground">생산 완료와 별개의 전수검사 사건을 기록합니다. 합격만으로 재고가 증가하지 않습니다.</p></div>
    <div className="mb-4 flex gap-2">{(['inspections', 'codes'] as const).map(value => <Button key={value} aria-pressed={mode === value} variant={mode === value ? 'default' : 'outline'}
      onClick={() => { setMode(value); setPage(0); setSearch(''); setJudgement('') }}>{value === 'inspections' ? '검사 결과' : '불량코드'}</Button>)}</div>
    <ListToolbar search={search} onSearchChange={value => { setSearch(value); setPage(0) }} activeFilter={judgement} onActiveFilterChange={value => { setJudgement(value); setPage(0) }}
      showFilter={mode === 'inspections'} filterLabel="검사 판정" filterOptions={[{ value: '', label: '전체 판정' }, { value: 'PASS', label: '합격' }, { value: 'FAIL', label: '불합격' }]}
      searchPlaceholder={mode === 'inspections' ? '생산 LOT 또는 작업지시번호' : '불량코드 또는 이름'}
      onCreate={user.role !== 'WORKER' ? () => setDialog(mode === 'inspections' ? 'inspection' : 'code') : undefined}
      createLabel={mode === 'inspections' ? '전수검사 등록' : '불량코드 등록'} />
    {active.isError ? <p role="alert" className="text-destructive">{getErrorMessage(active.error)}</p> : mode === 'inspections'
      ? <DataTable columns={inspectionColumns} data={inspections.data?.content ?? emptyInspections} emptyMessage={inspections.isPending ? '불러오는 중…' : '검사 결과가 없습니다.'} />
      : <DataTable columns={codeColumns} data={codes.data?.content ?? emptyCodes} emptyMessage={codes.isPending ? '불러오는 중…' : '불량코드가 없습니다.'} />}
    {data ? <Pagination onPageChange={setPage} page={data.page} totalElements={data.totalElements} totalPages={data.totalPages} /> : null}
    {dialog === 'inspection' ? <InspectionDialog onClose={() => setDialog(null)} /> : null}
    {dialog === 'code' ? <CodeDialog onClose={() => setDialog(null)} /> : null}
    {detail ? <InspectionDetail inspection={detail} onClose={() => setDetail(null)} /> : null}
  </section>
}
