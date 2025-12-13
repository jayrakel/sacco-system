import { useState, useEffect } from 'react'
import axios from 'axios'
import './App.css' // Keep the default styling for now

function App() {
  const [members, setMembers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // 1. Define the API URL (Your Java Backend)
  const API_URL = "http://localhost:8080/api/members"

  // 2. Fetch data when the page loads
  useEffect(() => {
    fetchMembers()
  }, [])

  const fetchMembers = async () => {
    try {
      // This calls GET /api/members
      const response = await axios.get(API_URL)

      // Your backend returns { success: true, data: [...] }
      if (response.data.success) {
        setMembers(response.data.data)
      }
      setLoading(false)
    } catch (err) {
      console.error("Error fetching members:", err)
      setError("Could not connect to Sacco Backend. Is it running?")
      setLoading(false)
    }
  }

  return (
    <div className="container">
      <h1>🚀 SACCO Management System</h1>

      <div className="card">
        <h2>Member Register</h2>

        {loading && <p>Loading members...</p>}
        {error && <p style={{color: 'red'}}>{error}</p>}

        {!loading && !error && (
          <table border="1" cellPadding="10" style={{width: '100%', borderCollapse: 'collapse'}}>
            <thead>
              <tr>
                <th>Member No</th>
                <th>Name</th>
                <th>Phone</th>
                <th>Status</th>
                <th>Total Savings</th>
              </tr>
            </thead>
            <tbody>
              {members.length === 0 ? (
                <tr><td colSpan="5">No members found.</td></tr>
              ) : (
                members.map((member) => (
                  <tr key={member.id}>
                    <td>{member.memberNumber}</td>
                    <td>{member.firstName} {member.lastName}</td>
                    <td>{member.phoneNumber}</td>
                    <td>
                      <span style={{
                        backgroundColor: member.status === 'ACTIVE' ? '#d4edda' : '#f8d7da',
                        padding: '5px 10px',
                        borderRadius: '15px',
                        color: 'black'
                      }}>
                        {member.status}
                      </span>
                    </td>
                    <td><strong>KES {member.totalSavings}</strong></td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        )}
      </div>

      <div className="card">
        <button onClick={fetchMembers}>Refresh List</button>
      </div>
    </div>
  )
}

export default App