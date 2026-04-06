import { apiClient } from "./apiClient";
import { Transaction } from "../types/index";

// Định nghĩa interface cho phản hồi phân trang từ BE
interface PaginatedResponse<T> {
    data: {
        data: T[];
        currentPage: number;
        total: number;
        maxPage: number;
    };
    success: boolean;
}

export const transactionService = {

    getAll: (params?: any) =>
        apiClient.get<PaginatedResponse<Transaction>>('/v1/transactions', { params }),


    create: (transactionData: any) =>
        apiClient.post<any>("/v1/transactions", transactionData),


    update: (id: string, transactionData: any) =>
        apiClient.put<any>(`/v1/transactions/${id}`, transactionData),

    delete: (id: string) =>
        apiClient.delete<any>(`/v1/transactions/${id}`)
};