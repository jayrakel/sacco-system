import axios from 'axios';

// Create the Axios instance
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8082',
});

// 1. Request Interceptor: Attach Token to Every Request
api.interceptors.request.use(
  (config) => {
    // Check if we have a token (User might use 'user' object or just 'sacco_token')
    const token = localStorage.getItem('sacco_token');

    // Fallback: Check if token is inside a 'user' object (Common pattern)
    if (!token) {
        const userStr = localStorage.getItem('user');
        if (userStr) {
            const user = JSON.parse(userStr);
            if (user.token) config.headers['Authorization'] = `Bearer ${user.token}`;
        }
    } else {
        config.headers['Authorization'] = `Bearer ${token}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// 2. Response Interceptor: Handle Errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (!error.response) {
      console.error("❌ Network error - no response from server");
      return Promise.reject(error);
    }
    const status = error.response.status;
    console.error(`❌ HTTP ${status} Error:`, error.response.data);
    return Promise.reject(error);
  }
);

// =========================================================================
// ✅ AUTH SERVICE (Added to help fetch current user details)
// =========================================================================
export const authService = {
  login: async (credentials) => {
    const response = await api.post('/api/auth/login', credentials);
    if (response.data.token) {
        localStorage.setItem('sacco_token', response.data.token);
        localStorage.setItem('user', JSON.stringify(response.data));
    }
    return response.data;
  },
  logout: () => {
    localStorage.removeItem('sacco_token');
    localStorage.removeItem('user');
    window.location.href = '/login';
  },
  getCurrentUser: () => {
    const userStr = localStorage.getItem('user');
    return userStr ? JSON.parse(userStr) : null;
  }
};

// =========================================================================
// ✅ PAYMENT SERVICE (M-Pesa Integration)
// =========================================================================
export const paymentService = {
  /**
   * Trigger STK Push for Loan Fees
   * @param {Object} params { amount, phoneNumber, memberId (optional) }
   */
  payLoanFee: async (params) => {
    const user = authService.getCurrentUser();
    // Use passed memberId OR fallback to logged-in user ID
    const memberId = params.memberId || user?.id || user?.memberId;

    // We expect the backend to return { success: true, checkoutRequestId: "..." }
    const response = await api.post('/api/payments/mpesa/pay-fee', null, {
        params: {
            memberId: memberId,
            amount: params.amount,
            phoneNumber: params.phoneNumber,
            reference: "Loan Fee"
        }
    });
    return response.data;
  },

  /**
   * Generic Deposit STK Push
   */
  initiateDeposit: async (params) => {
    const response = await api.post('/api/payments/mpesa/stk', null, { params });
    return response.data;
  }
};

// =========================================================================
// ✅ LOAN SERVICE
// =========================================================================
export const loanService = {
  getDashboard: async () => {
    const response = await api.get('/api/loans/dashboard');
    return response.data;
  },

  getProducts: async () => {
    const response = await api.get('/api/loans/products');
    return response.data;
  },

  applyForLoan: async (loanData) => {
    // If backend requires email param, we inject it here
    const user = authService.getCurrentUser();
    const config = { params: {} };
    if (user?.email) config.params.email = user.email;

    const response = await api.post('/api/loans/apply', loanData, config);
    return response.data;
  },

  addGuarantor: async (loanId, guarantorData) => {
    const response = await api.post(`/api/loans/${loanId}/guarantors`, guarantorData);
    return response.data;
  },

  submitApplication: async (loanId) => {
    const response = await api.post(`/api/loans/${loanId}/submit`);
    return response.data;
  },

  getUserLoans: async () => {
    const response = await api.get('/api/loans/my-loans');
    return response.data;
  }
};

export default api;