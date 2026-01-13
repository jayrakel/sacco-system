import api from '../../../api';

const login = async (credentials) => {
    try {
        const response = await api.post('/api/auth/login', credentials);

        if (response.data && response.data.token) {
            const userData = response.data;
            localStorage.setItem('sacco_user', JSON.stringify(userData));
            localStorage.setItem('sacco_token', userData.token);

            return { success: true, data: userData };
        } else {
            return { success: false, message: "Invalid response from server" };
        }
    } catch (error) {
        if (error.response?.data) {
            return error.response.data;
        }

        return { success: false, message: "Network Error" };
    }
};

const register = async (userData) => {
    try {
        const response = await api.post('/api/auth/register', userData);
        return response.data;
    } catch (error) {
        throw error.response ? error.response.data : { message: "Registration Failed" };
    }
};

const logout = () => {
    localStorage.removeItem('sacco_user');
    localStorage.removeItem('sacco_token');
    window.location.href = '/login';
};

const getCurrentUser = () => {
    try {
        const userStr = localStorage.getItem('sacco_user');
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