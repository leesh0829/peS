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
import type { PageResponse, UserRole, UserSummary } from '@/types'

const userSchema = z.object({
  username: z.string().regex(/^[a-z0-9._-]{4,50}$/, "소문자, 숫자, '.', '_', '-'를 사용해 4~50자로 입력해 주세요."),
  password: z.string(),
  displayName: z.string().min(1, '표시 이름을 입력해 주세요.').max(100),
  role: z.enum(['ADMIN', 'MANAGER', 'WORKER']),
  active: z.boolean(),
}).superRefine((value, context) => {
  if (value.password.length > 0 && (value.password.length < 10 || value.password.length > 72)) {
    context.addIssue({ code: 'custom', path: ['password'], message: '비밀번호는 10~72자로 입력해 주세요.' })
  }
})

type UserValues = z.infer<typeof userSchema>

const roleLabels: Record<UserRole, string> = {
  ADMIN: '관리자',
  MANAGER: '생산관리자',
  WORKER: '작업자',
}

const emptyUsers: UserSummary[] = []

type UserDialogProps = {
  user: UserSummary | null
  open: boolean
  onOpenChange: (open: boolean) => void
}

function UserDialog({ user, open, onOpenChange }: UserDialogProps) {
  const queryClient = useQueryClient()
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
    setError,
  } = useForm<UserValues>({
    resolver: zodResolver(userSchema),
    defaultValues: { username: '', password: '', displayName: '', role: 'WORKER', active: true },
  })

  useEffect(() => {
    reset(
      user
        ? { username: user.username, password: '', displayName: user.displayName, role: user.role, active: user.active }
        : { username: '', password: '', displayName: '', role: 'WORKER', active: true },
    )
  }, [user, reset])

  const mutation = useMutation({
    mutationFn: (values: UserValues) => {
      if (user) {
        return apiFetch<UserSummary>(`/api/admin/users/${user.id}`, {
          method: 'PUT',
          body: JSON.stringify({
            displayName: values.displayName,
            role: values.role,
            active: values.active,
            version: user.version,
          }),
        })
      }
      if (values.password.length < 10) {
        throw new Error('새 사용자의 비밀번호는 10자 이상이어야 합니다.')
      }
      return apiFetch<UserSummary>('/api/admin/users', {
        method: 'POST',
        body: JSON.stringify({
          username: values.username,
          password: values.password,
          displayName: values.displayName,
          role: values.role,
        }),
      })
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['users'] })
      onOpenChange(false)
    },
    onError: (error) => setError('root', { message: getErrorMessage(error) }),
  })

  return (
    <Dialog onOpenChange={onOpenChange} open={open}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{user ? '사용자 수정' : '사용자 등록'}</DialogTitle>
          <DialogDescription>역할은 화면 노출뿐 아니라 서버 API 권한에도 적용됩니다.</DialogDescription>
        </DialogHeader>
        <form className="space-y-4" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
          <div className="space-y-2">
            <Label htmlFor="user-username">아이디</Label>
            <Input disabled={Boolean(user)} id="user-username" {...register('username')} />
            {errors.username ? <p className="text-sm text-destructive">{errors.username.message}</p> : null}
          </div>
          {!user ? (
            <div className="space-y-2">
              <Label htmlFor="user-password">초기 비밀번호</Label>
              <Input id="user-password" type="password" {...register('password')} />
              {errors.password ? <p className="text-sm text-destructive">{errors.password.message}</p> : null}
            </div>
          ) : null}
          <div className="space-y-2">
            <Label htmlFor="user-display-name">표시 이름</Label>
            <Input id="user-display-name" {...register('displayName')} />
            {errors.displayName ? <p className="text-sm text-destructive">{errors.displayName.message}</p> : null}
          </div>
          <div className="space-y-2">
            <Label htmlFor="user-role">역할</Label>
            <select className="h-8 w-full rounded-lg border border-input bg-background px-3 text-sm" id="user-role" {...register('role')}>
              <option value="ADMIN">관리자</option>
              <option value="MANAGER">생산관리자</option>
              <option value="WORKER">작업자</option>
            </select>
          </div>
          {user ? (
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" {...register('active')} /> 활성 계정
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

export function UsersPage() {
  const [search, setSearch] = useState('')
  const [activeFilter, setActiveFilter] = useState('')
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState<UserSummary | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)

  const params = new URLSearchParams({ page: String(page), size: '20' })
  if (search.trim()) params.set('search', search.trim())
  if (activeFilter) params.set('active', activeFilter)

  const usersQuery = useQuery({
    queryKey: ['users', search, activeFilter, page],
    queryFn: () => apiFetch<PageResponse<UserSummary>>(`/api/admin/users?${params}`),
  })

  const columns = useMemo<DataColumnDef<UserSummary>[]>(
    () => [
      { accessorKey: 'username', header: '아이디' },
      { accessorKey: 'displayName', header: '표시 이름' },
      { accessorKey: 'role', header: '역할', cell: ({ row }) => roleLabels[row.original.role] },
      {
        accessorKey: 'active',
        header: '상태',
        cell: ({ row }) => <Badge variant={row.original.active ? 'default' : 'secondary'}>{row.original.active ? '활성' : '비활성'}</Badge>,
      },
      {
        id: 'actions',
        header: '',
        cell: ({ row }) => (
          <Button
            aria-label={`${row.original.displayName} 수정`}
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
      },
    ],
    [],
  )

  const data = usersQuery.data

  return (
    <section>
      <div className="mb-8">
        <p className="text-sm font-medium text-slate-500">Administration</p>
        <h1 className="mt-1 font-heading text-3xl font-semibold tracking-tight">사용자 관리</h1>
        <p className="mt-2 text-slate-600">계정을 생성하고 역할과 활성 상태를 관리합니다.</p>
      </div>
      <ListToolbar
        activeFilter={activeFilter}
        createLabel="사용자 등록"
        onActiveFilterChange={(value) => {
          setActiveFilter(value)
          setPage(0)
        }}
        onCreate={() => {
          setSelected(null)
          setDialogOpen(true)
        }}
        onSearchChange={(value) => {
          setSearch(value)
          setPage(0)
        }}
        search={search}
      />
      {usersQuery.isError ? (
        <p className="rounded-xl bg-red-50 p-4 text-red-700">{getErrorMessage(usersQuery.error)}</p>
      ) : (
        <DataTable columns={columns} data={data?.content ?? emptyUsers} emptyMessage={usersQuery.isPending ? '불러오는 중…' : '등록된 사용자가 없습니다.'} />
      )}
      {data ? <Pagination onPageChange={setPage} page={data.page} totalElements={data.totalElements} totalPages={data.totalPages} /> : null}
      <UserDialog onOpenChange={setDialogOpen} open={dialogOpen} user={selected} />
    </section>
  )
}
