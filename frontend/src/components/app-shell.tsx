import { Boxes, Factory, Gauge, LogOut, Route, Users } from 'lucide-react'
import { NavLink, Outlet } from 'react-router'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import type { CurrentUser } from '@/types'

type AppShellProps = {
  user: CurrentUser
  onLogout: () => void
  isLoggingOut: boolean
}

const roleLabels = {
  ADMIN: '관리자',
  MANAGER: '생산관리자',
  WORKER: '작업자',
} as const

export function AppShell({ user, onLogout, isLoggingOut }: AppShellProps) {
  const canManageMasterData = user.role !== 'WORKER'

  const navigation = [
    { to: '/', label: '대시보드', icon: Gauge, visible: true },
    { to: '/products', label: '품목 관리', icon: Boxes, visible: true },
    { to: '/processes', label: '공정 관리', icon: Route, visible: true },
    { to: '/users', label: '사용자 관리', icon: Users, visible: user.role === 'ADMIN' },
  ].filter((item) => item.visible)

  return (
    <div className="min-h-screen bg-slate-50 text-slate-950 lg:grid lg:grid-cols-[240px_1fr]">
      <aside className="border-b bg-slate-950 text-white lg:min-h-screen lg:border-r lg:border-b-0">
        <div className="flex h-16 items-center gap-3 border-b border-white/10 px-5">
          <span className="flex size-9 items-center justify-center rounded-lg bg-white text-slate-950">
            <Factory aria-hidden="true" className="size-5" />
          </span>
          <div>
            <p className="font-heading text-lg font-semibold">peS</p>
            <p className="text-xs text-slate-400">Small MES</p>
          </div>
        </div>
        <nav aria-label="주 메뉴" className="flex gap-2 overflow-x-auto p-3 lg:flex-col">
          {navigation.map((item) => (
            <NavLink
              className={({ isActive }) =>
                cn(
                  'flex shrink-0 items-center gap-3 rounded-lg px-3 py-2 text-sm transition-colors',
                  isActive ? 'bg-white text-slate-950' : 'text-slate-300 hover:bg-white/10 hover:text-white',
                )
              }
              end={item.to === '/'}
              key={item.to}
              to={item.to}
            >
              <item.icon aria-hidden="true" className="size-4" />
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="min-w-0">
        <header className="flex h-16 items-center justify-between border-b bg-white px-5 sm:px-8">
          <div>
            <p className="font-medium">{user.displayName}</p>
            <div className="mt-0.5 flex items-center gap-2 text-xs text-slate-500">
              <Badge variant="outline">{roleLabels[user.role]}</Badge>
              {canManageMasterData ? '기준정보 편집 가능' : '기준정보 조회 전용'}
            </div>
          </div>
          <Button disabled={isLoggingOut} onClick={onLogout} size="sm" variant="outline">
            <LogOut aria-hidden="true" />
            로그아웃
          </Button>
        </header>
        <div className="p-5 sm:p-8">
          <Outlet />
        </div>
      </div>
    </div>
  )
}
