import { useState, useEffect } from 'react'
import Login from './components/Login'
import AuditLogTable from './components/AuditLogTable'

function App() {
  const [token, setToken] = useState<string | null>(localStorage.getItem('token'))

  useEffect(() => {
    if (token) {
      localStorage.setItem('token', token)
    } else {
      localStorage.removeItem('token')
    }
  }, [token])

  return (
    <div className="min-h-screen bg-gray-50 text-gray-900 font-sans">
      {!token ? (
        <Login setToken={setToken} />
      ) : (
        <AuditLogTable token={token} logout={() => setToken(null)} />
      )}
    </div>
  )
}

export default App
