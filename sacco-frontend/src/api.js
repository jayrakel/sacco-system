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
      // DEBUGGING: Disabled redirect - show error in console
      // if (window.location.pathname !== '/network-error') {
      //   window.location.href = '/network-error';
      // }
      return Promise.reject(error);
    }

    const status = error.response.status;
    const currentPath = window.location.pathname;

    // DEBUGGING MODE: All error redirects disabled - showing errors in console
    console.error(`❌ HTTP ${status} Error:`, error.response.data);
    console.error("Full error details:", error);
    console.error("Request URL:", error.config?.url);
    
    // Handle different HTTP status codes
    switch (status) {
      case 400:
        // Bad Request - Invalid data sent to server
        console.warn("Bad request - validation error:", error.response.data);
        // DEBUGGING: Redirect disabled
        // if (error.response.data?.critical) {
        //   window.location.href = '/bad-request';
        // }
        break;

      case 401:
        // Unauthorized - Session expired or not logged in
        console.warn("Session expired or unauthorized:", error.response.data);
        // DEBUGGING: Redirect disabled
        // if (currentPath !== '/' && currentPath !== '/login' && currentPath !== '/session-expired') {
        //   localStorage.removeItem('sacco_user');
        //   localStorage.removeItem('sacco_token');
        //   window.location.href = '/session-expired';
        // }
        break;

      case 403:
        // Forbidden - User doesn't have permission
        console.warn("Access forbidden:", error.response.data);
        // DEBUGGING: Redirect disabled
        // const isAuthEndpoint = error.config?.url?.includes('/api/auth/');
        // if (!isAuthEndpoint && currentPath !== '/unauthorized') {
        //   window.location.href = '/unauthorized';
        // }
        break;

      case 404:
        // Not Found - API endpoint doesn't exist
        console.warn("Resource not found:", error.config.url);
        break;

      case 500:
      case 502:
      case 503:
      case 504:
        // Server Error
        console.error("Server error:", error.response.data);
        // DEBUGGING: Redirect disabled
        // if (currentPath !== '/server-error') {
        //   window.location.href = '/server-error';
        // }
        break;

      default:
        // Other errors
        console.error(`HTTP Error ${status}:`, error.response.data);
    }

    return Promise.reject(error);
  }
);

export default api;