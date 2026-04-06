// src/api/budgetService.ts
import { apiClient } from "./apiClient";
import { Budget } from "../types/index";

export const budgetService = {

    getAll: (params?: any) =>
        apiClient.get('/v1/budgets', { params }).then(res => res as any),


    getById: (id: string) =>
        apiClient.get<Budget>(`/v1/budgets/${id}`),


    create: (budgetData: any) =>
        apiClient.post<Budget>('/v1/budgets', budgetData),


    update: (id: string, budgetData: any) =>
        apiClient.put<Budget>(`/v1/budgets/${id}`, budgetData),


    activate: (id: string) =>
        apiClient.patch<Budget>(`/v1/budgets/${id}/activate`),

    deactivate: (id: string) =>
        apiClient.patch<Budget>(`/v1/budgets/${id}/deactivate`),


    delete: (id: string) =>
        apiClient.delete<Budget>(`/v1/budgets/${id}`),
};