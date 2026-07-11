import { useState } from 'react';

const GATEWAY_URL = 'http://localhost:8083';

function TransactionHistory() {
  const [accountNumber, setAccountNumber] = useState('');
  const [history, setHistory] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleSearch = async (e) => {
    e.preventDefault();
    setHistory(null);
    setError(null);
    setLoading(true);

    try {
      const response = await fetch(`${GATEWAY_URL}/api/transfer/history/${accountNumber}`);

      if (response.ok) {
        const data = await response.json();
        setHistory(data);
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
      <h2>İşlem Geçmişi</h2>
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
          {loading ? 'Sorgulanıyor...' : 'Geçmişi Getir'}
        </button>
      </form>

      {history && history.length === 0 && (
        <div className="result">Bu hesaba ait işlem kaydı bulunamadı.</div>
      )}

      {history && history.length > 0 && (
        <div className="ledger-list">
          {history.map((item) => (
            <div key={item.transactionId} className={`ledger-row ${item.status === 'SUCCESS' ? 'success' : 'failed'}`}>
              <div className="ledger-row-top">
                <span className="ledger-direction">
                  {item.fromAccount === accountNumber
                    ? `→ ${item.toAccount}`
                    : `← ${item.fromAccount}`}
                </span>
                <span className="ledger-amount">{item.amount}</span>
              </div>
              <div className="ledger-row-bottom">
                <span>{item.createdAt}</span>
                <span className="ledger-status">{item.status}</span>
              </div>
            </div>
          ))}
        </div>
      )}

      {error && <div className="result error">{error}</div>}
    </div>
  );
}

export default TransactionHistory;