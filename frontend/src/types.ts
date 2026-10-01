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
  id: string
  workOrderNumber: string
  productionPlan: Pick<ProductionPlan, 'id' | 'planNumber'>
  product: Pick<Product, 'id' | 'code' | 'name'>
  productionProcess: Pick<ProductionProcess, 'id' | 'code' | 'name'>
  assignedWorker: WorkerOption
  targetQuantity: number
  status: WorkOrderStatus
  startedAt: string | null
  completedAt: string | null
  version: number
  createdAt: string
  updatedAt: string
}
