import { useEffect, useState } from 'react';
import { UserButton } from '@clerk/clerk-react';
import { useApiClient } from '../auth/apiClient';

// Placeholder Owner dashboard — proves the whole auth chain works end to
// end: Clerk sign-in -> JWT -> SecurityConfig verifies it -> AdminController
// returns real data scoped to this owner's clerkUserId.
export default function AdminDashboard() {
  const api = useApiClient();
  const [properties, setProperties] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api.get('/api/admin/properties')
      .then((res) => setProperties(res.data))
      .catch((err) => setError(err.message));
  }, [api]);

  return (
    <div style={{ padding: '2rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>Owner Dashboard</h1>
        <UserButton />
      </div>

      <h2>Your Properties</h2>
      {error && <p style={{ color: 'red' }}>Error: {error}</p>}
      {!error && !properties && <p>Loading...</p>}
      {properties && properties.length === 0 && <p>No properties yet.</p>}
      {properties && properties.length > 0 && (
        <ul>
          {properties.map((p) => (
            <li key={p.id}>{p.name} — {p.address}</li>
          ))}
        </ul>
      )}
    </div>
  );
}
