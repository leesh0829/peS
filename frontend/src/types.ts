export type UserRole = 'ADMIN' | 'MANAGER' | 'WORKER'
export type ProductUnit = 'EACH' | 'KILOGRAM'
export type ProductionPlanStatus = 'DRAFT' | 'CONFIRMED'
export type WorkOrderStatus = 'WAITING' | 'IN_PROGRESS' | 'COMPLETED'

export type CurrentUser = {
  id: string
  username: string
  displayName: string
  role: UserRole
}

export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type UserSummary = CurrentUser & {
  active: boolean
  version: number
  createdAt: string
  updatedAt: string
}

export type Product = {
  id: string
  code: string
  name: string
  unit: ProductUnit
  active: boolean
  version: number
  createdAt: string
  updatedAt: string
}

export type ProductionProcess = {
  id: string
  code: string
  name: string
  description: string | null
  active: boolean
  version: number
  createdAt: string
  updatedAt: string
}

export type WorkerOption = Pick<CurrentUser, 'id' | 'username' | 'displayName'>

export type ProductionPlan = {
  id: string
  planNumber: string
  product: Pick<Product, 'id' | 'code' | 'name' | 'unit'>
  dueDate: string
  targetQuantity: number
  status: ProductionPlanStatus
  allocatedQuantity: number
  remainingQuantity: number
  workOrderCount: number
  version: number
  createdAt: string
  updatedAt: string
}

export type WorkOrder = {

  lotTrackingEnabled: boolean
  id: string
  workOrderNumber: string
  productionPlan: Pick<ProductionPlan, 'id' | 'planNumber'>
  product: Pick<Product, 'id' | 'code' | 'name'>
  productionProcess: Pick<ProductionProcess, 'id' | 'code' | 'name'>
  assignedWorker: WorkerOption
  targetQuantity: number
  producedQuantity: number
  goodQuantity: number
  defectQuantity: number
  remainingQuantity: number
  status: WorkOrderStatus
  startedAt: string | null
  completedAt: string | null
  version: number
  createdAt: string
  updatedAt: string
}

export type ProductionResult = {
  id: string
  workOrder: Pick<WorkOrder, 'id' | 'workOrderNumber' | 'targetQuantity'>
  product: Pick<Product, 'id' | 'code' | 'name'>
  producedQuantity: number
  goodQuantity: number
  defectQuantity: number
  recordedBy: WorkerOption
  recordedAt: string
}

export type MaterialLot = {
  id: string
  lotNumber: string
  materialCode: string
  materialName: string
  receivedQuantity: number
  createdAt: string
}
export type MaterialInput = {
  id: string
  workOrderId: string
  workOrderNumber: string
  status: WorkOrderStatus
  materialLot: MaterialLot
  inputQuantity: number
  recordedBy: string
  createdAt: string
}
export type ProductLot = { id: string; lotNumber: string; result: ProductionResult }
export type DefectCode = { id: string; code: string; name: string }
export type Inspection = {
  id: string
  productLot: ProductLot
  inspectedQuantity: number
  acceptedQuantity: number
  rejectedQuantity: number
  judgement: 'PASS' | 'FAIL'
  defects: { defectCode: DefectCode; quantity: number }[]
  inspectedBy: { id: string; displayName: string }
  inspectedAt: string
  note: string | null
}
export type ProductTrace = { productLot: ProductLot; materials: MaterialInput[] }
export type MaterialTrace = { materialLot: MaterialLot; inputs: MaterialInput[]; productLots: ProductLot[] }

export type DashboardSummary = {
  confirmedPlanCount: number
  workOrders: {
    waiting: number
    inProgress: number
    completed: number
    total: number
  }
  quantities: {
    planned: number
    ordered: number
    produced: number
    good: number
    defect: number
  }
  planAchievementRate: number | null
  goodRate: number | null
}
