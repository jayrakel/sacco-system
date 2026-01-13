import api from '../../../api';

const loanService = {
  /**
   * Fetches dashboard data (Active loans, Eligibility)
   */
  getDashboard: async () => {
    // ✅ FIX: Added /api
    const response = await api.get('/api/loans/dashboard');
    return response.data;
  },

  /**
   * Fetches available loan products
   */
  getProducts: async () => {
    // ✅ FIX: Added /api
    const response = await api.get('/api/loans/products');
    return response.data;
  },

  /**
   * Submits a new loan application
   */
  applyForLoan: async (loanData) => {
    // ✅ FIX: Already had /api, kept consistent
    const response = await api.post('/api/loans/apply', loanData);
    return response.data;
  },

  /**
   * Adds a guarantor to a draft loan
   */
  addGuarantor: async (loanId, guarantorData) => {
    // ✅ FIX: Added /api
    const response = await api.post(`/api/loans/${loanId}/guarantors`, guarantorData);
    return response.data;
  },

  /**
   * Finalizes the application
   */
  submitApplication: async (loanId) => {
    // ✅ FIX: Added /api
    const response = await api.post(`/api/loans/${loanId}/submit`);
    return response.data;
  }
};

export default loanService;