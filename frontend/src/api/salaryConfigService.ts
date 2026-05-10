import { apiClient } from './apiClient';

export const salaryConfigService = {
    // URL khớp với: /api/v1/transactions/auto-configs
    upsert: (payload: any) => apiClient.post('/v1/transactions/auto-configs', payload),

    // URL khớp với: /api/v1/transactions/auto-configs/{id}/toggle
    toggleActive: (id: string) => apiClient.patch(`/v1/transactions/auto-configs/${id}/toggle`),

    // URL khớp với: /api/v1/transactions/auto-configs/{id}
    delete: (id: string) => apiClient.delete(`/v1/transactions/auto-configs/${id}`),
};