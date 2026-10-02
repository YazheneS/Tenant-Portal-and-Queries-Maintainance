import { useEffect, useState, useCallback } from 'react';
import { UserButton } from '@clerk/clerk-react';
import { useApiClient } from '../auth/apiClient';

const sectionStyle = { marginTop: '2rem', paddingTop: '1rem', borderTop: '1px solid #ddd' };

export default function AdminDashboard() {
  const api = useApiClient();
  const [properties, setProperties] = useState(null);
  const [openQueries, setOpenQueries] = useState(null);
  const [vendors, setVendors] = useState(null);
  const [disputes, setDisputes] = useState(null);
  const [error, setError] = useState(null);

  // Simple bill-creation form state — unit/tenant id typed in directly for
  // now rather than a dropdown, since there's no "pick a tenant" UI yet.
  // One RENT line item is collected inline since that's the common case;
  // additional line items (electricity, water, etc.) go through
  // POST /api/admin/bills/{id}/line-items after creation — not built into
  // this form yet, but the endpoint exists if you want to call it directly.
  const [billForm, setBillForm] = useState({
    unitId: '', tenantId: '', billMonth: '', billYear: '', dueDate: '', rentAmount: '',
  });

  const refreshAll = useCallback(() => {
    api.get('/api/admin/properties').then((res) => setProperties(res.data)).catch((err) => setError(err.message));
    api.get('/api/admin/maintenance-queries').then((res) => setOpenQueries(res.data)).catch((err) => setError(err.message));
    api.get('/api/admin/vendors').then((res) => setVendors(res.data)).catch((err) => setError(err.message));
    api.get('/api/admin/disputes').then((res) => setDisputes(res.data)).catch((err) => setError(err.message));
  }, [api]);

  useEffect(() => { refreshAll(); }, [refreshAll]);

  const assignVendor = async (queryId, vendorId) => {
    if (!vendorId) return;
    try {
      await api.post(`/api/admin/maintenance-queries/${queryId}/assign-vendor`, { vendorId: Number(vendorId) });
      refreshAll();
    } catch (err) {
      setError(err.response?.data?.error || err.message);
    }
  };

  const resolveDispute = async (disputeId, status) => {
    const ownerResponse = window.prompt(`Response to tenant (resolving as ${status}):`, '');
    if (ownerResponse === null) return; // cancelled
    try {
      await api.post(`/api/admin/disputes/${disputeId}/resolve`, { status, ownerResponse });
      refreshAll();
    } catch (err) {
      setError(err.response?.data?.error || err.message);
    }
  };

  const createBill = async (e) => {
    e.preventDefault();
    try {
      await api.post('/api/admin/bills', {
        unitId: Number(billForm.unitId),
        tenantId: Number(billForm.tenantId),
        billMonth: Number(billForm.billMonth),
        billYear: Number(billForm.billYear),
        dueDate: billForm.dueDate,
        lineItems: [
          { type: 'RENT', description: 'Monthly rent', amount: Number(billForm.rentAmount) },
        ],
      });
      setBillForm({ unitId: '', tenantId: '', billMonth: '', billYear: '', dueDate: '', rentAmount: '' });
      refreshAll();
    } catch (err) {
      setError(err.response?.data?.error || err.message);
    }
  };

  return (
    <div style={{ padding: '2rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>Owner Dashboard</h1>
        <UserButton />
      </div>

      {error && <p style={{ color: 'red' }}>Error: {error}</p>}

      <h2>Your Properties</h2>
      {!properties && <p>Loading...</p>}
      {properties && properties.length === 0 && <p>No properties yet.</p>}
      {properties && properties.length > 0 && (
        <ul>{properties.map((p) => <li key={p.id}>{p.name} — {p.address}</li>)}</ul>
      )}

      <div style={sectionStyle}>
        <h2>Open Maintenance Queries</h2>
        {!openQueries && <p>Loading...</p>}
        {openQueries && openQueries.length === 0 && <p>Nothing open.</p>}
        {openQueries && openQueries.map((q) => (
          <div key={q.id} style={{ marginBottom: '0.5rem' }}>
            {q.title} — {q.category} — {q.priority}
            {' '}
            <select defaultValue="" onChange={(e) => assignVendor(q.id, e.target.value)}>
              <option value="" disabled>Assign vendor...</option>
              {vendors && vendors.map((v) => <option key={v.id} value={v.id}>{v.name} ({v.specialization})</option>)}
            </select>
          </div>
        ))}
      </div>

      <div style={sectionStyle}>
        <h2>Open Disputes</h2>
        {!disputes && <p>Loading...</p>}
        {disputes && disputes.length === 0 && <p>No open disputes.</p>}
        {disputes && disputes.map((d) => (
          <div key={d.id} style={{ marginBottom: '0.5rem' }}>
            Dispute #{d.id} — {d.reason} — <em>{d.status}</em>
            {' '}
            <button onClick={() => resolveDispute(d.id, 'ACCEPTED')}>Accept</button>
            <button onClick={() => resolveDispute(d.id, 'REJECTED')} style={{ marginLeft: '0.5rem' }}>Reject</button>
          </div>
        ))}
      </div>

      <div style={sectionStyle}>
        <h2>Create a Bill</h2>
        <form onSubmit={createBill} style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
          <input placeholder="Unit ID" value={billForm.unitId}
                 onChange={(e) => setBillForm({ ...billForm, unitId: e.target.value })} required />
          <input placeholder="Tenant ID" value={billForm.tenantId}
                 onChange={(e) => setBillForm({ ...billForm, tenantId: e.target.value })} required />
          <input placeholder="Month (1-12)" value={billForm.billMonth}
                 onChange={(e) => setBillForm({ ...billForm, billMonth: e.target.value })} required />
          <input placeholder="Year" value={billForm.billYear}
                 onChange={(e) => setBillForm({ ...billForm, billYear: e.target.value })} required />
          <input type="date" value={billForm.dueDate}
                 onChange={(e) => setBillForm({ ...billForm, dueDate: e.target.value })} required />
          <input placeholder="Rent amount (₹)" value={billForm.rentAmount}
                 onChange={(e) => setBillForm({ ...billForm, rentAmount: e.target.value })} required />
          <button type="submit">Create</button>
        </form>
        <p style={{ fontSize: '0.85rem', color: '#666' }}>
          Creates a bill with one RENT line item. For electricity/water/other
          charges, call POST /api/admin/bills/&#123;id&#125;/line-items directly
          until that gets its own form.
        </p>
      </div>
    </div>
  );
}
