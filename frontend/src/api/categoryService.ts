import { apiClient } from "./apiClient";

export const categoryService = {

    getAll: (params?: any) => apiClient.get('/v1/categories', { params })
        .then(res => res as any),

    create: (categoryData: any) => apiClient.post("/v1/categories", categoryData),

    update: (id: string, categoryData: any) => apiClient.put(`/v1/categories/${id}`, categoryData),

    delete: (id: string) => apiClient.delete(`/v1/categories/${id}`),

    getCategoryById: (id: string) => apiClient.get(`/v1/categories/${id}`),

    // getCategoriesById: (ids: string) => apiClient.get('/v1/categories/user', { params: { ids } }),

    archive: (id: string) => apiClient.patch(`/v1/categories/${id}/archive`),

    unarchive: (id: string) => apiClient.patch(`/v1/categories/${id}/unarchive`),

    increaseUsage: (id: string) => apiClient.patch(`/v1/categories/${id}/increase-usage`),
};