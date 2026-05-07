import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

const TOKEN_KEY = 'userToken';

export const storage = {
    // Lưu Token
    saveToken: async (token: string) => {
        try {
            if (Platform.OS === 'web') {
                localStorage.setItem(TOKEN_KEY, token);
            } else {
                await SecureStore.setItemAsync(TOKEN_KEY, token);
            }
        } catch (e) {
            console.error('❌ Lỗi khi lưu Token:', e);
        }
    },

    // Lấy Token
    getToken: async () => {
        try {
            if (Platform.OS === 'web') {
                return localStorage.getItem(TOKEN_KEY);
            } else {
                return await SecureStore.getItemAsync(TOKEN_KEY);
            }
        } catch (e) {
            console.error('❌ Lỗi khi lấy Token:', e);
            return null;
        }
    },

    // Xóa Token (Dùng khi Logout)
    removeToken: async () => {
        try {
            if (Platform.OS === 'web') {
                localStorage.removeItem(TOKEN_KEY);
            } else {
                await SecureStore.deleteItemAsync(TOKEN_KEY);
            }
        } catch (e) {
            console.error('❌ Lỗi khi xóa Token:', e);
        }
    }
};