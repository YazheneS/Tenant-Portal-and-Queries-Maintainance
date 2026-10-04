import { useEffect, useState, useCallback } from 'react';
import { useUser } from '@clerk/clerk-react';
import { useApiClient } from '../auth/apiClient';
import Layout from '../components/Layout';
import StatusPill from '../components/StatusPill';

export default function TenantDashboard() {
  const api = useApiClient();
  const { user } = useUser();
  const [passkeyStatus, setPasskeyStatus] = useState(null);

  // Clerk requires an existing credential (e.g. the email/password used at
  // signup) before a passkey can be added — this isn't passkey-only signup,
  // it's "add a faster method on top of the account you already have."
  const existingPasskeys = user?.passkeys ?? [];

  const addPasskey = async () => {
    setPasskeyStatus(null);
    try {
      await user.createPasskey();
      setPasskeyStatus({ type: 'success', message: 'Passkey added — you can use it next time you sign in.' });
    } catch (err) {
      setPasskeyStatus({ type: 'error', message: err?.errors?.[0]?.longMessage || err.message });
    }
  };

  const [queries, setQueries] = useState(null);
  const [bills, setBills] = useState(null);
  const [lineItemsByBill, setLineItemsByBill] = useState({});
  const [disputes, setDisputes] = useState(null);
  const [rentAgreements, setRentAgreements] = useState(null);
  const [error, setError] = useState(null);
  const [payingBillId, setPayingBillId] = useState(null);
  const [expandedBillId, setExpandedBillId] = useState(null);

  const [queryForm, setQueryForm] = useState({ title: '', description: '', category: 'PLUMBING', priority: 'MEDIUM' });
  const [photoFile, setPhotoFile] = useState(null);
  const [submittingQuery, setSubmittingQuery] = useState(false);

  const [disputeForm, setDisputeForm] = useState({ billId: '', lineItemId: '', reason: '' });

  const refreshAll = useCallback(() => {
    api.get('/api/tenant/maintenance-queries').then((res) => setQueries(res.data)).catch((err) => setError(err.response?.data?.error || err.message));
    api.get('/api/tenant/bills').then((res) => setBills(res.data)).catch((err) => setError(err.response?.data?.error || err.message));
    api.get('/api/tenant/disputes').then((res) => setDisputes(res.data)).catch((err) => setError(err.response?.data?.error || err.message));
    api.get('/api/tenant/rent-agreement').then((res) => setRentAgreements(res.data)).catch((err) => setError(err.response?.data?.error || err.message));
  }, [api]);

  useEffect(() => { refreshAll(); }, [refreshAll]);

  // Fetches and caches a bill's line items if not already loaded. Shared by
  // the "View items" toggle (display only) and the dispute form's line-item
  // dropdown (needs the data but shouldn't affect the expand/collapse state).
  const ensureLineItemsLoaded = useCallback(async (billId) => {
    if (!billId || lineItemsByBill[billId]) return;
    const { data } = await api.get(`/api/tenant/bills/${billId}/line-items`);
    setLineItemsByBill((prev) => ({ ...prev, [billId]: data }));
  }, [api, lineItemsByBill]);

  const toggleLineItems = async (billId) => {
    if (expandedBillId === billId) { setExpandedBillId(null); return; }
    setExpandedBillId(billId);
    await ensureLineItemsLoaded(billId);
  };

  const raiseQuery = async (e) => {
    e.preventDefault();
    setSubmittingQuery(true);
    setError(null);
    try {
      let photoUrl = null;
      if (photoFile) {
        const formData = new FormData();
        formData.append('file', photoFile);
        // No explicit Content-Type here — axios/the browser set it
        // automatically for FormData, including the required multipart
        // boundary. Setting it manually without a boundary breaks the
        // upload server-side.
        const { data } = await api.post('/api/tenant/maintenance-queries/photo', formData);
        photoUrl = data.url;
      }
      await api.post('/api/tenant/maintenance-queries', { ...queryForm, photoUrl });
      setQueryForm({ title: '', description: '', category: 'PLUMBING', priority: 'MEDIUM' });
      setPhotoFile(null);
      refreshAll();
    } catch (err) {
      setError(err.response?.data?.error || err.message);
    } finally {
      setSubmittingQuery(false);
    }
  };

  const payBill = async (bill) => {
    setPayingBillId(bill.id);
    setError(null);
    try {
      const { data: payment } = await api.post('/api/tenant/payments/order', { billId: bill.id });
      const options = {
        key: import.meta.env.VITE_RAZORPAY_KEY_ID,
        amount: Math.round(payment.amount * 100),
        currency: 'INR',
        name: 'Tenant Portal',
        description: `Bill #${bill.id} — ${bill.billMonth}/${bill.billYear}`,
        order_id: payment.razorpayOrderId,
        handler: async (response) => {
          try {
            await api.post('/api/tenant/payments/verify', {
              razorpayOrderId: response.razorpay_order_id,
              razorpayPaymentId: response.razorpay_payment_id,
              razorpaySignature: response.razorpay_signature,
            });
            refreshAll();
          } catch (err) {
            setError('Payment verification failed: ' + err.message);
          } finally {
            setPayingBillId(null);
          }
        },
        modal: { ondismiss: () => setPayingBillId(null) },
      };
      if (!window.Razorpay) {
        setError('Razorpay Checkout script did not load — check your connection.');
        setPayingBillId(null);
        return;
      }
      new window.Razorpay(options).open();
    } catch (err) {
      setError(err.response?.data?.error || err.message);
      setPayingBillId(null);
    }
  };

  const downloadReceipt = async (paymentId) => {
    const res = await api.get(`/api/tenant/payments/${paymentId}/receipt`, { responseType: 'blob' });
    const url = window.URL.createObjectURL(new Blob([res.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', `receipt-${paymentId}.pdf`);
    document.body.appendChild(link);
    link.click();
    link.remove();
  };

  const raiseDispute = async (e) => {
    e.preventDefault();
    try {
      await api.post(`/api/tenant/bills/${disputeForm.billId}/dispute`, {
        lineItemId: Number(disputeForm.lineItemId),
        reason: disputeForm.reason,
        evidencePhotoUrl: null,
      });
      setDisputeForm({ billId: '', lineItemId: '', reason: '' });
      refreshAll();
    } catch (err) {
      setError(err.response?.data?.error || err.message);
    }
  };

  const selectedBillItems = disputeForm.billId ? lineItemsByBill[disputeForm.billId] : null;

  return (
      <Layout title="My dashboard">
        {error && <div className="error-banner">{error}</div>}

        {/* Passkey enrollment */}
        <div className="panel">
          <div className="panel-header"><h2>Sign-in method</h2></div>
          <p className="helptext" style={{ marginBottom: '0.6rem' }}>
            Add a passkey (Face ID, Windows Hello, fingerprint) for faster sign-in next time.
            Your existing email/password still works as a fallback.
          </p>
          {existingPasskeys.length > 0 && (
              <p className="empty-state" style={{ marginBottom: '0.5rem' }}>
                {existingPasskeys.length} passkey{existingPasskeys.length > 1 ? 's' : ''} registered on this account.
              </p>
          )}
          <button className="btn btn-primary" onClick={addPasskey}>Set up a passkey</button>
          {passkeyStatus && (
              <p className={passkeyStatus.type === 'error' ? 'helptext' : 'empty-state'}
                 style={{ marginTop: '0.5rem', color: passkeyStatus.type === 'error' ? 'var(--danger)' : 'var(--success)' }}>
                {passkeyStatus.message}
              </p>
          )}
        </div>

        {/* Maintenance queries */}
        <div className="panel">
          <div className="panel-header"><h2>Your maintenance queries</h2></div>
          {!queries && <p className="empty-state">Loading…</p>}
          {queries && queries.length === 0 && <p className="empty-state">Nothing raised yet — use the form below if something needs fixing.</p>}
          {queries && queries.length > 0 && (
              <table>
                <thead><tr><th>Title</th><th>Category</th><th>Priority</th><th>Status</th></tr></thead>
                <tbody>{queries.map((q) => (
                    <tr key={q.id}><td>{q.title}</td><td>{q.category}</td><td>{q.priority}</td><td><StatusPill status={q.status} /></td></tr>
                ))}</tbody>
              </table>
          )}
        </div>
        <div className="panel panel-form">
          <h3>Raise a maintenance query</h3>
          <form onSubmit={raiseQuery} className="field-row" style={{ marginTop: '0.75rem' }}>
            <div className="field"><label>Title</label>
              <input value={queryForm.title} onChange={(e) => setQueryForm({ ...queryForm, title: e.target.value })} required /></div>
            <div className="field"><label>Description</label>
              <input value={queryForm.description} onChange={(e) => setQueryForm({ ...queryForm, description: e.target.value })} required /></div>
            <div className="field"><label>Category</label>
              <select value={queryForm.category} onChange={(e) => setQueryForm({ ...queryForm, category: e.target.value })}>
                <option value="PLUMBING">Plumbing</option>
                <option value="ELECTRICAL">Electrical</option>
                <option value="CARPENTRY">Carpentry</option>
                <option value="OTHER">Other</option>
              </select></div>
            <div className="field"><label>Priority</label>
              <select value={queryForm.priority} onChange={(e) => setQueryForm({ ...queryForm, priority: e.target.value })}>
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </select></div>
            <div className="field"><label>Photo (optional)</label>
              <input type="file" accept="image/*" onChange={(e) => setPhotoFile(e.target.files[0])} /></div>
            <button className="btn btn-primary" type="submit" disabled={submittingQuery}>
              {submittingQuery ? 'Submitting…' : 'Raise query'}
            </button>
          </form>
        </div>

        {/* Bills */}
        <div className="panel">
          <div className="panel-header"><h2>Your bills</h2></div>
          {!bills && <p className="empty-state">Loading…</p>}
          {bills && bills.length === 0 && <p className="empty-state">No bills yet.</p>}
          {bills && bills.map((bill) => {
            const due = bill.totalAmount - bill.paidAmount;
            // bill.status is overloaded — tracks both payment lifecycle
            // (PAID/OVERDUE) and dispute lifecycle (DISPUTED), and raising a
            // dispute on an already-paid bill overwrites status away from
            // PAID even though paidAmount still correctly reflects payment.
            // So "was this bill paid" is checked via amount, not status.
            const isFullyPaid = bill.paidAmount >= bill.totalAmount;
            const items = lineItemsByBill[bill.id];
            return (
                <div key={bill.id} style={{ padding: '0.6rem 0', borderBottom: '1px solid var(--border)' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                      <strong>Bill #{bill.id}</strong> — {bill.billMonth}/{bill.billYear}{' '}
                      <span className="num">₹{bill.totalAmount} total, ₹{due} due</span>{' '}
                      <StatusPill status={bill.status} />
                    </div>
                    <div className="row-actions">
                      <button className="btn btn-outline" onClick={() => toggleLineItems(bill.id)}>
                        {expandedBillId === bill.id ? 'Hide items' : 'View items'}
                      </button>
                      {due > 0 && bill.status !== 'DISPUTED' && (
                          <button className="btn btn-accent" onClick={() => payBill(bill)} disabled={payingBillId === bill.id}>
                            {payingBillId === bill.id ? 'Processing…' : 'Pay'}
                          </button>
                      )}
                      {isFullyPaid && (
                          <button className="btn btn-outline" onClick={() => downloadReceipt(bill.id)}>Receipt</button>
                      )}
                    </div>
                  </div>
                  {expandedBillId === bill.id && items && (
                      <table style={{ marginTop: '0.5rem' }}>
                        <thead><tr><th>Type</th><th>Description</th><th>Amount</th></tr></thead>
                        <tbody>{items.map((li) => (
                            <tr key={li.id}><td>{li.type}</td><td>{li.description}</td><td className="num">₹{li.amount}</td></tr>
                        ))}</tbody>
                      </table>
                  )}
                </div>
            );
          })}
        </div>

        {/* Disputes */}
        <div className="panel">
          <div className="panel-header"><h2>Your disputes</h2></div>
          {!disputes && <p className="empty-state">Loading…</p>}
          {disputes && disputes.length === 0 && <p className="empty-state">No disputes raised.</p>}
          {disputes && disputes.map((d) => (
              <div key={d.id} style={{ padding: '0.4rem 0' }}>
                Dispute #{d.id} — {d.reason} <StatusPill status={d.status} />
                {d.ownerResponse && <div className="helptext">Owner: {d.ownerResponse}</div>}
              </div>
          ))}
        </div>
        <div className="panel panel-form">
          <h3>Dispute a charge</h3>
          <form onSubmit={raiseDispute} className="field-row" style={{ marginTop: '0.75rem' }}>
            <div className="field"><label>Bill</label>
              <select value={disputeForm.billId}
                      onChange={(e) => { setDisputeForm({ ...disputeForm, billId: e.target.value, lineItemId: '' }); ensureLineItemsLoaded(e.target.value); }}
                      required>
                <option value="" disabled>Select…</option>
                {bills?.map((b) => <option key={b.id} value={b.id}>Bill #{b.id} — {b.billMonth}/{b.billYear}</option>)}
              </select></div>
            <div className="field"><label>Line item</label>
              <select value={disputeForm.lineItemId} onChange={(e) => setDisputeForm({ ...disputeForm, lineItemId: e.target.value })} required disabled={!disputeForm.billId}>
                <option value="" disabled>Select…</option>
                {selectedBillItems?.map((li) => <option key={li.id} value={li.id}>{li.type} — ₹{li.amount}</option>)}
              </select></div>
            <div className="field"><label>Reason</label>
              <input value={disputeForm.reason} onChange={(e) => setDisputeForm({ ...disputeForm, reason: e.target.value })} required /></div>
            <button className="btn btn-primary" type="submit">Raise dispute</button>
          </form>
        </div>

        {/* Rent agreement */}
        <div className="panel">
          <div className="panel-header"><h2>Rent agreement</h2></div>
          {!rentAgreements && <p className="empty-state">Loading…</p>}
          {rentAgreements && rentAgreements.length === 0 && <p className="empty-state">No active agreement on file.</p>}
          {rentAgreements && rentAgreements.map((a) => (
              <div key={a.id} className="num">
                ₹{a.baseRent}/month, {a.escalationPercent}% escalation — next change {a.nextEscalationDate}: ₹{a.nextRentAmount}
              </div>
          ))}
        </div>
      </Layout>
  );
}