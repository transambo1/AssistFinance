// Định nghĩa kiểu dữ liệu để TypeScript không báo lỗi
export interface Transaction {
    id: string;
    name: string;
    amount: number;
    category: string;
    type: 'income' | 'expense';
    date: string;
    icon: any;
    color: string;
}

export const USER_DATA = {
    name: "Thành no.1",
    totalBalance: 25850000,
    avatar: "https://i.pravatar.cc/150?img=11",
    monthlyIncome: 42850000,
    monthlyExpense: 17000000,
};

export const MOCK_TRANSACTIONS: Transaction[] = [
    { id: '1', name: 'Lương tháng 10', amount: 25000000, category: 'Thu nhập', type: 'income', date: 'Hôm qua, 15:00', icon: 'account-balance-wallet', color: '#1a237e' },
    { id: '2', name: 'Ăn trưa Hokkaido', amount: 450000, category: 'Ăn uống', type: 'expense', date: 'Hôm nay, 12:30', icon: 'restaurant', color: '#ff635f' },
    { id: '3', name: 'Grab đi làm', amount: 65000, category: 'Di chuyển', type: 'expense', date: 'Hôm nay, 08:15', icon: 'directions-car', color: '#1b6d24' },
    { id: '4', name: 'Tiền điện nước', amount: 1250000, category: 'Sinh hoạt', type: 'expense', date: 'Hôm qua, 09:00', icon: 'home', color: '#4c56af' },
    { id: '5', name: 'Cafe Sáng', amount: 45000, category: 'Ăn uống', type: 'expense', date: 'Hôm nay, 08:30', icon: 'local-cafe', color: '#ff635f' },
];