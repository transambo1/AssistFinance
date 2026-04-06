// src/api/userService.ts
import { apiClient } from "./apiClient";
import { User } from "../types/index";

export const userService = {
    register: (userData: any) => apiClient.post<User>('/v1/auths/register', userData),

    login: (userData: any) => apiClient.post<User>('/v1/auths/login', userData),

    getProfile: () => apiClient.get<User>('/v1/users/my-info'),

    updateUserProfile: (profileData: any) =>
        apiClient.put<User>('/v1/users/update-info', profileData),
};