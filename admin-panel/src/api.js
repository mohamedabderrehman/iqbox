import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_URL || '/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
});

// Add token to requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Auth
export const adminLogin = (email, password) => 
  api.post('/admin/login', { email, password });

// Users
export const getUsers = (page = 1, limit = 20) => 
  api.get(`/admin/users?page=${page}&limit=${limit}`);

export const getUserById = (id) => 
  api.get(`/admin/users/${id}`);

export const updateUser = (id, data) => 
  api.put(`/admin/users/${id}`, data);

export const deleteUser = (id) => 
  api.delete(`/admin/users/${id}`);

// Videos
export const getVideos = (page = 1, limit = 20) => 
  api.get(`/admin/videos?page=${page}&limit=${limit}`);

export const getVideoById = (id) => 
  api.get(`/admin/videos/${id}`);

export const deleteVideo = (id) => 
  api.delete(`/admin/videos/${id}`);

// Subscription Plans
export const getPlans = () => 
  api.get('/admin/plans');

export const createPlan = (data) => 
  api.post('/admin/plans', data);

export const updatePlan = (id, data) => 
  api.put(`/admin/plans/${id}`, data);

export const deletePlan = (id) => 
  api.delete(`/admin/plans/${id}`);

// Payment Requests
export const getPaymentRequests = (status = 'all') => 
  api.get(`/admin/payments?status=${status}`);

export const approvePayment = (id) => 
  api.post(`/admin/payments/${id}/approve`);

export const rejectPayment = (id, reason) => 
  api.post(`/admin/payments/${id}/reject`, { reason });

// App Settings
export const getAppSettings = () => 
  api.get('/admin/settings');

export const updateAppSettings = (data) => 
  api.put('/admin/settings', data);

// Stats
export const getDashboardStats = () => 
  api.get('/admin/stats');

export default api;
