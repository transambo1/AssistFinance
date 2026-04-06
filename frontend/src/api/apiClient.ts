import axios from 'axios';
// Import file storage của bạn vào đây (nhớ sửa lại đường dẫn cho đúng nhé)
import { storage } from '../utils/storage';

const BASE_URL = 'http://192.168.31.109:8082/api';

export const apiClient = axios.create({
    baseURL: BASE_URL,
    headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
    },
    timeout: 10000,
});

apiClient.interceptors.request.use(
    async (config) => {

        const isAuthAPI = config.url?.includes('/auths/');

        if (!isAuthAPI) {
            const token = await storage.getToken();
            console.log(" Nhét Token vào request:", token);
            if (token) {
                config.headers.Authorization = `Bearer ${token}`;
            }
        } else {
            console.log(" Đang gọi API Auth, BỎ QUA không nhét token.");
        }

        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

apiClient.interceptors.response.use(
    (response) => {
        return response.data;
    },
    (error) => {
        console.error("Lỗi API:", error.response?.data || error.message);
        const errorMessage = error.response?.data?.message || error.response?.data?.error || 'Có lỗi xảy ra từ máy chủ!';
        return Promise.reject(new Error(errorMessage));
    }
);