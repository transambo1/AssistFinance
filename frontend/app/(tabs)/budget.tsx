import React, { useState } from 'react';
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
import { formatMoney } from '../../src/utils/formatters';

// IMPORT TYPES
import { Budget } from '../../src/types';

interface BudgetResponse extends Budget {
    categoryName?: string;
    icon?: string;
    status: 'ACTIVE' | 'COMPLETED' | 'CANCELLED' | 'EXCEEDED';
    isActive: boolean;
}

type TabStatus = 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

export default function BudgetScreen() {
    const router = useRouter();
    const queryClient = useQueryClient();
    const { isDark, colors } = useTheme();
    const styles = getStyles(colors, isDark);

    const [activeTab, setActiveTab] = useState<TabStatus>('ACTIVE');

    // --- STATE CHO MODAL CHỈNH SỬA ---
    const [isEditModalVisible, setEditModalVisible] = useState(false);
    const [editingBudget, setEditingBudget] = useState<Partial<BudgetResponse>>({});

    // 1. QUERY: Lấy danh sách ngân sách
    const {
        data: budgets = [] as BudgetResponse[],
        isLoading,
        isRefetching,
        refetch
    } = useQuery<BudgetResponse[]>({
        queryKey: ['budgets'],
        queryFn: async () => {
            const response = await budgetService.getAll() as any;
            return response?.data?.data || response?.data?.items || response?.data || [];
        },
    });

    // 2. MUTATION: Cập nhật ngân sách
    const updateMutation = useMutation({
        mutationFn: (data: any) => budgetService.update(editingBudget.id!, data),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['budgets'] });
            setEditModalVisible(false);
            Alert.alert('Thành công', 'Đã cập nhật ngân sách');
        },
        onError: () => Alert.alert('Lỗi', 'Không thể cập nhật ngân sách')
    });

    // 3. MUTATION: Xóa ngân sách
    const deleteMutation = useMutation({
        mutationFn: (id: string) => budgetService.delete(id),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['budgets'] });
            Alert.alert('Thành công', 'Đã xóa ngân sách');
        }
    });

    // 4. MUTATION: Bật/Tắt trạng thái nhanh
    const toggleStatusMutation = useMutation({
        mutationFn: ({ id, currentStatus }: { id: string, currentStatus: string }) => {
            return currentStatus === 'ACTIVE' ? budgetService.deactivate(id) : budgetService.activate(id);
        },
        onSuccess: () => queryClient.invalidateQueries({ queryKey: ['budgets'] })
    });

    const handleMoreOptions = (item: BudgetResponse) => {
        Alert.alert(
            'Tùy chọn',
            `Ngân sách: ${item.name || item.categoryName}`,
            [
                {
                    text: 'Chỉnh sửa',
                    onPress: () => {
                        setEditingBudget({ ...item });
                        setEditModalVisible(true);
                    }
                },
                {
                    text: item.status === 'ACTIVE' ? 'Tạm tắt ngân sách' : 'Bật lại ngân sách',
                    onPress: () => toggleStatusMutation.mutate({ id: item.id, currentStatus: item.status || 'ACTIVE' })
                },
                {
                    text: 'Xóa',
                    style: 'destructive',
                    onPress: () => {
                        Alert.alert('Xác nhận', 'Bạn có chắc chắn muốn xóa ngân sách này?', [
                            { text: 'Hủy', style: 'cancel' },
                            { text: 'Xóa', style: 'destructive', onPress: () => deleteMutation.mutate(item.id) }
                        ]);
                    }
                },
                { text: 'Hủy', style: 'cancel' }
            ]
        );
    };

    const handleSaveEdit = () => {
        const payload = {
            name: editingBudget.name,
            targetAmount: Number(editingBudget.targetAmount),
            currentAmount: Number(editingBudget.currentAmount),
            type: editingBudget.type,
            categoryId: editingBudget.categoryId,
            isActive: editingBudget.isActive,
        };
        updateMutation.mutate(payload);
    };

    // --- LOGIC LỌC VÀ CHIA SECTION ---
    const filteredBudgets = budgets.filter((b: BudgetResponse) => {
        if (activeTab === 'ACTIVE') return b.isActive && b.status !== 'COMPLETED';
        if (activeTab === 'COMPLETED') return b.status === 'COMPLETED';
        if (activeTab === 'CANCELLED') return !b.isActive || b.status === 'CANCELLED';
        return true;
    });

    const limitBudgets = filteredBudgets.filter((b: BudgetResponse) => b.type === 'LIMIT');
    const savingBudgets = filteredBudgets.filter((b: BudgetResponse) => b.type === 'SAVING');

    const sections = [];
    if (limitBudgets.length > 0) sections.push({ title: 'Chi tiết danh mục', data: limitBudgets });
    if (savingBudgets.length > 0) sections.push({ title: 'Mục tiêu tích lũy', data: savingBudgets });

    const renderBudgetCard = ({ item }: { item: BudgetResponse }) => {
        const total = item.targetAmount || 0;
        const spent = item.currentAmount || 0;
        const progress = total > 0 ? spent / total : 0;
        const isOverBudget = progress > 1;

        let progressColor = '#1A237E';
        if (item.type === 'LIMIT' && isOverBudget) progressColor = '#D32F2F';
        else if (item.type === 'SAVING' && progress >= 1) progressColor = '#4CAF50';

        const remaining = total - spent;
        let bottomSubtitleText = '';
        let subtitleColor = '#9CA3AF';

        if (item.type === 'LIMIT') {
            bottomSubtitleText = isOverBudget ? `Vượt ${formatMoney(Math.abs(remaining))}` : `Còn lại ${formatMoney(remaining)}`;
            if (isOverBudget) subtitleColor = '#D32F2F';
        } else {
            bottomSubtitleText = `Đã tích lũy ${formatMoney(spent)}`;
            if (progress >= 1) subtitleColor = '#4CAF50';
        }

        return (
            <View style={styles.card}>
                <View style={styles.cardHeaderRow}>
                    <View style={styles.cardHeaderLeft}>
                        <View style={[styles.iconBox, { backgroundColor: isDark ? '#dbdbff' : '#E8EAF6' }]}>
                            <MaterialIcons name={item.categoryIcon as any || "restaurant"} size={24} color={isDark ? '#a0abff' : (item.categoryColor || '#1a237e')} />
                        </View>
                        <View style={styles.titleWrapper}>
                            <Text style={styles.cardTitle} numberOfLines={1}>{item.name}</Text>

                            <Text style={styles.cardAmount}>{formatMoney(spent)} <Text style={styles.totalAmountText}>/ {formatMoney(total)}</Text></Text>
                            <Text style={[styles.cardSubtitle, { color: subtitleColor }]}>{bottomSubtitleText}</Text>
                        </View>
                    </View>
                    <View style={styles.cardHeaderRight}>
                        <TouchableOpacity style={styles.moreButton} onPress={() => handleMoreOptions(item)}>
                            <MaterialIcons name="more-vert" size={24} color="#9CA3AF" />
                        </TouchableOpacity>
                    </View>
                </View>
                <View style={styles.progressBarBackground}>
                    <View style={[styles.progressBarFill, { width: `${Math.min(progress * 100, 100)}%`, backgroundColor: progressColor }]} />
                </View>
                <View style={styles.cardFooterRow}>
                    <Text style={styles.limitText}>0đ</Text>
                    <Text style={styles.limitText}>{formatMoney(total)}đ</Text>
                </View>
            </View>
        );
    };

    if (isLoading) {
        return (
            <SafeAreaView style={[styles.container, styles.centerContainer]}>
                <ActivityIndicator size="large" color="#1A237E" />
                <Text style={{ marginTop: 12, color: colors.textDim }}>Đang tải ngân sách...</Text>
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
                    <TouchableOpacity key={tab} style={[styles.tabBtn, activeTab === tab && styles.tabBtnActive]} onPress={() => setActiveTab(tab)}>
                        <Text style={[styles.tabText, activeTab === tab && styles.tabTextActive]}>
                            {tab === 'ACTIVE' ? 'Đang hoạt động' : tab === 'COMPLETED' ? 'Đã kết thúc' : 'Tạm ẩn'}
                        </Text>
                    </TouchableOpacity>
                ))}
            </View>

            {sections.length === 0 ? (
                <View style={styles.centerContainer}>
                    <MaterialIcons name="account-balance-wallet" size={64} color="#9CA3AF" />
                    <Text style={styles.emptyText}>Chưa có ngân sách nào.</Text>
                </View>
            ) : (
                <SectionList
                    sections={sections}
                    keyExtractor={(item) => item.id}
                    renderItem={renderBudgetCard}
                    renderSectionHeader={({ section: { title } }) => <Text style={styles.sectionTitle}>{title}</Text>}
                    contentContainerStyle={styles.listContent}
                    stickySectionHeadersEnabled={false}
                    refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} colors={['#1A237E']} />}
                />
            )}

            {/* --- MODAL CHỈNH SỬA --- */}
            <Modal visible={isEditModalVisible} animationType="slide" transparent>
                <View style={styles.modalOverlay}>
                    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : 'height'} style={styles.modalContent}>
                        <View style={styles.modalHeader}>
                            <Text style={styles.modalTitle}>Chỉnh sửa ngân sách</Text>
                            <TouchableOpacity onPress={() => setEditModalVisible(false)}>
                                <MaterialIcons name="close" size={24} color="#1A237E" />
                            </TouchableOpacity>
                        </View>
                        <ScrollView showsVerticalScrollIndicator={false} style={{ padding: 20 }}>
                            <Text style={styles.inputLabel}>TÊN NGÂN SÁCH</Text>
                            <TextInput
                                style={styles.textInput}
                                value={editingBudget.name}
                                onChangeText={(val) => setEditingBudget({ ...editingBudget, name: val })}
                            />

                            <Text style={styles.inputLabel}>MỤC TIÊU / HẠN MỨC (VND)</Text>
                            <TextInput
                                style={styles.textInput}
                                keyboardType="numeric"
                                value={editingBudget.targetAmount?.toString()}
                                onChangeText={(val) => setEditingBudget({ ...editingBudget, targetAmount: Number(val) })}
                            />

                            <Text style={styles.inputLabel}>SỐ TIỀN HIỆN TẠI (VND)</Text>
                            <TextInput
                                style={styles.textInput}
                                keyboardType="numeric"
                                value={editingBudget.currentAmount?.toString()}
                                onChangeText={(val) => setEditingBudget({ ...editingBudget, currentAmount: Number(val) })}
                            />
                            <Text style={styles.inputLabel}>Ngày kết thúc </Text>
                            <TextInput
                                style={styles.textInput}
                                keyboardType="numeric"
                                value={editingBudget.currentAmount?.toString()}
                                onChangeText={(val) => setEditingBudget({ ...editingBudget, currentAmount: Number(val) })}
                            />
                            <View style={styles.switchRow}>
                                <Text style={styles.inputLabel}>TRẠNG THÁI HOẠT ĐỘNG</Text>
                                <Switch
                                    value={editingBudget.isActive}
                                    onValueChange={(val) => setEditingBudget({ ...editingBudget, isActive: val })}
                                    trackColor={{ false: "#767577", true: "#1A237E" }}
                                />
                            </View>

                            <TouchableOpacity style={styles.saveBtn} onPress={handleSaveEdit}>
                                {updateMutation.isPending ? <ActivityIndicator color="#fff" /> : <Text style={styles.saveBtnText}>Lưu thay đổi</Text>}
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
    centerContainer: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 20 },
    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 20, paddingTop: 16, paddingBottom: 8 },
    headerTitle: { fontSize: 28, fontWeight: 'bold', color: colors.text },
    addButton: { backgroundColor: '#1A237E', borderRadius: 25, width: 40, height: 40, justifyContent: 'center', alignItems: 'center' },
    tabsContainer: { flexDirection: 'row', paddingHorizontal: 20, paddingVertical: 12, gap: 8 },
    tabBtn: { paddingVertical: 8, paddingHorizontal: 16, borderRadius: 20, backgroundColor: 'transparent' },
    tabBtnActive: { borderBottomWidth: 2, borderBottomColor: '#1A237E', borderRadius: 0, paddingHorizontal: 8 },
    tabText: { fontSize: 15, fontWeight: '600', color: '#9CA3AF' },
    tabTextActive: { color: '#1A237E' },
    listContent: { paddingHorizontal: 20, paddingBottom: 120 },
    sectionTitle: { fontSize: 18, fontWeight: 'bold', color: colors.text, marginTop: 20, marginBottom: 16 },
    card: { backgroundColor: colors.surface, borderRadius: 24, padding: 20, marginBottom: 16, elevation: 2, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 8, borderWidth: 1, borderColor: '#F3F4F6' },
    cardHeaderRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 },
    cardHeaderLeft: { flexDirection: 'row', alignItems: 'center', flex: 1 },
    cardHeaderRight: { alignItems: 'flex-end', justifyContent: 'center' },
    iconBox: { width: 48, height: 48, borderRadius: 24, justifyContent: 'center', alignItems: 'center', marginRight: 16 },
    titleWrapper: { flex: 1, paddingRight: 8 },
    cardTitle: { fontSize: 16, fontWeight: '700', color: colors.text, marginBottom: 4 },
    cardAmount: { fontSize: 16, fontWeight: '700', color: '#1A237E', marginBottom: 4 },
    totalAmountText: { fontSize: 13, color: '#9CA3AF', fontWeight: '500' },
    cardSubtitle: { fontSize: 12, fontWeight: '500' },
    moreButton: { padding: 4 },
    progressBarBackground: { height: 8, backgroundColor: isDark ? '#333333' : '#F3F4F6', borderRadius: 4, overflow: 'hidden' },
    progressBarFill: { height: '100%', borderRadius: 4 },
    cardFooterRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 8 },
    limitText: { fontSize: 12, color: '#9CA3AF', fontWeight: '500' },
    emptyText: { fontSize: 18, fontWeight: '600', color: colors.text, marginTop: 16 },

    // --- MODAL STYLES ---
    modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'flex-end' },
    modalContent: { backgroundColor: '#fff', borderTopLeftRadius: 32, borderTopRightRadius: 32, height: '75%' },
    modalHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 24, borderBottomWidth: 1, borderBottomColor: '#F0F0F0' },
    modalTitle: { fontSize: 20, fontWeight: 'bold', color: '#1A237E' },
    inputLabel: { fontSize: 11, fontWeight: '700', color: '#9CA3AF', marginBottom: 8, marginTop: 16 },
    textInput: { backgroundColor: '#F8F9FA', borderRadius: 12, padding: 16, fontSize: 16, color: '#1A237E', borderWidth: 1, borderColor: '#E8EAF6' },
    switchRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 24 },
    saveBtn: { backgroundColor: '#1A237E', paddingVertical: 18, borderRadius: 16, alignItems: 'center', marginTop: 32, marginBottom: 40 },
    saveBtnText: { color: '#fff', fontSize: 16, fontWeight: 'bold' }
});