import React, { useState } from 'react';
import {
    View, Text, StyleSheet, FlatList,
    Alert, ActivityIndicator, SafeAreaView, 
    TouchableOpacity as RNTouchableOpacity // Đổi tên TouchableOpacity mặc định để không bị trùng
} from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';
import { useRouter, useLocalSearchParams } from 'expo-router';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

// IMPORT QUAN TRỌNG: Dùng TouchableOpacity của thư viện này để fix lỗi liệt cảm ứng nút Xóa
import { 
  GestureHandlerRootView, 
  Swipeable, 
  TouchableOpacity as GHTouchableOpacity 
} from 'react-native-gesture-handler';

import { categoryService } from '@/src/api/categoryService';
import { useTheme } from '@/src/context/ThemeContext';
import { Category } from '@/src/types/index';

export default function ManageCategoriesScreen() {
    const router = useRouter();
    // Nhận type ban đầu từ màn trước truyền sang, mặc định là EXPENSE
    const { type: initialType } = useLocalSearchParams<{ type: 'EXPENSE' | 'INCOME' }>();
    const [currentType, setCurrentType] = useState<'EXPENSE' | 'INCOME'>(initialType || 'EXPENSE');

    const { colors  } = useTheme();
    const queryClient = useQueryClient();

    // 1. Lấy danh sách danh mục THEO LOẠI ĐANG CHỌN (currentType)
    const { data: response, isLoading } = useQuery({
        queryKey: ['categories', currentType],
        queryFn: () => categoryService.getAll({ type: currentType }),
    });

    const categories: Category[] = (response as any)?.data?.data || [];

    // 2. Mutation Xóa danh mục
    const deleteMutation = useMutation({
        mutationFn: (id: string) => categoryService.delete(id),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['categories', currentType] });
            Alert.alert('Thành công', 'Đã xóa danh mục');
        },
        onError: (error) => {
            console.log("Lỗi xóa danh mục:", error);
            Alert.alert('Lỗi', 'Không thể xóa danh mục này. Có thể đã có giao dịch đang sử dụng nó!');
        }
    });

    const handleDelete = (id: string) => {
        Alert.alert('Xác nhận', 'Bạn có chắc chắn muốn xóa danh mục này?', [
            { text: 'Hủy', style: 'cancel' },
            { text: 'Xóa', style: 'destructive', onPress: () => deleteMutation.mutate(id) }
        ]);
    };

    // Render nút xóa khi kéo sang trái (Sử dụng GHTouchableOpacity)
    const renderRightActions = (id: string) => (
        <GHTouchableOpacity style={styles.deleteAction} onPress={() => handleDelete(id)}>
            <MaterialIcons name="delete-outline" size={28} color="#fff" />
            <Text style={styles.deleteText}>Xóa</Text>
        </GHTouchableOpacity>
    );

    const renderItem = ({ item }: { item: Category }) => (
        <Swipeable renderRightActions={() => renderRightActions(item.id)}>
            <View style={[styles.categoryRow, { backgroundColor: colors.surface, borderBottomColor: colors.divider }]}>
                <View style={styles.categoryInfo}>
                    <View style={[styles.iconCircle, { backgroundColor: item.color + '20' }]}>
                        <MaterialIcons name={item.icon as any} size={24} color={item.color} />
                    </View>
                    <Text style={[styles.categoryName, { color: colors.text }]}>{item.name}</Text>
                </View>
                <MaterialIcons name="drag-handle" size={20} color="#BDBDBD" />
            </View>
        </Swipeable>
    );

    return (
        <GestureHandlerRootView style={{ flex: 1 }}>
            <SafeAreaView style={[styles.container, { backgroundColor: colors.background }]}>
                {/* Header */}
                <View style={styles.header}>
                    <RNTouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
                        <MaterialIcons name="arrow-back" size={24} color={colors.text} />
                    </RNTouchableOpacity>
                    <Text style={[styles.headerTitle, { color: colors.text }]}>Quản lý danh mục</Text>
                    <RNTouchableOpacity 
                        onPress={() => router.push({ pathname: '/create-category', params: { type: currentType } })}
                        style={styles.addBtn}
                    >
                        <MaterialIcons name="add" size={28} color="#1a237e" />
                    </RNTouchableOpacity>
                </View>

                {/* --- THANH TOGGLE CHUYỂN ĐỔI THU NHẬP / CHI TIÊU --- */}
                <View style={styles.toggleContainer}>
                    <View style={styles.typeSelector}>
                        <RNTouchableOpacity
                            style={[styles.typeBtn, currentType === 'EXPENSE' && styles.typeBtnActiveExpense]}
                            onPress={() => setCurrentType('EXPENSE')}
                        >
                            <Text style={[styles.typeText, currentType === 'EXPENSE' && styles.typeTextActive]}>Chi tiêu</Text>
                        </RNTouchableOpacity>
                        <RNTouchableOpacity
                            style={[styles.typeBtn, currentType === 'INCOME' && styles.typeBtnActiveIncome]}
                            onPress={() => setCurrentType('INCOME')}
                        >
                            <Text style={[styles.typeText, currentType === 'INCOME' && styles.typeTextActive]}>Thu nhập</Text>
                        </RNTouchableOpacity>
                    </View>
                </View>

                {/* Danh sách danh mục */}
                {isLoading ? (
                    <View style={styles.centered}><ActivityIndicator size="large" color="#1a237e" /></View>
                ) : (
                    <FlatList
                        data={categories}
                        keyExtractor={(item) => item.id}
                        renderItem={renderItem}
                        contentContainerStyle={styles.list}
                        ListEmptyComponent={
                            <Text style={styles.emptyText}>Chưa có danh mục nào. Hãy nhấn {`"+"`} để thêm.</Text>
                        }
                    />
                )}
            </SafeAreaView>
        </GestureHandlerRootView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1 },
    centered: { flex: 1, justifyContent: 'center', alignItems: 'center' },
    header: { 
        flexDirection: 'row', 
        justifyContent: 'space-between', 
        alignItems: 'center', 
        paddingHorizontal: 20, 
        height: 60,
    },
    headerTitle: { fontSize: 18, fontWeight: 'bold' },
    backBtn: { padding: 8 },
    addBtn: { padding: 8 },
    
    // --- STYLES CHO TOGGLE (Sao chép từ màn add-transaction) ---
    toggleContainer: { paddingHorizontal: 20, paddingBottom: 15, borderBottomWidth: 0.5, borderBottomColor: '#EEE' },
    typeSelector: { flexDirection: 'row', backgroundColor: '#f5f2fb', borderRadius: 16, padding: 4 },
    typeBtn: { flex: 1, paddingVertical: 10, alignItems: 'center', borderRadius: 12 },
    typeBtnActiveExpense: { backgroundColor: '#ff635f' },
    typeBtnActiveIncome: { backgroundColor: '#1b6d24' },
    typeText: { fontSize: 14, fontWeight: 'bold', color: '#767683' },
    typeTextActive: { color: '#ffffff' },

    list: { paddingBottom: 40 },
    categoryRow: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 20,
        paddingVertical: 15,
        borderBottomWidth: 0.5,
    },
    categoryInfo: { flexDirection: 'row', alignItems: 'center', gap: 15 },
    iconCircle: { width: 44, height: 44, borderRadius: 22, alignItems: 'center', justifyContent: 'center' },
    categoryName: { fontSize: 16, fontWeight: '600' },
    deleteAction: {
        backgroundColor: '#FF3B30',
        justifyContent: 'center',
        alignItems: 'center',
        width: 80,
        height: '100%',
    },
    deleteText: {
        color: '#fff',
        fontSize: 12,
        fontWeight: 'bold',
        marginTop: 4
    },
    emptyText: { textAlign: 'center', marginTop: 50, color: '#767683', paddingHorizontal: 40 }
});