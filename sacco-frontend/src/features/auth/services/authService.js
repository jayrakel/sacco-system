import api from '../../../api';

export const loginUser = async (credentials) => {
    // Java expects { "username": "...", "password": "..." }
    // We map the email field from the form to "username" here
    const payload = {
        username: credentials.email,
        password: credentials.password
    };

    const { data } = await api.post('/api/auth/login', payload);
    return data;
};

export const registerUser = async (userData) => {
    const { data } = await api.post('/api/auth/register', userData);
    return data;
};

export const logoutUser = () => {
    localStorage.removeItem('sacco_user');
    localStorage.removeItem('sacco_token');
};