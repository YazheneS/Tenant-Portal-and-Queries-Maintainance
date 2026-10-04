import { useEffect, useState, useCallback } from 'react';
import { useApiClient } from '../auth/apiClient';
import Layout from '../components/Layout';
import StatusPill from '../components/StatusPill';

export default function AdminDashboard() {
  const api = useApiClient();
  const [data, setData] = useState({
    properties: null, units: null, vendors: null, tenants: null,
    queries: null, disputes: null, bills: null, rentAgreements: null,
  });
  const [error, setError] = useState(null);

  const refreshAll = useCallback(() => {
    const endpoints = {
      properties: '/api/admin/properties',
      units: '/api/admin/units',
      vendors: '/api/admin/vendors',
      tenants: '/api/admin/tenants',
      queries: '/api/admin/maintenance-queries',
      disputes: '/api/admin/disputes',
      bills: '/api/admin/bills',
      rentAgreements: '/api/admin/rent-agreements',
    };
    Object.entries(endpoints).forEach(([key, url]) => {
      api.get(url)
        .then((res) => setData((prev) => ({ ...prev, [key]: res.data })))
        .catch((err) => setError(err.response?.data?.error || err.message));
    });
  }, [api]);

  useEffect(() => { refreshAll(); }, [refreshAll]);

  const { properties, units, vendors, tenants, queries, disputes, bills, rentAgreements } = data;

  // ---- Property ----
  const [propertyForm, setPropertyForm] = useState({ name: '', address: '' });
  const createProperty = async (e) => {
    e.preventDefault();
    try {
      await api.post('/api/admin/properties', propertyForm);
      setPropertyForm({ name: '', address: '' });
      refreshAll();
    } catch (err) { setError(err.response?.data?.error || err.message); }
  };

  // ---- Unit ----
  const [unitForm, setUnitForm] = useState({ unitNumber: '', floor: '', propertyId: '', monthlyRent: '' });
  const createUnit = async (e) => {
    e.preventDefault();
    try {
      await api.post('/api/admin/units', {
        unitNumber: unitForm.unitNumber,
        floor: Number(unitForm.floor),
        propertyId: Number(unitForm.propertyId),
        monthlyRent: Number(unitForm.monthlyRent),
      });
      setUnitForm({ unitNumber: '', floor: '', propertyId: '', monthlyRent: '' });
      refreshAll();
    } catch (err) { setError(err.response?.data?.error || err.message); }
  };

  // ---- Vendor ----
  const [vendorForm, setVendorForm] = useState({ name: '', phone: '', specialization: 'PLUMBING' });
  const createVendor = async (e) => {
    e.preventDefault();
    try {
      await api.post('/api/admin/vendors', vendorForm);
      setVendorForm({ name: '', phone: '', specialization: 'PLUMBING' });
      refreshAll();
    } catch (err) { setError(err.response?.data?.error || err.message); }
  };

  // ---- Tenant occupancy ----
  const [occupancyForm, setOccupancyForm] = useState({ tenantId: '', unitId: '' });
  const assignTenant = async (e) => {
    e.preventDefault();
    try {
      await api.post(`/api/admin/tenants/${occupancyForm.tenantId}/assign-unit/${occupancyForm.unitId}`);
      setOccupancyForm({ tenantId: '', unitId: '' });
      refreshAll();
    } catch (err) { setError(err.response?.data?.error || err.message); }
  };

  // ---- Maintenance queries ----
  const assignVendor = async (queryId, vendorId) => {
    if (!vendorId) return;
    try {
      await api.post(`/api/admin/maintenance-queries/${queryId}/assign-vendor`, { vendorId: Number(vendorId) });
      refreshAll();
    } catch (err) { setError(err.response?.data?.error || err.message); }
  };

  // ---- Disputes ----
  const resolveDispute = async (disputeId, status) => {
    const ownerResponse = window.prompt(`Response to tenant (resolving as ${status.toLowerCase()}):`, '');
    if (ownerResponse === null) return;
    try {
      await api.post(`/api/admin/disputes/${disputeId}/resolve`, { status, ownerResponse });
      refreshAll();
    } catch (err) { setError(err.response?.data?.error || err.message); }
  };

  // ---- Bills ----
  const [billForm, setBillForm] = useState({ unitId: '', tenantId: '', billMonth: '', billYear: '', dueDate: '', rentAmount: '' });
  const createBill = async (e) => {
    e.preventDefault();
    try {
      await api.post('/api/admin/bills', {
        unitId: Number(billForm.unitId),
        tenantId: Number(billForm.tenantId),
        billMonth: Number(billForm.billMonth),
        billYear: Number(billForm.billYear),
        dueDate: billForm.dueDate,
        lineItems: [{ type: 'RENT', description: 'Monthly rent', amount: Number(billForm.rentAmount) }],
      });
      setBillForm({ unitId: '', tenantId: '', billMonth: '', billYear: '', dueDate: '', rentAmount: '' });
      refreshAll();
    } catch (err) { setError(err.response?.data?.error || err.message); }
  };

  const addLineItem = async (billId) => {
    const type = window.prompt('Line item type (RENT, ELECTRICITY, WATER, MAINTENANCE, OTHER):', 'ELECTRICITY');
    if (!type) return;
    const description = window.prompt('Description:', '');
    const amount = window.prompt('Amount (₹):', '');
    if (!amount) return;
    try {
      await api.post(`/api/admin/bills/${billId}/line-items`, { type, description, amount: Number(amount) });
      refreshAll();
    } catch (err) { setError(err.response?.data?.error || err.message); }
  };

  // ---- Rent agreements ----
  const [agreementForm, setAgreementForm] = useState({
    unitId: '', tenantId: '', baseRent: '', escalationPercent: '', escalationDayOfYear: '', startDate: '',
  });
  const createAgreement = async (e) => {
    e.preventDefault();
    try {
      await api.post('/api/admin/rent-agreements', {
        unitId: Number(agreementForm.unitId),
        tenantId: Number(agreementForm.tenantId),
        baseRent: Number(agreementForm.baseRent),
        escalationPercent: Number(agreementForm.escalationPercent),
        escalationDayOfYear: Number(agreementForm.escalationDayOfYear),
        startDate: agreementForm.startDate,
      });
      setAgreementForm({ unitId: '', tenantId: '', baseRent: '', escalationPercent: '', escalationDayOfYear: '', startDate: '' });
      refreshAll();
    } catch (err) { setError(err.response?.data?.error || err.message); }
  };

  return (
    <Layout title="Owner dashboard">
      {error && <div className="error-banner">{error}</div>}

      {/* Properties */}
      <div className="panel">
        <div className="panel-header"><h2>Properties</h2></div>
        {!properties && <p className="empty-state">Loading…</p>}
        {properties && properties.length === 0 && <p className="empty-state">No properties yet — add your first one below.</p>}
        {properties && properties.length > 0 && (
          <table>
            <thead><tr><th>Name</th><th>Address</th></tr></thead>
            <tbody>{properties.map((p) => <tr key={p.id}><td>{p.name}</td><td>{p.address}</td></tr>)}</tbody>
          </table>
        )}
      </div>
      <div className="panel panel-form">
        <h3>Add a property</h3>
        <form onSubmit={createProperty} className="field-row" style={{ marginTop: '0.75rem' }}>
          <div className="field"><label>Name</label>
            <input value={propertyForm.name} onChange={(e) => setPropertyForm({ ...propertyForm, name: e.target.value })} required /></div>
          <div className="field"><label>Address</label>
            <input value={propertyForm.address} onChange={(e) => setPropertyForm({ ...propertyForm, address: e.target.value })} required /></div>
          <button className="btn btn-primary" type="submit">Add property</button>
        </form>
      </div>

      {/* Units */}
      <div className="panel">
        <div className="panel-header"><h2>Units</h2></div>
        {!units && <p className="empty-state">Loading…</p>}
        {units && units.length === 0 && <p className="empty-state">No units yet — add one below once you have a property.</p>}
        {units && units.length > 0 && (
          <table>
            <thead><tr><th>Unit</th><th>Property</th><th>Rent</th><th>Occupied</th></tr></thead>
            <tbody>{units.map((u) => (
              <tr key={u.id}>
                <td>#{u.id} — {u.unitNumber}</td>
                <td>{u.property?.name}</td>
                <td className="num">₹{u.monthlyRent}</td>
                <td>{u.isOccupied ? 'Yes' : 'No'}</td>
              </tr>
            ))}</tbody>
          </table>
        )}
      </div>
      <div className="panel panel-form">
        <h3>Add a unit</h3>
        <form onSubmit={createUnit} className="field-row" style={{ marginTop: '0.75rem' }}>
          <div className="field"><label>Property</label>
            <select value={unitForm.propertyId} onChange={(e) => setUnitForm({ ...unitForm, propertyId: e.target.value })} required>
              <option value="" disabled>Select…</option>
              {properties?.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
            </select></div>
          <div className="field"><label>Unit number</label>
            <input value={unitForm.unitNumber} onChange={(e) => setUnitForm({ ...unitForm, unitNumber: e.target.value })} required /></div>
          <div className="field"><label>Floor</label>
            <input type="number" value={unitForm.floor} onChange={(e) => setUnitForm({ ...unitForm, floor: e.target.value })} required /></div>
          <div className="field"><label>Monthly rent (₹)</label>
            <input type="number" value={unitForm.monthlyRent} onChange={(e) => setUnitForm({ ...unitForm, monthlyRent: e.target.value })} required /></div>
          <button className="btn btn-primary" type="submit">Add unit</button>
        </form>
      </div>

      {/* Tenants + occupancy */}
      <div className="panel">
        <div className="panel-header"><h2>Tenants</h2></div>
        {!tenants && <p className="empty-state">Loading…</p>}
        {tenants && tenants.length === 0 && <p className="empty-state">No tenants yet.</p>}
        {tenants && tenants.length > 0 && (
          <table>
            <thead><tr><th>Name</th><th>Email</th><th>Unit</th><th>Active</th></tr></thead>
            <tbody>{tenants.map((t) => (
              <tr key={t.id}>
                <td>#{t.id} — {t.name}</td><td>{t.email}</td>
                <td>{t.unit ? `#${t.unit.id} — ${t.unit.unitNumber}` : '—'}</td>
                <td>{t.isActive ? 'Yes' : 'No'}</td>
              </tr>
            ))}</tbody>
          </table>
        )}
        <form onSubmit={assignTenant} className="field-row" style={{ marginTop: '0.75rem' }}>
          <div className="field"><label>Tenant</label>
            <select value={occupancyForm.tenantId} onChange={(e) => setOccupancyForm({ ...occupancyForm, tenantId: e.target.value })} required>
              <option value="" disabled>Select…</option>
              {tenants?.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
            </select></div>
          <div className="field"><label>Unit</label>
            <select value={occupancyForm.unitId} onChange={(e) => setOccupancyForm({ ...occupancyForm, unitId: e.target.value })} required>
              <option value="" disabled>Select…</option>
              {units?.map((u) => <option key={u.id} value={u.id}>{u.unitNumber} — {u.property?.name}</option>)}
            </select></div>
          <button className="btn btn-outline" type="submit">Assign to unit</button>
        </form>
        <p className="helptext">Tenants must already exist from signup — this only links an existing tenant to a unit.</p>
      </div>

      {/* Vendors */}
      <div className="panel">
        <div className="panel-header"><h2>Vendors</h2></div>
        {!vendors && <p className="empty-state">Loading…</p>}
        {vendors && vendors.length === 0 && <p className="empty-state">No vendors yet — add one below.</p>}
        {vendors && vendors.length > 0 && (
          <table>
            <thead><tr><th>Name</th><th>Phone</th><th>Specialization</th><th>Jobs completed</th></tr></thead>
            <tbody>{vendors.map((v) => (
              <tr key={v.id}><td>{v.name}</td><td>{v.phone}</td><td>{v.specialization}</td><td className="num">{v.totalJobsCompleted}</td></tr>
            ))}</tbody>
          </table>
        )}
      </div>
      <div className="panel panel-form">
        <h3>Add a vendor</h3>
        <form onSubmit={createVendor} className="field-row" style={{ marginTop: '0.75rem' }}>
          <div className="field"><label>Name</label>
            <input value={vendorForm.name} onChange={(e) => setVendorForm({ ...vendorForm, name: e.target.value })} required /></div>
          <div className="field"><label>Phone</label>
            <input value={vendorForm.phone} onChange={(e) => setVendorForm({ ...vendorForm, phone: e.target.value })} required /></div>
          <div className="field"><label>Specialization</label>
            <select value={vendorForm.specialization} onChange={(e) => setVendorForm({ ...vendorForm, specialization: e.target.value })}>
              <option value="PLUMBING">Plumbing</option>
              <option value="ELECTRICAL">Electrical</option>
              <option value="CARPENTRY">Carpentry</option>
            </select></div>
          <button className="btn btn-primary" type="submit">Add vendor</button>
        </form>
      </div>

      {/* Maintenance queries */}
      <div className="panel">
        <div className="panel-header"><h2>Open maintenance queries</h2></div>
        {!queries && <p className="empty-state">Loading…</p>}
        {queries && queries.length === 0 && <p className="empty-state">Nothing open right now.</p>}
        {queries && queries.map((q) => (
          <div key={q.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.5rem 0', borderBottom: '1px solid var(--border)' }}>
            <div>
              <strong>{q.title}</strong> <span className="helptext">{q.category} · {q.priority}</span>{' '}
              <StatusPill status={q.status} />
            </div>
            <select defaultValue="" onChange={(e) => assignVendor(q.id, e.target.value)}>
              <option value="" disabled>Assign vendor…</option>
              {vendors?.map((v) => <option key={v.id} value={v.id}>{v.name} ({v.specialization})</option>)}
            </select>
          </div>
        ))}
      </div>

      {/* Disputes */}
      <div className="panel">
        <div className="panel-header"><h2>Open disputes</h2></div>
        {!disputes && <p className="empty-state">Loading…</p>}
        {disputes && disputes.length === 0 && <p className="empty-state">No open disputes.</p>}
        {disputes && disputes.map((d) => (
          <div key={d.id} style={{ padding: '0.5rem 0', borderBottom: '1px solid var(--border)' }}>
            <div>Dispute #{d.id} — {d.reason} <StatusPill status={d.status} /></div>
            <div className="row-actions" style={{ marginTop: '0.4rem' }}>
              <button className="btn btn-primary" onClick={() => resolveDispute(d.id, 'ACCEPTED')}>Accept</button>
              <button className="btn btn-danger-outline" onClick={() => resolveDispute(d.id, 'REJECTED')}>Reject</button>
            </div>
          </div>
        ))}
      </div>

      {/* Bills */}
      <div className="panel">
        <div className="panel-header"><h2>Bills</h2></div>
        {!bills && <p className="empty-state">Loading…</p>}
        {bills && bills.length === 0 && <p className="empty-state">No bills yet — create one below.</p>}
        {bills && bills.length > 0 && (
          <table>
            <thead><tr><th>Bill</th><th>Tenant</th><th>Total</th><th>Paid</th><th>Status</th><th></th></tr></thead>
            <tbody>{bills.map((b) => (
              <tr key={b.id}>
                <td>#{b.id} — {b.billMonth}/{b.billYear}</td>
                <td>{b.tenant?.name}</td>
                <td className="num">₹{b.totalAmount}</td>
                <td className="num">₹{b.paidAmount}</td>
                <td><StatusPill status={b.status} /></td>
                <td><button className="btn btn-outline" onClick={() => addLineItem(b.id)}>Add line item</button></td>
              </tr>
            ))}</tbody>
          </table>
        )}
      </div>
      <div className="panel panel-form">
        <h3>Create a bill</h3>
        <p className="helptext">Creates a bill with one RENT line item. Add electricity/water/other charges from the table above afterward.</p>
        <form onSubmit={createBill} className="field-row" style={{ marginTop: '0.75rem' }}>
          <div className="field"><label>Unit</label>
            <select value={billForm.unitId} onChange={(e) => setBillForm({ ...billForm, unitId: e.target.value })} required>
              <option value="" disabled>Select…</option>
              {units?.map((u) => <option key={u.id} value={u.id}>{u.unitNumber}</option>)}
            </select></div>
          <div className="field"><label>Tenant</label>
            <select value={billForm.tenantId} onChange={(e) => setBillForm({ ...billForm, tenantId: e.target.value })} required>
              <option value="" disabled>Select…</option>
              {tenants?.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
            </select></div>
          <div className="field"><label>Month</label>
            <input type="number" min="1" max="12" value={billForm.billMonth} onChange={(e) => setBillForm({ ...billForm, billMonth: e.target.value })} required /></div>
          <div className="field"><label>Year</label>
            <input type="number" value={billForm.billYear} onChange={(e) => setBillForm({ ...billForm, billYear: e.target.value })} required /></div>
          <div className="field"><label>Due date</label>
            <input type="date" value={billForm.dueDate} onChange={(e) => setBillForm({ ...billForm, dueDate: e.target.value })} required /></div>
          <div className="field"><label>Rent amount (₹)</label>
            <input type="number" value={billForm.rentAmount} onChange={(e) => setBillForm({ ...billForm, rentAmount: e.target.value })} required /></div>
          <button className="btn btn-primary" type="submit">Create bill</button>
        </form>
      </div>

      {/* Rent agreements */}
      <div className="panel">
        <div className="panel-header"><h2>Rent agreements</h2></div>
        {!rentAgreements && <p className="empty-state">Loading…</p>}
        {rentAgreements && rentAgreements.length === 0 && <p className="empty-state">No agreements yet — create one below.</p>}
        {rentAgreements && rentAgreements.length > 0 && (
          <table>
            <thead><tr><th>Tenant</th><th>Base rent</th><th>Escalation</th><th>Next change</th><th>Active</th></tr></thead>
            <tbody>{rentAgreements.map((a) => (
              <tr key={a.id}>
                <td>{a.tenant?.name}</td>
                <td className="num">₹{a.baseRent}</td>
                <td className="num">{a.escalationPercent}% → ₹{a.nextRentAmount}</td>
                <td>{a.nextEscalationDate}</td>
                <td>{a.isActive ? 'Yes' : 'No'}</td>
              </tr>
            ))}</tbody>
          </table>
        )}
      </div>
      <div className="panel panel-form">
        <h3>Create a rent agreement</h3>
        <form onSubmit={createAgreement} className="field-row" style={{ marginTop: '0.75rem' }}>
          <div className="field"><label>Unit</label>
            <select value={agreementForm.unitId} onChange={(e) => setAgreementForm({ ...agreementForm, unitId: e.target.value })} required>
              <option value="" disabled>Select…</option>
              {units?.map((u) => <option key={u.id} value={u.id}>{u.unitNumber}</option>)}
            </select></div>
          <div className="field"><label>Tenant</label>
            <select value={agreementForm.tenantId} onChange={(e) => setAgreementForm({ ...agreementForm, tenantId: e.target.value })} required>
              <option value="" disabled>Select…</option>
              {tenants?.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
            </select></div>
          <div className="field"><label>Base rent (₹)</label>
            <input type="number" value={agreementForm.baseRent} onChange={(e) => setAgreementForm({ ...agreementForm, baseRent: e.target.value })} required /></div>
          <div className="field"><label>Escalation %</label>
            <input type="number" value={agreementForm.escalationPercent} onChange={(e) => setAgreementForm({ ...agreementForm, escalationPercent: e.target.value })} required /></div>
          <div className="field"><label>Escalation day of year</label>
            <input type="number" min="1" max="366" value={agreementForm.escalationDayOfYear} onChange={(e) => setAgreementForm({ ...agreementForm, escalationDayOfYear: e.target.value })} required /></div>
          <div className="field"><label>Start date</label>
            <input type="date" value={agreementForm.startDate} onChange={(e) => setAgreementForm({ ...agreementForm, startDate: e.target.value })} required /></div>
          <button className="btn btn-primary" type="submit">Create agreement</button>
        </form>
      </div>
    </Layout>
  );
}
