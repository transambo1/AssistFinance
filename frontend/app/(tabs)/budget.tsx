import React, { useState, useMemo, useEffect } from 'react';
import {
    View,
    Text,
    StyleSheet,
    TouchableOpacity,
    ActivityIndicator,
    RefreshControl,
    Alert,
    SafeAreaView,
    SectionList,
    Modal,
    TextInput,
    ScrollView,
    Switch,
    KeyboardAvoidingView,
    Platform
} from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'expo-router';

import { budgetService } from '../../src/api/budgetService';
import { useTheme } from '../../src/context/ThemeContext';
import { formatMoney, formatDate } from '../../src/utils/formatters';
import { Budget } from '../../src/types';

// Interface mở rộng để khớp với dữ liệu thực tế từ Backend
interface BudgetResponse extends Budget {
    categoryName?: string;

    status: 'ACTIVE' | 'COMPLETED' | 'CANCELLED' | 'EXCEEDED' | string;
    isActive: boolean;
    updatedAt: string;
}

type TabStatus = 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

export default function BudgetScreen() {
    const router = useRouter();
    const queryClient = useQueryClient();
    const { isDark, colors } = useTheme();
    const styles = getStyles(colors, isDark);

    const [activeTab, setActiveTab] = useState<TabStatus>('ACTIVE');
    const [isEditModalVisible, setEditModalVisible] = useState(false);
    const [editingBudget, setEditingBudget] = useState<Partial<BudgetResponse>>({});

    // --- 1. LẤY DỮ LIỆU TỪ API ---
    const {
        data: budgets = [] as BudgetResponse[],
        isLoading,
        isRefetching,
        refetch
    } = useQuery<BudgetResponse[]>({
        queryKey: ['budgets'],
        queryFn: async () => {
            const response = await budgetService.getAll() as any;
            // Tự động bóc tách theo đúng payload "data: { total: 15, data: [...] }" bạn gửi
            const result = response?.data?.data || response?.data || [];
            console.log("📊 Tổng dữ liệu thô nhận được:", result.length);
            return result;
        },
    });

    // --- 2. LOGIC LỌC TABS (Đã fix để hiện đủ dữ liệu) ---
    const filteredBudgets = useMemo(() => {
        return budgets.filter((b: BudgetResponse) => {
            // Chỉ coi là xong nếu thực sự hết tiền hoặc Status là COMPLETED
            const isFinished = b.currentAmount >= b.targetAmount || b.status === 'COMPLETED';

            if (activeTab === 'ACTIVE') {
                return b.isActive && !isFinished;
            }
            if (activeTab === 'COMPLETED') {
                return isFinished;
            }
            if (activeTab === 'CANCELLED') {
                return !b.isActive || b.status === 'CANCELLED';
            }
            return true;
        });
    }, [budgets, activeTab]);

    // --- 3. CHIA SECTION THEO LOẠI (Fix lỗi bỏ sót Type viết thường) ---
    const sections = useMemo(() => {
        const s = [];
        const limitData = filteredBudgets.filter(b => b.type?.toUpperCase() === 'LIMIT');
        const savingData = filteredBudgets.filter(b => b.type?.toUpperCase() === 'SAVING');
        const otherData = filteredBudgets.filter(b => b.type?.toUpperCase() !== 'LIMIT' && b.type?.toUpperCase() !== 'SAVING');

        if (limitData.length > 0) s.push({ title: 'Hạn mức chi tiêu', data: limitData });
        if (savingData.length > 0) s.push({ title: 'Mục tiêu tích lũy', data: savingData });
        if (otherData.length > 0) s.push({ title: 'Ngân sách khác', data: otherData });
        
        return s;
    }, [filteredBudgets]);

    // --- 4. CÁC THAO TÁC (XÓA/SỬA) ---
    const updateMutation = useMutation({
        mutationFn: (data: any) => budgetService.update(editingBudget.id!, data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['budgets'] });
            setEditModalVisible(false);
            Alert.alert('Thành công', 'Đã cập nhật ngân sách');
        }
    });

    const deleteMutation = useMutation({
        mutationFn: (id: string) => budgetService.delete(id),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['budgets'] });
            Alert.alert('Thành công', 'Đ đã xóa ngân sách');
        }
    });

    const handleMoreOptions = (item: BudgetResponse) => {
        Alert.alert(
            'Tùy chọn',
            `Ngân sách: ${item.name}`,
            [
                { text: 'Chỉnh sửa', onPress: () => { setEditingBudget({ ...item }); setEditModalVisible(true); } },
                { 
                    text: 'Xóa', 
                    style: 'destructive', 
                    onPress: () => {
                        Alert.alert('Xác nhận', 'Bạn có chắc chắn muốn xóa?', [
                            { text: 'Hủy' },
                            { text: 'Xóa', onPress: () => deleteMutation.mutate(item.id) }
                        ]);
                    } 
                },
                { text: 'Đóng', style: 'cancel' }
            ]
        );
    };

    const renderBudgetCard = ({ item }: { item: BudgetResponse }) => {
        const total = item.targetAmount || 1;
        const progress = Math.min(item.currentAmount / total, 1);
        
        return (
            <View style={styles.card}>
                <View style={styles.cardHeaderRow}>
                    <View style={styles.cardHeaderLeft}>
                        <View style={[styles.iconBox, { backgroundColor: isDark ? '#2C2C3E' : '#E8EAF6' }]}>
                            <MaterialIcons 
                                name={(item.categoryIcon as any) || "payments"} 
                                size={24} 
                                color={item.categoryColor || '#1A237E'} 
                            />
                        </View>
                        <View style={styles.titleWrapper}>
                            <Text style={styles.cardTitle} numberOfLines={1}>{item.name}</Text>
                            <Text style={styles.cardAmount}>
                                {formatMoney(item.currentAmount)} 
                                <Text style={styles.totalAmountText}> / {formatMoney(item.targetAmount)}</Text>
                            </Text>
                            {item.categoryName && <Text style={styles.categoryNameText}>{item.categoryName}</Text>}
                        </View>
                    </View>

                    {/* VIEW RIGHT: Đã fix gộp chung để nhấn được nút More */}
                    <View style={styles.cardHeaderRight}>
                        <TouchableOpacity 
                            style={styles.moreButton} 
                            onPress={() => handleMoreOptions(item)}
                            hitSlop={{ top: 20, bottom: 20, left: 20, right: 20 }}
                        >
                            <MaterialIcons name="more-vert" size={24} color="#9CA3AF" />
                        </TouchableOpacity>
                        <Text style={styles.dateText}>{formatDate(item.updatedAt)}</Text>
                    </View>
                </View>

                <View style={styles.progressBarBackground}>
                    <View style={[styles.progressBarFill, { 
                        width: `${progress * 100}%`, 
                        backgroundColor: progress >= 1 ? (item.type === 'SAVING' ? '#4CAF50' : '#D32F2F') : '#1A237E' 
                    }]} />
                </View>
                <View style={styles.cardFooterRow}>
                    <Text style={styles.limitText}>Tiến độ: {Math.round(progress * 100)}%</Text>
                    <Text style={styles.limitText}>{item.type?.toUpperCase()}</Text>
                </View>
            </View>
        );
    };

    if (isLoading) {
        return (
            <SafeAreaView style={[styles.container, styles.centerContainer]}>
                <ActivityIndicator size="large" color="#1A237E" />
                <Text style={{ marginTop: 15, color: colors.textDim }}>Đang tải dữ liệu Aeon...</Text>
            </SafeAreaView>
        );
    }

    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <Text style={styles.headerTitle}>Ngân sách</Text>
                <TouchableOpacity style={styles.addButton} onPress={() => router.push('/create-budget')}>
                    <MaterialIcons name="add" size={24} color="#FFF" />
                </TouchableOpacity>
            </View>

            <View style={styles.tabsContainer}>
                {(['ACTIVE', 'COMPLETED', 'CANCELLED'] as TabStatus[]).map((tab) => (
                    <TouchableOpacity 
                        key={tab} 
                        style={[styles.tabBtn, activeTab === tab && styles.tabBtnActive]} 
                        onPress={() => setActiveTab(tab)}
                    >
                        <Text style={[styles.tabText, activeTab === tab && styles.tabTextActive]}>
                            {tab === 'ACTIVE' ? 'Đang chạy' : tab === 'COMPLETED' ? 'Đã xong' : 'Tạm ẩn'}
                        </Text>
                    </TouchableOpacity>
                ))}
            </View>

            <SectionList
                sections={sections}
                keyExtractor={(item) => item.id}
                renderItem={renderBudgetCard}
                renderSectionHeader={({ section: { title } }) => <Text style={styles.sectionTitle}>{title}</Text>}
                contentContainerStyle={styles.listContent}
                stickySectionHeadersEnabled={false}
                refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} />}
                ListEmptyComponent={
                    <View style={styles.emptyContainer}>
                        <MaterialIcons name="layers-clear" size={60} color="#9CA3AF" />
                        <Text style={styles.emptyText}>Không tìm thấy ngân sách nào ở tab này.</Text>
                    </View>
                }
            />

            {/* MODAL SỬA NHANH */}
            <Modal visible={isEditModalVisible} animationType="slide" transparent>
                <View style={styles.modalOverlay}>
                    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={styles.modalContent}>
                        <View style={styles.modalHeader}>
                            <Text style={styles.modalTitle}>Sửa ngân sách</Text>
                            <TouchableOpacity onPress={() => setEditModalVisible(false)}>
                                <MaterialIcons name="close" size={24} color="#1A237E" />
                            </TouchableOpacity>
                        </View>
                        <ScrollView style={{ padding: 20 }}>
                            <Text style={styles.inputLabel}>TÊN NGÂN SÁCH</Text>
                            <TextInput
                                style={styles.textInput}
                                value={editingBudget.name}
                                onChangeText={(val) => setEditingBudget({ ...editingBudget, name: val })}
                            />
                            <TouchableOpacity style={styles.saveBtn} onPress={() => updateMutation.mutate(editingBudget)}>
                                <Text style={styles.saveBtnText}>CẬP NHẬT</Text>
                            </TouchableOpacity>
                        </ScrollView>
                    </KeyboardAvoidingView>
                </View>
            </Modal>
        </SafeAreaView>
    );
}

