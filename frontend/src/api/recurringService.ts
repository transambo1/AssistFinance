import { apiClient } from "./apiClient";

export const debtLoanService = {
    getAll: () =>
        apiClient('/debtsloans', { method: 'GET' }),

    create: async (debtLoanData: any) => {
        await apiClient("/debtsloans", {
            method: "POST",
            body: JSON.stringify(debtLoanData)
        });
    },

    update: async (id: string, debtLoanData: any) => {
        await apiClient(`/debtsloans/${id}`, {
            method: "PUT",
            body: JSON.stringify(debtLoanData)
        });
    },

    delete: async (id: string) => {
        await apiClient(`/debtsloans/${id}`, {
            method: "DELETE"
        });
    },
};