export interface Transaction {
    id: string;
    title: string;
    amount: number;
    date: string;
    category: string;
    type: 'income' | 'expense';
    note?: string; // Dấu ? nghĩa là có thể có hoặc không
}