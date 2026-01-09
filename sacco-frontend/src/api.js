import axios from 'axios';

// Create the Axios instance
const api = axios.create({
  baseURL: 'http://localhost:8082',  // Updated to match SERVER_PORT in .env
  // ❌ REMOVED: headers: { 'Content-Type': 'application/json' }
  // Axios will now auto-detect content type (JSON or File) correctly.
});

// 1. Request Interceptor: Attach Token to Every Request
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('sacco_token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 2. Response Interceptor: Handle Errors and Redirects
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Network Error (no response from server)
    if (!error.response) {
      console.error("❌ Network error - no response from server");
      return Promise.reject(error);
    }

    const status = error.response.status;

    // DEBUGGING MODE: All error redirects disabled - showing errors in console
    console.error(`❌ HTTP ${status} Error:`, error.response.data);
    console.error("Full error details:", error);
    console.error("Request URL:", error.config?.url);

    // Handle different HTTP status codes
    switch (status) {
      case 400:
        console.warn("Bad request - validation error:", error.response.data);
        break;

      case 401:
        console.warn("Session expired or unauthorized - redirecting to login");
        localStorage.removeItem('sacco_token');
        localStorage.removeItem('user');
        window.location.href = '/login';
        break;

      case 403:
        console.warn("Access forbidden - checking authentication...");
        const token = localStorage.getItem('sacco_token');
        if (!token) {
          console.error("No token found - redirecting to login");
          window.location.href = '/login';
        } else {
          console.error("Token exists but access forbidden. You may not have permission for this action.");
          alert("Access Denied: You don't have permission to perform this action.");
        }
        break;

      case 404:
        console.warn("Resource not found:", error.config.url);
        break;

      case 500:
      case 502:
      case 503:
      case 504:
        console.error("Server error:", error.response.data);
        break;

      default:
        console.error(`HTTP Error ${status}:`, error.response.data);
    }

    return Promise.reject(error);
  }
);

export default api;

// =========================================================================
// ✅ NEW: Loan Module Services
// These functions use the 'api' instance above, so Auth Tokens are auto-included.
// =========================================================================

export const loanService = {
  /**
   * Fetches the dashboard data (Eligibility status + Active Loans list)
   * GET /api/loans/dashboard
   */
  getDashboard: async () => {
    const response = await api.get('/api/loans/dashboard');
    return response.data; // Returns { success: true, data: { ... } }
  },

  /**
   * Fetches available loan products for the application form
   * GET /api/loans/products
   */
  getProducts: async () => {
    const response = await api.get('/api/loans/products');
    return response.data;
  },

  /**
   * Submits a new loan application
   * POST /api/loans/apply
   */
  applyForLoan: async (loanData) => {
    const response = await api.post('/api/loans/apply', loanData);
    return response.data;
  },

  /**
   * Adds a guarantor to a draft loan
   * POST /api/loans/{loanId}/guarantors
   */
  addGuarantor: async (loanId, guarantorData) => {
    const response = await api.post(`/api/loans/${loanId}/guarantors`, guarantorData);
    return response.data;
  },

  /**
   * Finalizes the application
   * POST /api/loans/{loanId}/submit
   */
  submitApplication: async (loanId) => {
    const response = await api.post(`/api/loans/${loanId}/submit`);
    return response.data;
  }
};