import { useUser, useAuth } from '@clerk/clerk-react';
import { Navigate } from 'react-router-dom';

/**
 * Wrap any route element that needs a logged-in Clerk user, optionally
 * scoped to a role stored in Clerk's public metadata (set from your
 * Clerk dashboard, or server-side via the Clerk Backend API when an
 * Owner invites a Tenant).
 *
 * Usage:
 *   <Route path="/admin" element={
 *     <ProtectedRoute requiredRole="OWNER"><AdminDashboard /></ProtectedRoute>
 *   } />
 */
export default function ProtectedRoute({ children, requiredRole }) {
    const { isLoaded, isSignedIn } = useAuth();
    const { user } = useUser();

    if (!isLoaded) return null; // or a spinner

    if (!isSignedIn) {
        return <Navigate to="/sign-in" replace />;
    }

    if (requiredRole && user?.publicMetadata?.role !== requiredRole) {
        return <Navigate to="/unauthorized" replace />;
    }

    return children;
}