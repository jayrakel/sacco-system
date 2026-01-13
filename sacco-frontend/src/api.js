import axios from 'axios';

// ✅ Config: Point to Port 8082 - baseURL without /api (endpoints will include it)
const API_URL = 'http://localhost:8082';

const api = axios.create({
    baseURL: API_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// 1. Request Interceptor: Attach Token
api.interceptors.request.use(
    (config) => {
        // ✅ Get token directly from localStorage
        const token = localStorage.getItem('sacco_token');

        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        return config;
    },
    (error) => Promise.reject(error)
);

// 2. Response Interceptor: Handle Errors (401/403)
api.interceptors.response.use(
    (response) => response,
    (error) => {
        const originalRequest = error.config;

        if (error.response) {
            // 401: Unauthorized (Token Expired)
            if (error.response.status === 401 && !originalRequest._retry) {
                if (!window.location.pathname.includes('/login')) {
                    console.warn("Session expired. Logging out...");
                    localStorage.removeItem('sacco_user');
                    localStorage.removeItem('sacco_token');
                    window.location.href = '/login';
                }
            }

            // 403: Forbidden (Role Mismatch)
            if (error.response.status === 403) {
                console.error("Access Denied: You do not have permission.");
            }
        }

        return Promise.reject(error);
    }
);

export default api;