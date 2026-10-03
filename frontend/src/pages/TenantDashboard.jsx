import { useEffect, useState, useCallback } from 'react';
import { UserButton } from '@clerk/clerk-react';
import { useApiClient } from '../auth/apiClient';

const sectionStyle = { marginTop: '2rem', paddingTop: '1rem', borderTop: '1px solid #ddd' };

export default function TenantDashboard() {
  const api = useApiClient();
  const [queries, setQueries] = useState(null);
  const [bills, setBills] = useState(null);
  const [disputes, setDisputes] = useState(null);
  const [rentAgreements, setRentAgreements] = useState(null);
  const [error, setError] = useState(null);
  const [payingBillId, setPayingBillId] = useState(null);

  const [queryForm, setQueryForm] = useState({
    title: '', description: '', category: 'PLUMBING', priority: 'MEDIUM',
  });
  const [photoFile, setPhotoFile] = useState(null);
  const [submittingQuery, setSubmittingQuery] = useState(false);

  const refreshAll = useCallback(() => {
    api.get('/api/tenant/maintenance-queries').then((res) => setQueries(res.data)).catch((err) => setError(err.message));
    api.get('/api/tenant/bills').then((res) => setBills(res.data)).catch((err) => setError(err.message));
    api.get('/api/tenant/disputes').then((res) => setDisputes(res.data)).catch((err) => setError(err.message));
    api.get('/api/tenant/rent-agreement').then((res) => setRentAgreements(res.data)).catch((err) => setError(err.message));
  }, [api]);

  useEffect(() => { refreshAll(); }, [refreshAll]);

  // Opens Razorpay's hosted Checkout widget (loaded via index.html's
  // checkout.js script tag — not an npm package). On success, calls
  // /api/tenant/payments/verify, which does the REAL signature check
  // server-side — nothing here is trusted just because Checkout said "done."
  const payBill = async (bill) => {
    setPayingBillId(bill.id);
    setError(null);
    try {
      const { data: payment } = await api.post('/api/tenant/payments/order', { billId: bill.id });

      const options = {
        key: import.meta.env.VITE_RAZORPAY_KEY_ID,
        amount: Math.round(payment.amount * 100), // paise
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

  const raiseQuery = async (e) => {
    e.preventDefault();
    setSubmittingQuery(true);
    setError(null);
    try {
      let photoUrl = null;
      if (photoFile) {
        const formData = new FormData();
        formData.append('file', photoFile);
        // No explicit Content-Type here — axios/the browser set it automatically
        // for FormData, including the required multipart boundary. Setting it
        // manually without a boundary breaks the upload on the server side.
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

  return (
      <div style={{ padding: '2rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h1>Tenant Dashboard</h1>
          <UserButton />
        </div>

        {error && <p style={{ color: 'red' }}>Error: {error}</p>}

        <h2>Raise a Maintenance Query</h2>
        <form onSubmit={raiseQuery} style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', alignItems: 'center' }}>
          <input placeholder="Title" value={queryForm.title}
                 onChange={(e) => setQueryForm({ ...queryForm, title: e.target.value })} required />
          <input placeholder="Description" value={queryForm.description}
                 onChange={(e) => setQueryForm({ ...queryForm, description: e.target.value })} required />
          <select value={queryForm.category} onChange={(e) => setQueryForm({ ...queryForm, category: e.target.value })}>
            <option value="PLUMBING">Plumbing</option>
            <option value="ELECTRICAL">Electrical</option>
            <option value="CARPENTRY">Carpentry</option>
            <option value="OTHER">Other</option>
          </select>
          <select value={queryForm.priority} onChange={(e) => setQueryForm({ ...queryForm, priority: e.target.value })}>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="URGENT">Urgent</option>
          </select>
          <input type="file" accept="image/*" onChange={(e) => setPhotoFile(e.target.files[0])} />
          <button type="submit" disabled={submittingQuery}>
            {submittingQuery ? 'Submitting...' : 'Raise Query'}
          </button>
        </form>

        <h2 style={{ marginTop: '1.5rem' }}>Your Maintenance Queries</h2>
        {!queries && <p>Loading...</p>}
        {queries && queries.length === 0 && <p>No open requests.</p>}
        {queries && queries.length > 0 && (
            <ul>
              {queries.map((q) => <li key={q.id}>{q.title} — {q.status}</li>)}
            </ul>
        )}

        <div style={sectionStyle}>
          <h2>Your Bills</h2>
          {!bills && <p>Loading...</p>}
          {bills && bills.length === 0 && <p>No bills yet.</p>}
          {bills && bills.map((bill) => {
            const due = bill.totalAmount - bill.paidAmount;
            // bill.status is overloaded — it tracks BOTH payment lifecycle
            // (PAID/OVERDUE) and dispute lifecycle (DISPUTED), and raising a
            // dispute on an already-paid bill overwrites status away from
            // PAID even though paidAmount still correctly reflects the
            // payment. So "was this bill paid" has to be checked via the
            // amount, not the status string, or the receipt button
            // incorrectly disappears the moment a dispute gets raised.
            const isFullyPaid = bill.paidAmount >= bill.totalAmount;
            return (
                <div key={bill.id} style={{ marginBottom: '0.75rem' }}>
                  <strong>Bill #{bill.id}</strong> — {bill.billMonth}/{bill.billYear} —
                  {' '}₹{bill.totalAmount} total, ₹{due} due — <em>{bill.status}</em>
                  {due > 0 && bill.status !== 'DISPUTED' && (
                      <button
                          onClick={() => payBill(bill)}
                          disabled={payingBillId === bill.id}
                          style={{ marginLeft: '0.75rem' }}
                      >
                        {payingBillId === bill.id ? 'Processing...' : 'Pay'}
                      </button>
                  )}
                  {isFullyPaid && (
                      <button onClick={() => downloadReceipt(bill.id)} style={{ marginLeft: '0.75rem' }}>
                        Download Receipt
                      </button>
                  )}
                </div>
            );
          })}
        </div>

        <div style={sectionStyle}>
          <h2>Your Disputes</h2>
          {!disputes && <p>Loading...</p>}
          {disputes && disputes.length === 0 && <p>No disputes raised.</p>}
          {disputes && disputes.map((d) => (
              <div key={d.id}>
                Dispute #{d.id} — {d.reason} — <em>{d.status}</em>
                {d.ownerResponse && <> — Owner: {d.ownerResponse}</>}
              </div>
          ))}
        </div>

        <div style={sectionStyle}>
          <h2>Rent Agreement</h2>
          {!rentAgreements && <p>Loading...</p>}
          {rentAgreements && rentAgreements.length === 0 && <p>No active agreement on file.</p>}
          {rentAgreements && rentAgreements.map((a) => (
              <div key={a.id}>
                ₹{a.baseRent}/month, {a.escalationPercent}% escalation —
                next change {a.nextEscalationDate}: ₹{a.nextRentAmount}
              </div>
          ))}
        </div>
      </div>
  );
}