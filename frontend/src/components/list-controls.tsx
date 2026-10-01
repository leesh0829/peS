import { ChevronLeft, ChevronRight, Plus, Search } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'

type ListToolbarProps = {
  search: string
  onSearchChange: (value: string) => void
  activeFilter: string
  onActiveFilterChange: (value: string) => void
  onCreate?: () => void
  createLabel?: string
  filterLabel?: string
  filterOptions?: { label: string; value: string }[]
  searchPlaceholder?: string
  showFilter?: boolean
}

const defaultFilterOptions = [
  { value: '', label: '전체 상태' },
  { value: 'true', label: '사용' },
  { value: 'false', label: '미사용' },
]

export function ListToolbar({
  search,
  onSearchChange,
  activeFilter,
  onActiveFilterChange,
  onCreate,
  createLabel,
  filterLabel = '사용 상태',
  filterOptions = defaultFilterOptions,
  searchPlaceholder = '코드 또는 이름 검색',
  showFilter = true,
}: ListToolbarProps) {
  return (
    <div className="mb-4 flex flex-col gap-3 rounded-xl border bg-white p-4 sm:flex-row sm:items-center">
      <div className="relative flex-1">
        <Search aria-hidden="true" className="absolute top-1/2 left-3 size-4 -translate-y-1/2 text-slate-400" />
        <Input
          aria-label="검색"
          className="pl-9"
          onChange={(event) => onSearchChange(event.target.value)}
          placeholder={searchPlaceholder}
          value={search}
        />
      </div>
      {showFilter ? (
        <select
          aria-label={filterLabel}
          className="h-8 rounded-lg border border-input bg-background px-3 text-sm"
          onChange={(event) => onActiveFilterChange(event.target.value)}
          value={activeFilter}
        >
          {filterOptions.map((option) => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </select>
      ) : null}
      {onCreate && createLabel ? (
        <Button onClick={onCreate} type="button">
          <Plus aria-hidden="true" />
          {createLabel}
        </Button>
      ) : null}
    </div>
  )
}

type PaginationProps = {
  page: number
  totalPages: number
  totalElements: number
  onPageChange: (page: number) => void
}

export function Pagination({ page, totalPages, totalElements, onPageChange }: PaginationProps) {
  return (
    <div className="mt-4 flex items-center justify-between text-sm text-slate-500">
      <span>총 {totalElements.toLocaleString()}건</span>
      <div className="flex items-center gap-2">
        <Button
          aria-label="이전 페이지"
          disabled={page === 0}
          onClick={() => onPageChange(page - 1)}
          size="icon-sm"
          variant="outline"
        >
          <ChevronLeft aria-hidden="true" />
        </Button>
        <span className="min-w-20 text-center">
          {totalPages === 0 ? 0 : page + 1} / {totalPages}
        </span>
        <Button
          aria-label="다음 페이지"
          disabled={page + 1 >= totalPages}
          onClick={() => onPageChange(page + 1)}
          size="icon-sm"
          variant="outline"
        >
          <ChevronRight aria-hidden="true" />
        </Button>
      </div>
    </div>
  )
}
