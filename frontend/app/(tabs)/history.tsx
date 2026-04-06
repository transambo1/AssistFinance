import React, { useState, useMemo } from 'react';
import {
    View, Text, ScrollView, TouchableOpacity, StyleSheet,
    SafeAreaView, TextInput, ActivityIndicator, RefreshControl
} from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';
import { useTheme } from '../../src/context/ThemeContext';
import { useQuery } from '@tanstack/react-query';
import { transactionService } from '../../src/api/transactionService';
import { Transaction } from '../../src/types/index';
import { router } from 'expo-router';

export default function HistoryScreen() {
    const [activeFilter, setActiveFilter] = useState('Tất cả');
    const [searchQuery, setSearchQuery] = useState('');
    const { isDark, colors } = useTheme();
    const styles = getStyles(colors, isDark);

    // 1. GỌI API LẤY DỮ LIỆU (Thêm <any> để TS không bắt bẻ cấu trúc response)
    const { data: transactionsData, isLoading, refetch, isRefetching } = useQuery<any>({
        queryKey: ['transactions'],
        queryFn: () => transactionService.getAll()
    });

    // Trỏ đúng vào mảng: Response -> data (Object phân trang) -> data (Mảng thật)
    const transactions: Transaction[] = transactionsData?.data?.data || [];

    // 2. LOGIC LỌC DỮ LIỆU
    const filteredData = useMemo(() => {
        return transactions.filter((item: Transaction) => {
            const matchesFilter =
                activeFilter === 'Tất cả' ||
                (activeFilter === 'Thu nhập' && item.type === 'INCOME') ||
                (activeFilter === 'Chi tiêu' && item.type === 'EXPENSE');

            const matchesSearch =
                item.note?.toLowerCase().includes(searchQuery.toLowerCase()) ||
                item.categoryName?.toLowerCase().includes(searchQuery.toLowerCase());

            return matchesFilter && matchesSearch;
        });
    }, [transactions, activeFilter, searchQuery]);

    // 3. HÀM NHÓM GIAO DỊCH THEO NGÀY
    const groupedTransactions = useMemo(() => {
        const groups: { [key: string]: Transaction[] } = {};

        filteredData.forEach((item: Transaction) => {
            const date = new Date(String(item.transactionDate).replace(' ', 'T'));
            const today = new Date();
            const yesterday = new Date();
            yesterday.setDate(today.getDate() - 1);

            let dateLabel = "";
            if (date.toDateString() === today.toDateString()) {
                dateLabel = `Hôm nay, ${date.getDate()} Thg ${date.getMonth() + 1}`;
            } else if (date.toDateString() === yesterday.toDateString()) {
                dateLabel = `Hôm qua, ${date.getDate()} Thg ${date.getMonth() + 1}`;
            } else {
                dateLabel = `${date.getDate()} Thg ${date.getMonth() + 1}, ${date.getFullYear()}`;
            }

            if (!groups[dateLabel]) groups[dateLabel] = [];
            groups[dateLabel].push(item);
        });
        return groups;
    }, [filteredData]);

    const renderTransactionItem = (item: Transaction) => {
        // Xác định màu nền dựa trên type và chế độ sáng/tối
        const isExpense = item.type === 'EXPENSE';
        const dynamicBg = isExpense
            ? (isDark ? '#2D1615' : '#FFF5F5') // Chi tiêu: Đỏ sẫm (Dark) / Đỏ nhạt (Light)
            : (isDark ? '#162D12' : '#F0FFF4'); // Thu nhập: Xanh đen (Dark) / Xanh nhạt (Light)

        return (
            <TouchableOpacity
                key={item.id}
                // Sử dụng mảng style, cái sau sẽ ghi đè cái trước
                style={[styles.transactionItem, { backgroundColor: dynamicBg }]}
                onPress={() => router.push({
                    pathname: '/transaction-detail',
                    params: { id: item.id } // Truyền ID qua bên kia
                })}
            >
                <View style={styles.transactionLeft}>
                    <View style={[styles.iconBox, { backgroundColor: isDark ? '#2C2C3E' : (item.categoryColor || '#1a237e') + '20' }]}>
                        <MaterialIcons
                            name={(item.categoryIcon as any) || 'receipt'}
                            size={24}
                            color={isDark ? '#a0abff' : (item.categoryColor || '#1a237e')}
                        />
                    </View>
                    <View style={{ flex: 1 }}>
                        <Text style={styles.transactionName} numberOfLines={1}>
                            {item.note || item.categoryName}
                        </Text>
                        <Text style={styles.transactionCategory}>{item.categoryName}</Text>
                    </View>
                </View>
                <View style={{ alignItems: 'flex-end' }}>
                    <Text style={styles.transactionTime}>

                        {item.transactionDate}
                    </Text>
                    <Text style={[
                        styles.transactionAmount,
                        { color: isExpense ? '#fe625c' : (isDark ? '#a0f399' : '#1b6d24') }
                    ]}>
                        {isExpense ? '-' : '+'}{item.amount.toLocaleString('vi-VN')}đ
                    </Text>
                </View>
            </TouchableOpacity>
        );
    };
    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <Text style={styles.headerTitle}>Lịch sử giao dịch</Text>
                <TouchableOpacity style={styles.filterBtn}>
                    <MaterialIcons name="tune" size={24} color={isDark ? '#ffffff' : '#000666'} />
                </TouchableOpacity>
            </View>

            <View style={styles.searchContainer}>
                <MaterialIcons name="search" size={24} color={colors.textDim} />
                <TextInput
                    style={styles.searchInput}
                    placeholder="Tìm kiếm giao dịch..."
                    placeholderTextColor={colors.textDim}
                    value={searchQuery}
                    onChangeText={setSearchQuery}
                />
                {searchQuery.length > 0 && (
                    <TouchableOpacity onPress={() => setSearchQuery('')}>
                        <MaterialIcons name="cancel" size={20} color={colors.textDim} />
                    </TouchableOpacity>
                )}
            </View>

            <View style={styles.chipContainer}>
                {['Tất cả', 'Thu nhập', 'Chi tiêu'].map((filter) => (
                    <TouchableOpacity
                        key={filter}
                        style={[styles.chip, activeFilter === filter && styles.chipActive]}
                        onPress={() => setActiveFilter(filter)}
                    >
                        <Text style={[styles.chipText, activeFilter === filter && styles.chipTextActive]}>
                            {filter}
                        </Text>
                    </TouchableOpacity>
                ))}
            </View>

            <ScrollView
                contentContainerStyle={styles.scrollContent}
                showsVerticalScrollIndicator={false}
                refreshControl={
                    <RefreshControl refreshing={isRefetching} onRefresh={refetch} tintColor="#1a237e" />
                }
            >
                {isLoading ? (
                    <ActivityIndicator size="large" color="#1a237e" style={{ marginTop: 50 }} />
                ) : Object.keys(groupedTransactions).length > 0 ? (
                    Object.keys(groupedTransactions).map((dateLabel) => (
                        <View key={dateLabel} style={styles.dateGroup}>
                            <Text style={styles.dateHeader}>{dateLabel}</Text>
                            {groupedTransactions[dateLabel].map(renderTransactionItem)}
                        </View>
                    ))
                ) : (
                    <View style={{ alignItems: 'center', marginTop: 100 }}>
                        <MaterialIcons name="history" size={64} color={colors.textDim} />
                        <Text style={styles.emptyText}>Chưa có giao dịch nào phù hợp</Text>
                    </View>
                )}
            </ScrollView>
        </SafeAreaView>
    );
}

