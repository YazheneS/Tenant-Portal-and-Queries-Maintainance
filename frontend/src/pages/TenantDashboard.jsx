import { useEffect, useState } from 'react';
import { UserButton } from '@clerk/clerk-react';
import { useApiClient } from '../auth/apiClient';

export default function TenantDashboard() {
  const api = useApiClient();
  const [queries, setQueries] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api.get('/api/tenant/maintenance-queries')
      .then((res) => setQueries(res.data))
      .catch((err) => setError(err.message));
  }, [api]);

  return (
    <div style={{ padding: '2rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>Tenant Dashboard</h1>
        <UserButton />
      </div>

      <h2>Your Maintenance Queries</h2>
      {error && <p style={{ color: 'red' }}>Error: {error}</p>}
      {!error && !queries && <p>Loading...</p>}
      {queries && queries.length === 0 && <p>No open requests.</p>}
      {queries && queries.length > 0 && (
        <ul>
          {queries.map((q) => (
            <li key={q.id}>{q.title} — {q.status}</li>
          ))}
        </ul>
      )}
    </div>
  );
}