const getStyles = (colors: any, isDark: boolean) => StyleSheet.create({
    container: { flex: 1, backgroundColor: colors.background },
    centerContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 24, paddingTop: 20 },
    headerTitle: { fontSize: 32, fontWeight: 'bold', color: colors.text },
    addButton: { backgroundColor: '#1A237E', borderRadius: 15, width: 45, height: 45, justifyContent: 'center', alignItems: 'center', elevation: 4 },
    tabsContainer: { flexDirection: 'row', paddingHorizontal: 20, marginBottom: 15 },
    tabBtn: { paddingVertical: 8, paddingHorizontal: 16, marginRight: 10 },
    tabBtnActive: { borderBottomWidth: 3, borderBottomColor: '#1A237E' },
    tabText: { fontSize: 14, fontWeight: '600', color: '#9CA3AF' },
    tabTextActive: { color: '#1A237E' },
    listContent: { paddingHorizontal: 20, paddingBottom: 100 },
    sectionTitle: { fontSize: 14, fontWeight: '800', color: '#9CA3AF', textTransform: 'uppercase', marginTop: 25, marginBottom: 12, letterSpacing: 1 },
    card: { backgroundColor: colors.surface, borderRadius: 24, padding: 20, marginBottom: 16, elevation: 4, shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.1, shadowRadius: 10 },
    cardHeaderRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' },
    cardHeaderLeft: { flexDirection: 'row', alignItems: 'center', flex: 1 },
    cardHeaderRight: { alignItems: 'flex-end', justifyContent: 'space-between', height: 50, minWidth: 70 },
    iconBox: { width: 48, height: 48, borderRadius: 16, justifyContent: 'center', alignItems: 'center', marginRight: 16 },
    titleWrapper: { flex: 1 },
    cardTitle: { fontSize: 17, fontWeight: '700', color: colors.text },
    cardAmount: { fontSize: 14, fontWeight: '600', color: '#1A237E', marginTop: 4 },
    categoryNameText: { fontSize: 11, color: '#6297f3', fontWeight: '500', marginTop: 2 },
    totalAmountText: { color: '#9CA3AF', fontWeight: '400' },
    dateText: { fontSize: 9, color: '#9CA3AF', fontWeight: '500' },
    moreButton: { padding: 4 },
    progressBarBackground: { height: 8, backgroundColor: isDark ? '#333' : '#F3F4F6', borderRadius: 4, marginTop: 18 },
    progressBarFill: { height: '100%', borderRadius: 4 },
    cardFooterRow: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 8 },
    limitText: { fontSize: 10, color: '#9CA3AF', fontWeight: '600' },
    emptyContainer: { alignItems: 'center', marginTop: 100 },
    emptyText: { fontSize: 14, color: '#9CA3AF', marginTop: 10 },
    modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'flex-end' },
    modalContent: { backgroundColor: colors.surface, borderTopLeftRadius: 30, borderTopRightRadius: 30, height: '50%' },
    modalHeader: { flexDirection: 'row', justifyContent: 'space-between', padding: 24, borderBottomWidth: 1, borderBottomColor: '#EEE' },
    modalTitle: { fontSize: 20, fontWeight: 'bold', color: colors.text },
    inputLabel: { fontSize: 11, fontWeight: '700', color: '#9CA3AF', marginTop: 20, marginBottom: 8 },
    textInput: { backgroundColor: isDark ? '#1E1E2C' : '#F8F9FA', borderRadius: 12, padding: 16, color: colors.text, fontSize: 16 },
    saveBtn: { backgroundColor: '#1A237E', padding: 18, borderRadius: 16, alignItems: 'center', marginTop: 30 },
    saveBtnText: { color: '#FFF', fontWeight: 'bold', letterSpacing: 1 }
});