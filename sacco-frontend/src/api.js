import axios from 'axios';

// ✅ Config: Point to Port 8082 (based on your app.log)
const API_URL = 'http://localhost:8082/api';

const api = axios.create({
    baseURL: API_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// 1. Request Interceptor: Attach Token
api.interceptors.request.use(
    (config) => {
        // We use the 'user' key because authService saves the whole object
        const userStr = localStorage.getItem('user');
        if (userStr) {
            try {
                const user = JSON.parse(userStr);
                // Extract token from the saved user object
                const token = user.token || user.data?.token;

                if (token) {
                    config.headers.Authorization = `Bearer ${token}`;
                }
            } catch (e) {
                console.error("Error parsing user session:", e);
                localStorage.removeItem('user');
            }
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
                    localStorage.removeItem('user');
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