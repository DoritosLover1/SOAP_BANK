import { useState } from 'react';
import CreateAccount from './components/CreateAccount';
import ViewAccount from './components/ViewAccount';
import TransferMoney from './components/TransferMoney';
import './App.css';

function App() {
  const [activeTab, setActiveTab] = useState('create');

  return (
    <div className="app-container">
      <div className="masthead">
        <span className="eyebrow">Banka A.Ş. · Şube Sistemi</span>
        <h1>İşlem Defteri</h1>
        <p className="subtitle">Hesap açma, sorgulama ve transfer kayıtları</p>
      </div>

      <div className="tabs">
        <button
          className={activeTab === 'create' ? 'active' : ''}
          onClick={() => setActiveTab('create')}
        >
          Hesap Aç
        </button>
        <button
          className={activeTab === 'view' ? 'active' : ''}
          onClick={() => setActiveTab('view')}
        >
          Sorgula
        </button>
        <button
          className={activeTab === 'transfer' ? 'active' : ''}
          onClick={() => setActiveTab('transfer')}
        >
          Transfer
        </button>
      </div>

      <div className="tab-content">
        {activeTab === 'create' && <CreateAccount />}
        {activeTab === 'view' && <ViewAccount />}
        {activeTab === 'transfer' && <TransferMoney />}
      </div>
    </div>
  );
}

export default App;