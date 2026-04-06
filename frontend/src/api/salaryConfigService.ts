import { apiClient } from './apiClient';

export const salaryConfigService = {
    getAll: () => apiClient.get('/salaryconfigs'),

    create: async (salaryConfigData: any) => { await apiClient("/salaryconfigs", salaryConfigData) },

    update: async (id: string, salaryConfigData: any) => await apiClient(`/salaryconfigs/${id}`, salaryConfigData),

    delete: async (id: string) => {
        await apiClient(`/salaryconfigs/${id}`, {
            method: "DELETE"
        });
    },
};