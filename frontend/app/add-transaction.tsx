import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, TextInput, ScrollView, KeyboardAvoidingView, SafeAreaView, Platform, ActivityIndicator, Alert } from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { categoryService } from '@/src/api/categoryService';
import { transactionService } from '@/src/api/transactionService'; // Import thêm service này
import { Category } from '@/src/types/index';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'; // Thêm Mutation

export default function AddTransactionScreen() {
    const router = useRouter();
    const queryClient = useQueryClient();

    const [type, setType] = useState<'EXPENSE' | 'INCOME'>('EXPENSE');
    const [amount, setAmount] = useState('');
    const [note, setNote] = useState('');
    const [selectedCategoryId, setSelectedCategoryId] = useState<string | null>(null);


    const {
        data: categoriesData,
        isLoading: isCategoriesLoading
    } = useQuery({
        queryKey: ['categories', type],
        queryFn: () => categoryService.getAll({ type: type })
    });

    const categories: Category[] = (categoriesData as any)?.data?.data || [];

    // 2. MUTATION: Logic gửi dữ liệu lên Server
    const createMutation = useMutation({
        mutationFn: (data: any) => transactionService.create(data),
        onSuccess: () => {
            // Làm mới dữ liệu ở Dashboard và Lịch sử
            queryClient.invalidateQueries({ queryKey: ['userProfile'] });
            queryClient.invalidateQueries({ queryKey: ['transactions'] });
            queryClient.invalidateQueries({ queryKey: ['budgets'] });

            Alert.alert('Thành công', 'Giao dịch đã được lưu');
            router.back();
        },
        onError: (error: any) => {
            Alert.alert('Lỗi', 'Không thể lưu giao dịch. Vui lòng thử lại.');
            console.error(error);
        }
    });

    // Hàm format số tiền khi nhập (Ví dụ: 1000 -> 1.000)
    const handleAmountChange = (text: string) => {
        const cleaned = text.replace(/\D/g, '');
        const formatted = cleaned !== '' ? Number(cleaned).toLocaleString('vi-VN') : '';
        setAmount(formatted);
    };

    const handleSaveTransaction = () => {
        // Chuyển chuỗi "1.000.000" về số 1000000
        const rawAmount = Number(amount.replace(/\D/g, ''));

        if (!rawAmount || rawAmount <= 0) {
            Alert.alert('Thông báo', 'Vui lòng nhập số tiền hợp lệ.');
            return;
        }
        if (!selectedCategoryId) {
            Alert.alert('Thông báo', 'Vui lòng chọn một danh mục.');
            return;
        }

        // Payload khớp với UpsertTransactionRequest của BE
        const payload = {
            type,
            amount: rawAmount,
            note: note.trim(),
            categoryId: selectedCategoryId,
            imageUrl: null,
            isAuto: false
        };
        console.log(payload)
        createMutation.mutate(payload);
    };

    return (
        <SafeAreaView style={styles.container}>
            <KeyboardAvoidingView
                behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
                keyboardVerticalOffset={Platform.OS === 'ios' ? 64 : 0}
                style={{ flex: 1 }}
            >
                {/* Header - GIỮ NGUYÊN */}
                <View style={styles.header}>
                    <TouchableOpacity onPress={() => router.back()} style={styles.closeBtn}>
                        <MaterialIcons name="close" size={28} color="#000666" />
                    </TouchableOpacity>
                    <Text style={styles.headerTitle}>Giao dịch mới</Text>
                    <View style={{ width: 40 }} />
                </View>

                <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
                    {/* Toggle Thu nhập / Chi tiêu - GIỮ NGUYÊN */}
                    <View style={styles.typeSelector}>
                        <TouchableOpacity
                            style={[styles.typeBtn, type === 'EXPENSE' && styles.typeBtnActiveExpense]}
                            onPress={() => {
                                setType('EXPENSE');
                                setSelectedCategoryId(null);
                            }}
                        >
                            <Text style={[styles.typeText, type === 'EXPENSE' && styles.typeTextActive]}>Chi tiêu</Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                            style={[styles.typeBtn, type === 'INCOME' && styles.typeBtnActiveIncome]}
                            onPress={() => {
                                setType('INCOME');
                                setSelectedCategoryId(null);
                            }}
                        >
                            <Text style={[styles.typeText, type === 'INCOME' && styles.typeTextActive]}>Thu nhập</Text>
                        </TouchableOpacity>
                    </View>

                    {/* Input Số tiền - GIỮ NGUYÊN */}
                    <View style={styles.inputSection}>
                        <Text style={styles.inputLabel}>Số tiền (VND)</Text>
                        <TextInput
                            style={[styles.amountInput, { color: type === 'EXPENSE' ? '#ff635f' : '#1b6d24' }]}
                            placeholder="0"
                            placeholderTextColor="#c6c5d4"
                            keyboardType="numeric"
                            value={amount}
                            onChangeText={handleAmountChange}
                        />
                    </View>

                    {/* Chọn Danh mục - GIỮ NGUYÊN */}
                    <Text style={styles.sectionTitle}>Danh mục</Text>

                    {isCategoriesLoading ? (
                        <ActivityIndicator size="small" color="#1a237e" style={{ marginTop: 20 }} />
                    ) : categories.length === 0 ? (
                        <Text style={{ color: '#767683', fontStyle: 'italic' }}>Chưa có danh mục nào cho loại này.</Text>
                    ) : (
                        <View style={styles.categoryGrid}>
                            {categories.map((cat) => (
                                <TouchableOpacity
                                    key={cat.id}
                                    style={[
                                        styles.categoryItem,
                                        selectedCategoryId === cat.id && { borderColor: cat.color, backgroundColor: cat.color + '10' }
                                    ]}
                                    onPress={() => setSelectedCategoryId(cat.id)}
                                >
                                    <View style={[styles.iconCircle, { backgroundColor: cat.color + '40' }]}>
                                        <MaterialIcons name={cat.icon as any} size={24} color={cat.color} />
                                    </View>
                                    <Text style={styles.categoryText} numberOfLines={1}>{cat.name}</Text>
                                </TouchableOpacity>
                            ))}
                            <TouchableOpacity
                                style={[styles.categoryItem, styles.addCategoryItem]}
                                onPress={() => router.push('/create-category')}
                            >
                                <View style={styles.addIconCircle}>
                                    <MaterialIcons name="add" size={24} color="#767683" />
                                </View>
                                <Text style={[styles.categoryText, { color: '#767683' }]}>Thêm mới</Text>
                            </TouchableOpacity>
                        </View>
                    )}

                    {/* Ghi chú - GIỮ NGUYÊN */}
                    <View style={styles.inputSection}>
                        <Text style={styles.inputLabel}>Ghi chú</Text>
                        <TextInput
                            style={styles.noteInput}
                            placeholder="Bạn đã chi cho việc gì?..."
                            placeholderTextColor="#767683"
                            value={note}
                            onChangeText={setNote}
                            multiline
                        />
                    </View>

                    {/* Nút Lưu - GIỮ NGUYÊN LAYOUT, THÊM LOADING */}
                    <TouchableOpacity
                        style={[styles.saveBtn, createMutation.isPending && { opacity: 0.7 }]}
                        onPress={handleSaveTransaction}
                        disabled={createMutation.isPending}
                    >
                        {createMutation.isPending ? (
                            <ActivityIndicator color="#fff" />
                        ) : (
                            <Text style={styles.saveBtnText}>Lưu giao dịch</Text>
                        )}
                    </TouchableOpacity>
                </ScrollView>
            </KeyboardAvoidingView>
        </SafeAreaView>
    );
}

