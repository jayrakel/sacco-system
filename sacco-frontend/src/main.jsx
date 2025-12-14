import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './index.css'
import { SettingsProvider } from './context/SettingsContext'; // Import

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <SettingsProvider>
      <App />
    </SettingsProvider>
  </React.StrictMode>,
)