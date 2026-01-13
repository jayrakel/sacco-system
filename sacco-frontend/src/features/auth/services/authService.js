import api from '../../../api';

const login = async (credentials) => {
    try {
        const response = await api.post('/auth/login', credentials);

        // Backend returns: { success: true, message: "...", data: { token: "...", ... } }
        if (response.data.success && response.data.data) {
            // ✅ We save only the inner 'data' object which contains the token and user info
            localStorage.setItem('user', JSON.stringify(response.data.data));
        }
        return response.data;
    } catch (error) {
        throw error.response ? error.response.data : { message: "Network Error" };
    }
};

const register = async (userData) => {
    try {
        const response = await api.post('/auth/register', userData);
        return response.data;
    } catch (error) {
        throw error.response ? error.response.data : { message: "Registration Failed" };
    }
};

const logout = () => {
    localStorage.removeItem('user');
    window.location.href = '/login';
};

const getCurrentUser = () => {
    try {
        const userStr = localStorage.getItem('user');
        return userStr ? JSON.parse(userStr) : null;
    } catch (e) {
        return null;
    }
};

const authService = {
    login,
    register,
    logout,
    getCurrentUser
};

export default authService;