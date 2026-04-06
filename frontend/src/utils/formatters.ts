
export const formatMoney = (amount: number) => {
    return amount.toLocaleString('vi-VN');
};

export const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN');
};