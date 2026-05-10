import {apiClient} from './apiClient';

// --- ĐỊNH NGHĨA KIỂU DỮ LIỆU (Khớp với Backend) ---
export interface ChartDataPoint {
    label: string;
    amount: number;
    forecast: boolean; // Sửa từ isForecast thành forecast
}

export interface DashboardForecastResponse {
    // Lưu ý: Postman không thấy trả về currency và currentBalance trong cục data này
    // Nếu Backend thực sự không trả về, bạn nên để optional (?) để tránh lỗi
    currency?: string; 
    currentBalance?: number;
    chartData: ChartDataPoint[];
    aiAnalysis: string;
    percentageChange?: string; // Trong ảnh Postman không thấy field này
}

export interface ApiResponse<T> {
    success: boolean; // Sửa từ code thành success
    data: T;
}
export const dashboardService = {
    getForecast: async (year: number) => {
        const response = await apiClient.get<ApiResponse<DashboardForecastResponse>>(`/v1/dashboard/chart/${year}`);
        return response.data;
    },
};