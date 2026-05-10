import { apiClient } from './apiClient';

// --- ĐỊNH NGHĨA KIỂU DỮ LIỆU (Khớp với Backend) ---
export interface ChartDataPoint {
    label: string;
    amount: number;
    forecast: boolean;
}

export interface DashboardForecastResponse {
    currency?: string;
    currentBalance?: number;
    chartData: ChartDataPoint[];
    aiAnalysis: string;
    percentageChange?: string; // Trong ảnh Postman không thấy field này
}
export interface MonthlyDataPoint {
    label: string;
    amount: number;
    forecast: boolean;
}

export interface CategoryDataPoint {
    categoryName: string;
    amount: number;
    percentage: number;
    color: string;
}



export interface DashboardAnalyticsResponse {
    categoryDistribution: {
        totalExpense: number;
        details: CategoryDataPoint[];
    };
}

export interface ApiResponse<T> {
    success: boolean; // Sửa từ code thành success
    data: T;
}

export const dashboardService = {
    // Gọi Endpoint: /api/v1/dashboard/chart/2026
    getForecast: async (year: number, type: 'expense' | 'income' = 'expense'): Promise<DashboardForecastResponse> => {
        const url = `/v1/dashboard/${type}/chart/${year}`;
        const response = await apiClient.get(url);

        const result = response.data?.data || response.data;
        if (!result) throw new Error("Forecast data empty");
        return result;
    },

    // FE truyền mặc định month = 0 để BE lấy dữ liệu nguyên năm
    getAnalytics: async (year: number, month: number = 0): Promise<DashboardAnalyticsResponse> => {
        // month = 0 nghĩa là lấy cả năm
        const url = `/v1/dashboard/analytics/chart/${year}?monthsToLookBack=${month}`;
        const response = await apiClient.get(url);

        const result = response.data?.data || response.data;
        if (!result) throw new Error("Analytics data empty");
        return result;
    }
};