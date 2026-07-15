import { useState } from 'react';

const GATEWAY_URL = 'http://localhost:8083';

function ViewAccount() {
  const [accountNumber, setAccountNumber] = useState('');
  const [account, setAccount] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleSearch = async (e) => {
    e.preventDefault();
    setAccount(null);
    setError(null);
    setLoading(true);

    try {
      const response = await fetch(`${GATEWAY_URL}/api/accounts/${accountNumber}`);

      if (response.ok) {
        const data = await response.json();
        setAccount(data);
      } else {
        const errText = await response.text();
        setError(errText);
      }
    } catch (err) {
      setError('Sunucuya bağlanılamadı: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="form-card">
      <h2>Hesap Sorgula</h2>
      <form onSubmit={handleSearch}>
        <label>
          Hesap Numarası
          <input
            type="text"
            value={accountNumber}
            onChange={(e) => setAccountNumber(e.target.value)}
            required
          />
        </label>

        <button type="submit" disabled={loading}>
          {loading ? 'Sorgulanıyor...' : 'Sorgula'}
        </button>
      </form>

      {account && (
        <div className="balance-display">
          <span className="balance-label">Bakiye</span>
          <div className="balance-amount">{account.balance} <span style={{fontSize: '18px'}}>{account.currency}</span></div>
          <div className="balance-account">Hesap No: {account.accountNumber}</div>
        </div>
      )}
      {error && <div className="result error">{error}</div>}
    </div>
  );
}

export default ViewAccount;