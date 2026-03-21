import React, { useState } from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet, SafeAreaView, TextInput } from 'react-native';
import { MOCK_TRANSACTIONS } from '../../src/mock/data';
import { MaterialIcons } from '@expo/vector-icons';

export default function HistoryScreen() {
    const [activeFilter, setActiveFilter] = useState('Tất cả');
    const [searchQuery, setSearchQuery] = useState(''); // Bước 1: State giữ chữ tìm kiếm

    // Bước 2: Logic lọc "Kép" (Lọc theo Chip + Lọc theo ô Tìm kiếm)
    const filteredData = MOCK_TRANSACTIONS.filter(item => {
        // Kiểm tra theo Chip (Thu nhập/Chi tiêu)
        const matchesFilter =
            activeFilter === 'Tất cả' ||
            (activeFilter === 'Thu nhập' && item.type === 'income') ||
            (activeFilter === 'Chi tiêu' && item.type === 'expense');

        // Kiểm tra theo ô Tìm kiếm (Tìm theo tên hoặc danh mục)
        const matchesSearch =
            item.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
            item.category.toLowerCase().includes(searchQuery.toLowerCase());

        return matchesFilter && matchesSearch;
    });

    const renderTransactionItem = (item: any) => (
        <TouchableOpacity key={item.id} style={styles.transactionItem}>
            <View style={styles.transactionLeft}>
                <View style={[styles.iconBox, { backgroundColor: item.color + '20' }]}>
                    <MaterialIcons name={item.icon} size={24} color={item.color} />
                </View>
                <View>
                    <Text style={styles.transactionName}>{item.name}</Text>
                    <Text style={styles.transactionCategory}>{item.category} • {item.date.split(', ')[1]}</Text>
                </View>
            </View>
            <Text style={[styles.transactionAmount, { color: item.type === 'expense' ? '#ff635f' : '#1b6d24' }]}>
                {item.type === 'expense' ? '-' : '+'}{item.amount.toLocaleString()}
            </Text>
        </TouchableOpacity>
    );

    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <Text style={styles.headerTitle}>Lịch sử giao dịch</Text>
                <TouchableOpacity style={styles.filterBtn}>
                    <MaterialIcons name="tune" size={24} color="#000666" />
                </TouchableOpacity>
            </View>

            {/* Bước 3: Kết nối TextInput với State */}
            <View style={styles.searchContainer}>
                <MaterialIcons name="search" size={24} color="#767683" />
                <TextInput
                    style={styles.searchInput}
                    placeholder="Tìm kiếm giao dịch, danh mục..."
                    placeholderTextColor="#767683"
                    value={searchQuery} // Gán giá trị từ state
                    onChangeText={(text) => setSearchQuery(text)} // Cập nhật state khi gõ
                />
                {searchQuery.length > 0 ? (
                    <TouchableOpacity onPress={() => setSearchQuery('')}>
                        <MaterialIcons name="cancel" size={20} color="#c6c5d4" />
                    </TouchableOpacity>
                ) : (
                    <TouchableOpacity>
                        <MaterialIcons name="mic" size={24} color="#000666" />
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

            <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
                {/* Nhóm ngày: Hôm nay */}
                <View style={styles.dateGroup}>
                    <Text style={styles.dateHeader}>Hôm nay, 21 Thg 3</Text>
                    {filteredData.filter(t => t.date.includes('Hôm nay')).length > 0 ? (
                        filteredData.filter(t => t.date.includes('Hôm nay')).map(renderTransactionItem)
                    ) : (
                        <Text style={styles.emptyText}>Không tìm thấy giao dịch nào</Text>
                    )}
                </View>

                {/* Nhóm ngày: Hôm qua */}
                <View style={styles.dateGroup}>
                    <Text style={styles.dateHeader}>Hôm qua, 20 Thg 3</Text>
                    {filteredData.filter(t => t.date.includes('Hôm qua')).length > 0 ? (
                        filteredData.filter(t => t.date.includes('Hôm qua')).map(renderTransactionItem)
                    ) : (
                        <Text style={styles.emptyText}>Không tìm thấy giao dịch nào</Text>
                    )}
                </View>
            </ScrollView>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#FBF8FF' },
    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 24, paddingTop: 16, paddingBottom: 16 },
    headerTitle: { fontSize: 24, fontWeight: 'bold', color: '#000666' },
    filterBtn: { padding: 8, backgroundColor: '#ffffff', borderRadius: 12, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2 },
    searchContainer: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#ffffff', marginHorizontal: 24, paddingHorizontal: 16, height: 50, borderRadius: 16, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.03, shadowRadius: 8, elevation: 1, marginBottom: 16 },
    searchInput: { flex: 1, marginLeft: 12, fontSize: 15, color: '#1b1b21' },
    chipContainer: { flexDirection: 'row', paddingHorizontal: 24, marginBottom: 16, gap: 12 },
    chip: { paddingHorizontal: 20, paddingVertical: 10, borderRadius: 20, backgroundColor: '#ffffff', borderWidth: 1, borderColor: '#e4e1ea' },
    chipActive: { backgroundColor: '#1a237e', borderColor: '#1a237e' },
    chipText: { fontSize: 14, fontWeight: '600', color: '#767683' },
    chipTextActive: { color: '#ffffff' },
    scrollContent: { paddingHorizontal: 24, paddingBottom: 120 },
    dateGroup: { marginBottom: 24 },
    dateHeader: { fontSize: 14, fontWeight: 'bold', color: '#767683', marginBottom: 12, textTransform: 'uppercase' },
    transactionItem: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', backgroundColor: '#ffffff', padding: 16, borderRadius: 16, marginBottom: 8, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.03, shadowRadius: 4, elevation: 1 },
    transactionLeft: { flexDirection: 'row', alignItems: 'center', gap: 16 },
    iconBox: { width: 48, height: 48, borderRadius: 24, alignItems: 'center', justifyContent: 'center' },
    transactionName: { fontSize: 16, fontWeight: 'bold', color: '#1b1b21', marginBottom: 4 },
    transactionCategory: { fontSize: 13, color: '#767683' },
    transactionAmount: { fontSize: 16, fontWeight: 'bold' },
    emptyText: { textAlign: 'center', color: '#767683', marginTop: 10, fontStyle: 'italic' }
});