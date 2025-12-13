import axios from 'axios';

// Create the Axios instance
const api = axios.create({
  // Connect to your Java Backend running on Port 8080
  baseURL: 'http://localhost:8080',
  headers: {
    'Content-Type': 'application/json',
  },
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

// 2. Response Interceptor: Handle Session Expiry
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      console.warn("Session expired. Redirecting to login...");
      localStorage.removeItem('sacco_user');
      localStorage.removeItem('sacco_token');
      window.location.href = '/';
    }
    return Promise.reject(error);
  }
);

export default api;