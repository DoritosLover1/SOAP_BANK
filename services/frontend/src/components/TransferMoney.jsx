import { useState } from 'react';

const GATEWAY_URL = 'http://localhost:8083';

function TransferMoney() {
  const [fromAccount, setFromAccount] = useState('');
  const [toAccount, setToAccount] = useState('');
  const [amount, setAmount] = useState('');
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleTransfer = async (e) => {
    e.preventDefault();
    setResult(null);
    setError(null);
    setLoading(true);

    try {
      const response = await fetch(`${GATEWAY_URL}/api/transfer`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          fromAccount,
          toAccount,
          amount: parseFloat(amount),
        }),
      });

      const data = await response.json();

      if (response.ok) {
        setResult(data);
      } else {
        setResult(data); // FAILED durumu da body ile dönüyor, onu da gösteriyoruz
      }
    } catch (err) {
      setError('Sunucuya bağlanılamadı: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="form-card">
      <h2>Para Transferi</h2>
      <form onSubmit={handleTransfer}>
        <label>
          Gönderen Hesap
          <input
            type="text"
            value={fromAccount}
            onChange={(e) => setFromAccount(e.target.value)}
            required
          />
        </label>

        <label>
          Alıcı Hesap
          <input
            type="text"
            value={toAccount}
            onChange={(e) => setToAccount(e.target.value)}
            required
          />
        </label>

        <label>
          Tutar
          <input
            type="number"
            step="0.01"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            required
          />
        </label>

        <button type="submit" disabled={loading}>
          {loading ? 'İşleniyor...' : 'Transfer Et'}
        </button>
      </form>

      {result && (
        <div className={`stamp ${result.status === 'SUCCESS' ? 'success' : 'failed'}`}>
          <div className="stamp-status">
            {result.status === 'SUCCESS' ? 'Onaylandı' : 'Reddedildi'}
          </div>
          <div className="stamp-detail">
            {result.transactionId && `${result.transactionId} · `}{result.message}
          </div>
        </div>
      )}
      {error && <div className="result error">{error}</div>}
    </div>
  );
}

export default TransferMoney;