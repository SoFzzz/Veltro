import { lazy, Suspense } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthGuard, RoleGuard } from './components/auth';
import { MainLayout } from './components/layout';
import { LoginPage, RegisterPage } from './pages/auth';
import { UnauthorizedPage, NotFoundPage } from './pages/ErrorPages';

// Lazy load page components for code splitting
const ProductListPage = lazy(() =>
  import('./pages/catalog').then((m) => ({ default: m.ProductListPage }))
);
const ProductFormPage = lazy(() =>
  import('./pages/catalog').then((m) => ({ default: m.ProductFormPage }))
);
const CategoryPage = lazy(() =>
  import('./pages/catalog').then((m) => ({ default: m.CategoryPage }))
);
const POSPage = lazy(() =>
  import('./pages/pos').then((m) => ({ default: m.POSPage }))
);
const AlertListPage = lazy(() =>
  import('./pages/inventory/AlertListPage').then((m) => ({
    default: m.AlertListPage,
  }))
);
const InventoryPage = lazy(() =>
  import('./pages/inventory/InventoryPage').then((m) => ({
    default: m.InventoryPage,
  }))
);
const PurchaseOrderPage = lazy(() =>
  import('./pages/purchasing/PurchaseOrderPage').then((m) => ({
    default: m.PurchaseOrderPage,
  }))
);
const SupplierPage = lazy(() =>
  import('./pages/purchasing/SupplierPage').then((m) => ({
    default: m.SupplierPage,
  }))
);
const DashboardPage = lazy(() =>
  import('./pages/dashboard/DashboardPage').then((m) => ({
    default: m.DashboardPage,
  }))
);
const AuditListPage = lazy(() =>
  import('./pages/audit').then((m) => ({ default: m.AuditListPage }))
);
const WorkersPage = lazy(() =>
  import('./pages/settings').then((m) => ({ default: m.WorkersPage }))
);

// Loading component shown while lazy components are loading
const PageLoader = () => (
  <div className="flex justify-center items-center h-96">
    <div className="space-y-4 text-center">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto"></div>
      <p className="text-gray-600">Loading...</p>
    </div>
  </div>
);

function App() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/unauthorized" element={<UnauthorizedPage />} />

      {/* Protected routes */}
      <Route
        path="/"
        element={
          <AuthGuard>
            <MainLayout />
          </AuthGuard>
        }
      >
        {/* Default redirect based on role handled by LoginPage */}
        <Route index element={<Navigate to="/dashboard" replace />} />

        {/* Dashboard - All authenticated users */}
        <Route
          path="dashboard"
          element={
            <Suspense fallback={<PageLoader />}>
              <DashboardPage />
            </Suspense>
          }
        />

        {/* POS - ADMIN and CASHIER only */}
        <Route
          path="pos"
          element={
            <RoleGuard allowedRoles={['ADMIN', 'CASHIER']}>
              <Suspense fallback={<PageLoader />}>
                <POSPage />
              </Suspense>
            </RoleGuard>
          }
        />

        {/* Inventory - ADMIN and WAREHOUSE only */}
        <Route
          path="inventory"
          element={
            <RoleGuard allowedRoles={['ADMIN', 'WAREHOUSE']}>
              <Suspense fallback={<PageLoader />}>
                <InventoryPage />
              </Suspense>
            </RoleGuard>
          }
        />

        {/* Alerts - ADMIN and WAREHOUSE only */}
        <Route
          path="alerts"
          element={
            <RoleGuard allowedRoles={['ADMIN', 'WAREHOUSE']}>
              <Suspense fallback={<PageLoader />}>
                <AlertListPage />
              </Suspense>
            </RoleGuard>
          }
        />

        {/* Purchasing - ADMIN and WAREHOUSE only */}
        <Route
          path="purchasing"
          element={
            <RoleGuard allowedRoles={['ADMIN', 'WAREHOUSE']}>
              <Suspense fallback={<PageLoader />}>
                <PurchaseOrderPage />
              </Suspense>
            </RoleGuard>
          }
        />
        <Route
          path="purchasing/suppliers"
          element={
            <RoleGuard allowedRoles={['ADMIN']}>
              <Suspense fallback={<PageLoader />}>
                <SupplierPage />
              </Suspense>
            </RoleGuard>
          }
        />

        {/* Catalog - ADMIN only */}
        <Route
          path="catalog"
          element={
            <RoleGuard allowedRoles={['ADMIN']}>
              <Navigate to="/catalog/products" replace />
            </RoleGuard>
          }
        />
        <Route
          path="catalog/products"
          element={
            <RoleGuard allowedRoles={['ADMIN']}>
              <Suspense fallback={<PageLoader />}>
                <ProductListPage />
              </Suspense>
            </RoleGuard>
          }
        />
        <Route
          path="catalog/products/new"
          element={
            <RoleGuard allowedRoles={['ADMIN']}>
              <Suspense fallback={<PageLoader />}>
                <ProductFormPage />
              </Suspense>
            </RoleGuard>
          }
        />
        <Route
          path="catalog/products/:id/edit"
          element={
            <RoleGuard allowedRoles={['ADMIN']}>
              <Suspense fallback={<PageLoader />}>
                <ProductFormPage />
              </Suspense>
            </RoleGuard>
          }
        />
        <Route
          path="catalog/categories"
          element={
            <RoleGuard allowedRoles={['ADMIN']}>
              <Suspense fallback={<PageLoader />}>
                <CategoryPage />
              </Suspense>
            </RoleGuard>
          }
        />

        {/* Audit Trail - ADMIN only */}
        <Route
          path="audit"
          element={
            <RoleGuard allowedRoles={['ADMIN']}>
              <Suspense fallback={<PageLoader />}>
                <AuditListPage />
              </Suspense>
            </RoleGuard>
          }
        />

        {/* Worker Management - ADMIN only */}
        <Route
          path="settings/workers"
          element={
            <RoleGuard allowedRoles={['ADMIN']}>
              <Suspense fallback={<PageLoader />}>
                <WorkersPage />
              </Suspense>
            </RoleGuard>
          }
        />
      </Route>

      {/* 404 */}
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}

export default App;
