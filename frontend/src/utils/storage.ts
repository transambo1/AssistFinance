import * as SecureStore from 'expo-secure-store';

export const storage = {
    saveToken: async (token: string) => {
        try {
            await SecureStore.setItemAsync('userToken', token);
        } catch (e) {
            console.error('Lỗi khi lưu Token:', e);
        }
    },
    getToken: async () => {
        try {
            return await SecureStore.getItemAsync('userToken');
        } catch (e) {
            console.error('Lỗi khi lấy Token:', e);
            return null;
        }
    },
    removeToken: async () => {
        try {
            await SecureStore.deleteItemAsync('userToken');
        } catch (e) {
            console.error('Lỗi khi xóa Token:', e);
        }
    }
};