// STYLES - GIỮ NGUYÊN 100% CỦA BẠN
const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#ffffff' },
    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 20, paddingTop: 10 },
    headerTitle: { fontSize: 18, fontWeight: 'bold', color: '#000666' },
    closeBtn: { padding: 8 },
    scrollContent: { padding: 24, gap: 32 },
    typeSelector: { flexDirection: 'row', backgroundColor: '#f5f2fb', borderRadius: 16, padding: 4 },
    typeBtn: { flex: 1, paddingVertical: 12, alignItems: 'center', borderRadius: 12 },
    typeBtnActiveExpense: { backgroundColor: '#ff635f' },
    typeBtnActiveIncome: { backgroundColor: '#1b6d24' },
    typeText: { fontSize: 15, fontWeight: 'bold', color: '#767683' },
    typeTextActive: { color: '#ffffff' },
    iconCircle: { width: 44, height: 44, borderRadius: 22, alignItems: 'center', justifyContent: 'center' },
    categoryText: { fontSize: 12, fontWeight: '600', color: '#454652', textAlign: 'center' },
    addCategoryItem: { borderStyle: 'dashed', borderColor: '#D1D1D1', backgroundColor: 'transparent', borderWidth: 1.5 },
    addIconCircle: { width: 44, height: 44, borderRadius: 22, backgroundColor: '#F0F0F0', alignItems: 'center', justifyContent: 'center' },
    inputSection: {
        gap: 3,
        borderWidth: 0.5,
        borderRadius: 25,
        borderColor: '#858585',
        alignItems: 'flex-start',
        paddingHorizontal: 24,
        paddingVertical: 12,
    },
    inputLabel: { fontSize: 13, fontWeight: 'bold', color: '#767683', textTransform: 'uppercase', borderBottomWidth: 1, borderBottomColor: '#858585' },
    amountInput: { fontSize: 28, fontWeight: 'bold', paddingVertical: 5, width: '100%' },
    noteInput: { backgroundColor: '#f5f2fb', borderRadius: 16, padding: 16, fontSize: 16, minHeight: 100, textAlignVertical: 'top', width: '100%' },
    sectionTitle: { fontSize: 16, fontWeight: 'bold', color: '#000666', marginBottom: -16 },
    categoryGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12, width: '100%' },
    categoryItem: { width: '30%', height: '30%', aspectRatio: 1, borderRadius: 25, borderWidth: 1, borderColor: '#f5f2fb', backgroundColor: '#f5f2fb', alignItems: 'center', justifyContent: 'center', gap: 8 },
    saveBtn: { backgroundColor: '#1a237e', paddingVertical: 18, borderRadius: 20, alignItems: 'center', shadowColor: '#1a237e', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.3, shadowRadius: 8, elevation: 4 },
    saveBtnText: { color: '#ffffff', fontSize: 16, fontWeight: 'bold' },
});