import axios from 'axios';

// Create the Axios instance
const api = axios.create({
  baseURL: 'http://localhost:8081',  // Updated to match SERVER_PORT in .env
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
      console.error("Network error - no response from server");
      if (window.location.pathname !== '/network-error') {
        window.location.href = '/network-error';
      }
      return Promise.reject(error);
    }

    const status = error.response.status;
    const currentPath = window.location.pathname;

    // Handle different HTTP status codes
    switch (status) {
      case 400:
        // Bad Request - Invalid data sent to server
        // Only redirect for critical validation errors
        console.warn("Bad request - validation error:", error.response.data);
        // Let component handle most 400s, but redirect for severe cases
        if (error.response.data?.critical) {
          window.location.href = '/bad-request';
        }
        break;

      case 401:
        // Unauthorized - Session expired or not logged in
        if (currentPath !== '/' && currentPath !== '/login' && currentPath !== '/session-expired') {
          console.warn("Session expired. Redirecting to session expired page...");
          localStorage.removeItem('sacco_user');
          localStorage.removeItem('sacco_token');
          window.location.href = '/session-expired';
        }
        break;

      case 403:
        // Forbidden - User doesn't have permission
        if (currentPath !== '/unauthorized') {
          console.warn("Access forbidden. Redirecting to unauthorized page...");
          window.location.href = '/unauthorized';
        }
        break;

      case 404:
        // Not Found - API endpoint doesn't exist
        // Only redirect for critical endpoints, not all 404s
        console.warn("Resource not found:", error.config.url);
        // Let component handle 404 errors individually
        break;

      case 500:
      case 502:
      case 503:
      case 504:
        // Server Error
        if (currentPath !== '/server-error') {
          console.error("Server error. Redirecting to error page...");
          window.location.href = '/server-error';
        }
        break;

      default:
        // Other errors - let component handle them
        console.error(`HTTP Error ${status}:`, error.response.data);
    }

    return Promise.reject(error);
  }
);

export default api;