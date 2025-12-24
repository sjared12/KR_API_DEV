import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { apiUrl } from '../../lib/api';

export default function PayStudent() {
  const { studentId } = useParams();
  const [amount, setAmount] = useState('');
  const [fee, setFee] = useState(0);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    const amt = parseFloat(amount) || 0;
    // Estimate fee client-side (2.9% + $0.30)
    const estTotal = amt ? Math.round(((amt + 0.30) / (1 - 0.029)) * 100) / 100 : 0;
    setFee(estTotal - amt);
    setTotal(estTotal);
  }, [amount]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const resp = await fetch(apiUrl('/api/square/create-checkout-link'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ amount, studentId })
      });
      const data = await resp.json();
      if (resp.ok && data.paymentLinkUrl) {
        window.location.href = data.paymentLinkUrl;
      } else {
        setError(data || 'Failed to create payment link');
      }
    } catch (err) {
      setError('Network error');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="pay-student-container">
      <h1>Student Payment</h1>
      <p>Student ID: <strong>{studentId}</strong></p>
      <form onSubmit={handleSubmit}>
        <label>
          Amount to Pay ($):
          <input
            type="number"
            min="1"
            max="10000"
            step="0.01"
            value={amount}
            onChange={e => setAmount(e.target.value)}
            required
          />
        </label>
        <div>
          <p>Estimated Square Fee: <strong>${fee.toFixed(2)}</strong></p>
          <p>Total Charged: <strong>${total.toFixed(2)}</strong></p>
        </div>
        <button type="submit" disabled={loading || !amount}>Proceed to Payment</button>
        {error && <div className="error">{error.toString()}</div>}
      </form>
    </div>
  );
}
