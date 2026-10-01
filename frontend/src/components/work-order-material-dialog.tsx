import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { apiFetch, getErrorMessage } from '@/lib/api'
import type { MaterialInput, MaterialLot, PageResponse, WorkOrder } from '@/types'

const schema = z.object({ materialLotId: z.string().min(1, '자재 LOT를 선택해 주세요.'),
  inputQuantity: z.number().int().min(1, '투입수량은 1 이상이어야 합니다.') })
type Values = z.infer<typeof schema>

export function WorkOrderMaterialDialog({ order, canInput, onClose }: {
  order: WorkOrder; canInput: boolean; onClose: () => void
}) {
  const queryClient = useQueryClient()
  const inputs = useQuery({ queryKey: ['order-materials', order.id],
    queryFn: () => apiFetch<MaterialInput[]>(`/api/work-orders/${order.id}/materials`) })
  const lots = useQuery({ queryKey: ['material-lots', 'options'], enabled: canInput,
    queryFn: () => apiFetch<PageResponse<MaterialLot>>('/api/material-lots?size=100') })
  const { register, handleSubmit, reset, setError, formState: { errors } } = useForm<Values>({
    resolver: zodResolver(schema), defaultValues: { materialLotId: '', inputQuantity: 1 },
  })
  const mutation = useMutation({ mutationFn: (values: Values) => apiFetch<MaterialInput>(`/api/work-orders/${order.id}/materials`, {
    method: 'POST', body: JSON.stringify(values),
  }), onSuccess: async () => {
    reset({ materialLotId: '', inputQuantity: 1 })
    await queryClient.invalidateQueries({ queryKey: ['order-materials', order.id] })
  }, onError: error => setError('root', { message: getErrorMessage(error) }) })
  const total = (inputs.data ?? []).reduce((sum, input) => sum + input.inputQuantity, 0)
  return <Dialog open onOpenChange={open => { if (!open) onClose() }}>
    <DialogContent>
      <DialogHeader><DialogTitle>자재 LOT 투입</DialogTitle>
        <DialogDescription>{order.workOrderNumber} · 작업 시작 전에 투입을 확정합니다. 시작 후 수정할 수 없습니다.</DialogDescription></DialogHeader>
      {inputs.isError ? <p role="alert" className="text-destructive">{getErrorMessage(inputs.error)}</p> : null}
      <p>투입 합계 {total} / 목표 {order.targetQuantity} (EACH)</p>
      <ul className="space-y-2 text-sm">{(inputs.data ?? []).map(input => <li key={input.id}>
        {input.materialLot.lotNumber} · {input.materialLot.materialName} · {input.inputQuantity}개
      </li>)}</ul>
      {canInput ? <form className="space-y-4" onSubmit={handleSubmit(values => mutation.mutate(values))}>
        <div className="space-y-2"><Label htmlFor="input-material">자재 LOT</Label>
          <select id="input-material" className="h-8 w-full rounded-lg border border-input bg-background px-3 text-sm" {...register('materialLotId')}>
            <option value="">자재 LOT 선택</option>
            {(lots.data?.content ?? []).filter(lot => !inputs.data?.some(input => input.materialLot.id === lot.id)).map(lot =>
              <option key={lot.id} value={lot.id}>{lot.lotNumber} · {lot.materialName} · 등록 {lot.receivedQuantity}개</option>)}
          </select>
          {errors.materialLotId ? <p role="alert" className="text-sm text-destructive">{errors.materialLotId.message}</p> : null}
          {lots.isError ? <p role="alert" className="text-destructive">{getErrorMessage(lots.error)}</p> : null}
          {lots.data && lots.data.totalElements > 100 ? <p className="text-sm text-muted-foreground">최신 100개 LOT를 표시합니다.</p> : null}
        </div>
        <div className="space-y-2"><Label htmlFor="input-quantity">투입수량</Label>
          <Input id="input-quantity" min={1} max={order.targetQuantity - total} type="number" {...register('inputQuantity', { valueAsNumber: true })} />
          {errors.inputQuantity ? <p role="alert" className="text-sm text-destructive">{errors.inputQuantity.message}</p> : null}
        </div>
        {errors.root ? <p role="alert" className="text-sm text-destructive">{errors.root.message}</p> : null}
        <Button disabled={mutation.isPending || inputs.isPending || lots.isPending || total >= order.targetQuantity} type="submit">자재 투입 확정</Button>
      </form> : null}
    </DialogContent>
  </Dialog>
}
