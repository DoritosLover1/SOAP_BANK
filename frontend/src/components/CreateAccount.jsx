import { useState } from 'react';

const GATEWAY_URL = 'http://localhost:8083';

function CreateAccount() {
  const [ownerName, setOwnerName] = useState('');
  const [initialBalance, setInitialBalance] = useState('');
  const [currency, setCurrency] = useState('EUR');
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setResult(null);
    setError(null);
    setLoading(true);

    try {
      const response = await fetch(`${GATEWAY_URL}/api/accounts`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          ownerName,
          initialBalance: parseFloat(initialBalance),
          currency,
        }),
      });

      const data = await response.text();

      if (response.ok) {
        setResult(data);
      } else {
        setError(data);
      }
    } catch (err) {
      setError('Sunucuya bağlanılamadı: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="form-card">
      <h2>Yeni Hesap Oluştur</h2>
      <form onSubmit={handleSubmit}>
        <label>
          Hesap Sahibi
          <input
            type="text"
            value={ownerName}
            onChange={(e) => setOwnerName(e.target.value)}
            required
          />
        </label>

        <label>
          Başlangıç Bakiyesi
          <input
            type="number"
            step="0.01"
            value={initialBalance}
            onChange={(e) => setInitialBalance(e.target.value)}
            required
          />
        </label>

        <label>
          Para Birimi
          <select value={currency} onChange={(e) => setCurrency(e.target.value)}>
            <option value="EUR">EUR</option>
            <option value="USD">USD</option>
            <option value="TRY">TRY</option>
          </select>
        </label>

        <button type="submit" disabled={loading}>
          {loading ? 'Gönderiliyor...' : 'Hesap Oluştur'}
        </button>
      </form>

      {result && (
        <div className="result success">
          Hesap oluşturuldu! Hesap Numarası: <strong>{result}</strong>
        </div>
      )}
      {error && <div className="result error">{error}</div>}
    </div>
  );
}

export default CreateAccount;