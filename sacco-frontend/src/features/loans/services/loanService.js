import api from '../../../api';

const loanService = {
  /**
   * Fetches dashboard data (Active loans, Eligibility)
   */
  getDashboard: async () => {
    const response = await api.get('/loans/dashboard');
    return response.data;
  },

  /**
   * Fetches available loan products
   */
  getProducts: async () => {
    const response = await api.get('/loans/products');
    return response.data;
  },

  /**
   * Submits a new loan application
   */
  applyForLoan: async (loanData) => {
    const response = await api.post('/loans/apply', loanData);
    return response.data;
  },

  /**
   * Adds a guarantor to a draft loan
   */
  addGuarantor: async (loanId, guarantorData) => {
    const response = await api.post(`/loans/${loanId}/guarantors`, guarantorData);
    return response.data;
  },

  /**
   * Finalizes the application
   */
  submitApplication: async (loanId) => {
    const response = await api.post(`/loans/${loanId}/submit`);
    return response.data;
  }
};

export default loanService;