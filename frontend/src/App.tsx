import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router'

import { AppShell } from '@/components/app-shell'
import { getCurrentUser, login, logout } from '@/lib/api'
import { DashboardPage } from '@/pages/dashboard-page'
import { LoginPage } from '@/pages/login-page'

const ProductsPage = lazy(() => import('@/pages/products-page').then((module) => ({ default: module.ProductsPage })))
const ProcessesPage = lazy(() => import('@/pages/processes-page').then((module) => ({ default: module.ProcessesPage })))
const UsersPage = lazy(() => import('@/pages/users-page').then((module) => ({ default: module.UsersPage })))
const ProductionPlansPage = lazy(() => import('@/pages/production-plans-page').then((module) => ({ default: module.ProductionPlansPage })))
const WorkOrdersPage = lazy(() => import('@/pages/work-orders-page').then((module) => ({ default: module.WorkOrdersPage })))
const ProductionResultsPage = lazy(() => import('@/pages/production-results-page').then((module) => ({ default: module.ProductionResultsPage })))

export default function App() {
  const queryClient = useQueryClient()
  const currentUserQuery = useQuery({
    queryKey: ['current-user'],
    queryFn: getCurrentUser,
    retry: false,
  })

  const logoutMutation = useMutation({
    mutationFn: logout,
    onSuccess: () => {
      queryClient.clear()
    },
  })

  if (currentUserQuery.isPending) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-slate-50 text-slate-500">
        세션 확인 중…
      </main>
    )
  }

  if (!currentUserQuery.isError && !currentUserQuery.data) {
    return (
      <LoginPage
        onLogin={async (values) => {
          await login(values.username, values.password)
          await queryClient.invalidateQueries({ queryKey: ['current-user'] })
        }}
      />
    )
  }

  if (currentUserQuery.isError || !currentUserQuery.data) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-slate-50 p-6 text-red-700">
        사용자 정보를 불러올 수 없습니다. 잠시 후 새로고침해 주세요.
      </main>
    )
  }

  const user = currentUserQuery.data
  const canManageMasterData = user.role === 'ADMIN' || user.role === 'MANAGER'

  return (
    <Suspense fallback={<p className="p-8 text-slate-500">화면을 불러오는 중…</p>}>
      <Routes>
      <Route
        element={(
          <AppShell
            isLoggingOut={logoutMutation.isPending}
            onLogout={() => logoutMutation.mutate()}
            user={user}
          />
        )}
      >
        <Route element={canManageMasterData ? <DashboardPage /> : <Navigate replace to="/work-orders" />} index />
        <Route element={<ProductsPage canManage={canManageMasterData} />} path="products" />
        <Route element={<ProcessesPage canManage={canManageMasterData} />} path="processes" />
        <Route element={canManageMasterData ? <ProductionPlansPage /> : <Navigate replace to="/work-orders" />} path="production-plans" />
        <Route element={<WorkOrdersPage user={user} />} path="work-orders" />
        <Route element={<ProductionResultsPage user={user} />} path="production-results" />
        <Route element={user.role === 'ADMIN' ? <UsersPage /> : <Navigate replace to="/" />} path="users" />
        <Route element={<Navigate replace to="/" />} path="*" />
      </Route>
      </Routes>
    </Suspense>
  )
}