const getStyles = (colors: any, isDark: boolean) => StyleSheet.create({
    container: { flex: 1, backgroundColor: colors.background },
    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 24, paddingTop: 16, paddingBottom: 16 },
    headerTitle: { fontSize: 24, fontWeight: 'bold', color: isDark ? '#ffffff' : '#000666' },
    filterBtn: { padding: 8, backgroundColor: colors.surface, borderRadius: 12, elevation: 2 },
    searchContainer: { flexDirection: 'row', alignItems: 'center', backgroundColor: colors.surface, marginHorizontal: 24, paddingHorizontal: 16, height: 50, borderRadius: 16, elevation: 1, marginBottom: 16 },
    searchInput: { flex: 1, marginLeft: 12, fontSize: 15, color: colors.text },
    chipContainer: { flexDirection: 'row', paddingHorizontal: 24, marginBottom: 16, gap: 12 },
    chip: { paddingHorizontal: 20, paddingVertical: 10, borderRadius: 20, backgroundColor: colors.surface, borderWidth: 1, borderColor: isDark ? '#333333' : '#e4e1ea' },
    chipActive: { backgroundColor: '#1a237e', borderColor: '#1a237e' },
    chipText: { fontSize: 14, fontWeight: '600', color: colors.textDim },
    chipTextActive: { color: '#ffffff' },
    scrollContent: { paddingHorizontal: 24, paddingBottom: 120 },
    dateGroup: { marginBottom: 24 },
    dateHeader: { fontSize: 14, fontWeight: 'bold', color: colors.textDim, marginBottom: 12, textTransform: 'uppercase' },
    transactionItem: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', backgroundColor: colors.surface, padding: 16, borderRadius: 30, marginBottom: 8, elevation: 1 },
    transactionLeft: { flexDirection: 'row', alignItems: 'center', flex: 1, gap: 16 },
    iconBox: { width: 48, height: 48, borderRadius: 24, alignItems: 'center', justifyContent: 'center' },
    transactionName: { fontSize: 16, fontWeight: 'bold', color: colors.text, marginBottom: 2 },
    transactionCategory: { fontSize: 12, color: colors.textDim },
    transactionTime: { fontSize: 12, color: colors.textDim, marginBottom: 4 },
    transactionAmount: { fontSize: 16, fontWeight: 'bold' },
    emptyText: { textAlign: 'center', color: colors.textDim, marginTop: 10, fontStyle: 'italic' }
});


