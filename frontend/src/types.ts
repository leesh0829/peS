export type UserRole = 'ADMIN' | 'MANAGER' | 'WORKER'
export type ProductUnit = 'EACH' | 'KILOGRAM'

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
