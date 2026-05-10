import React, { useState, useMemo } from 'react';
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
    KeyboardAvoidingView,
    Platform,
} from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import DateTimePicker from '@react-native-community/datetimepicker';

import { budgetService } from '../../src/api/budgetService';
import { useTheme } from '../../src/context/ThemeContext';
import { formatMoney, formatDate } from '../../src/utils/formatters';
import { Budget } from '../../src/types';

interface BudgetResponse extends Budget {
    categoryName?: string;
    status: 'ACTIVE' | 'COMPLETED' | 'CANCELLED' | 'EXCEEDED' | string;
    isActive: boolean;
    updatedAt: string;
}

type TabStatus = 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

const getValidIconName = (iconName?: string) => {
    if (!iconName) return 'payments';
    const map: Record<string, string> = {
        'car': 'directions-car',
        'salary': 'attach-money',
        'gift': 'card-giftcard',
        'trending_up': 'trending-up',
        'shopping_cart': 'shopping-cart',
        'more_horiz': 'more-horiz'
    };
    return map[iconName] || iconName.replace(/_/g, '-');
};

const MAX_AMOUNT = 50000000000; // 50 tỷ

export default function BudgetScreen() {
    const router = useRouter();
    const queryClient = useQueryClient();
    const { isDark, colors } = useTheme();
    const styles = getStyles(colors, isDark);

    const [activeTab, setActiveTab] = useState<TabStatus>('ACTIVE');
    
    // States cho Edit Modal
    const [isEditModalVisible, setEditModalVisible] = useState(false);
    const [editingBudget, setEditingBudget] = useState<Partial<BudgetResponse>>({});
    const [showDatePicker, setShowDatePicker] = useState(false);

    // States cho Add Money Modal (Mới thêm)
    const [isAddMoneyVisible, setAddMoneyVisible] = useState(false);
    const [addMoneyValue, setAddMoneyValue] = useState(0);
    const [selectedItem, setSelectedItem] = useState<BudgetResponse | null>(null);

    const {
        data: budgets = [] as BudgetResponse[],
        isLoading,
        isRefetching,
        refetch
    } = useQuery<BudgetResponse[]>({
        queryKey: ['budgets'],
        queryFn: async () => {
            const response = await budgetService.getAll() as any;
            return response?.data?.data || response?.data || [];
        },
    });

    const filteredBudgets = useMemo(() => {
        return budgets.filter((b: BudgetResponse) => {
            const endDateTimestamp = b.endDate ? new Date(b.endDate).getTime() : NaN;
            const isFinished = b.currentAmount >= b.targetAmount ||
                b.status === 'COMPLETED' ||
                (b.endDate && !Number.isNaN(endDateTimestamp) && Date.now() > endDateTimestamp);
            if (activeTab === 'ACTIVE') return b.isActive && !isFinished;
            if (activeTab === 'COMPLETED') return isFinished;
            if (activeTab === 'CANCELLED') return !b.isActive || b.status === 'CANCELLED';
            return true;
        });
    }, [budgets, activeTab]);

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

    const updateMutation = useMutation({
        mutationFn: (data: { id: string, payload: any }) => budgetService.update(data.id, data.payload),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['budgets'] });
            setEditModalVisible(false);
            setAddMoneyVisible(false); // Đóng cả modal thêm tiền nếu đang mở
            Alert.alert('Thành công', 'Cập nhật thành công');
        },
        onError: () => Alert.alert('Lỗi', 'Không thể cập nhật')
    });

    const deleteMutation = useMutation({
        mutationFn: (id: string) => budgetService.delete(id),
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['budgets'] });
            Alert.alert('Thành công', 'Đã xóa ngân sách');
        }
    });

    const toggleActiveMutation = useMutation({
        mutationFn: (item: BudgetResponse) => {
            if (item.isActive) return budgetService.deactivate(item.id);
            return budgetService.activate(item.id);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['budgets'] });
        }
    });

    const handleMoreOptions = (item: BudgetResponse) => {
        Alert.alert(
            'Tùy chọn',
            `Ngân sách: ${item.name}`,
            [
                {
                    text: 'Thêm tiền',
                    onPress: () => {
                        setSelectedItem(item);
                        setAddMoneyValue(0);
                        setAddMoneyVisible(true);
                    }
                },
                { text: 'Chỉnh sửa', onPress: () => { setEditingBudget({ ...item }); setEditModalVisible(true); } },
                {
                    text: item.isActive ? 'Tạm ẩn' : 'Kích hoạt lại',
                    onPress: () => toggleActiveMutation.mutate(item)
                },
                {
                    text: 'Xóa',
                    style: 'destructive',
                    onPress: () => {
                        Alert.alert('Xác nhận', 'Xóa ngân sách này?', [
                            { text: 'Hủy' },
                            { text: 'Xóa', onPress: () => deleteMutation.mutate(item.id), style: 'destructive' }
                        ]);
                    }
                },
                { text: 'Đóng', style: 'cancel' }
            ]
        );
    };

    const handleUpdateBudget = () => {
        if (!editingBudget.targetAmount || editingBudget.targetAmount <= 0 || editingBudget.targetAmount > MAX_AMOUNT) {
            Alert.alert('Lỗi', 'Số tiền mục tiêu phải lớn hơn 0 và không được vượt quá 50 tỷ VNĐ');
            return;
        }

        if (editingBudget.endDate) {
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            const selectedEndDate = new Date(editingBudget.endDate);
            selectedEndDate.setHours(0, 0, 0, 0);

            if (selectedEndDate.getTime() < today.getTime()) {
                Alert.alert('Lỗi', 'Ngày kết thúc không được nhỏ hơn ngày hiện tại');
                return;
            }
        }

        const payload = {
            name: editingBudget.name,
            targetAmount: editingBudget.targetAmount,
            endDate: editingBudget.endDate
        };
        if (editingBudget.id) updateMutation.mutate({ id: editingBudget.id, payload });
    };

    const onDateChange = (event: any, selectedDate?: Date) => {
        if (Platform.OS === 'android') {
            setShowDatePicker(false);
        }
        if (selectedDate) {
            setEditingBudget({ ...editingBudget, endDate: selectedDate.getTime() as any });
        }
    };

    const renderBudgetCard = ({ item }: { item: BudgetResponse }) => {
        const total = item.targetAmount || 1;
        const progress = Math.min(item.currentAmount / total, 1);
        const percentage = Math.round((item.currentAmount / total) * 100);
        const isLimit = item.type?.toUpperCase() === 'LIMIT';

        return (
            <View style={[styles.card, !item.isActive && { opacity: 0.6 }]}>
                <View style={styles.cardHeaderRow}>
                    <View style={styles.cardHeaderLeft}>
                        <View style={[styles.iconBox, { backgroundColor: isDark ? '#2C2C3E' : '#E8EAF6' }]}>
                            <MaterialIcons
                                name={getValidIconName(item.categoryIcon as string) as any}
                                size={24}
                                color={item.categoryColor || '#1A237E'}
                            />
                        </View>
                        <View style={styles.titleWrapper}>
                            <Text style={styles.cardTitle} numberOfLines={1}>{item.name} {!item.isActive && '(Đã ẩn)'}</Text>
                            {isLimit && item.categoryName && (
                                <Text style={styles.categoryNameText}>{item.categoryName}</Text>
                            )}
                            <Text style={styles.cardAmount}>
                                {formatMoney(item.currentAmount)}
                                <Text style={styles.totalAmountText}> / {formatMoney(item.targetAmount)}</Text>
                            </Text>
                        </View>
                    </View>
                    <View style={styles.cardHeaderRight}>
                        <TouchableOpacity style={styles.moreButton} onPress={() => handleMoreOptions(item)}>
                            <MaterialIcons name="more-horiz" size={24} color="#9CA3AF" />
                        </TouchableOpacity>
                    </View>
                </View>

                <View style={styles.progressInfoRow}>
                    <Text style={styles.dateText}>Hết hạn: {formatDate(item.endDate)}</Text>
                    <Text style={[styles.percentageText, percentage >= 100 && isLimit ? { color: '#D32F2F' } : {}]}>
                        {percentage}%
                    </Text>
                </View>

                <View style={styles.progressBarBackground}>
                    <View style={[styles.progressBarFill, {
                        width: `${progress * 100}%`,
                        backgroundColor: progress >= 1 ? (item.type === 'SAVING' ? '#4CAF50' : '#D32F2F') : '#1A237E'
                    }]} />
                </View>
            </View>
        );
    };

    if (isLoading) return <SafeAreaView style={styles.centerContainer}><ActivityIndicator size="large" color="#1A237E" /></SafeAreaView>;

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
                refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} />}
                stickySectionHeadersEnabled={false}
            />

            {/* MODAL SỬA NGÂN SÁCH */}
            <Modal visible={isEditModalVisible} animationType="slide" transparent>
                <View style={styles.modalOverlay}>
                    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.modalContent}>
                        <View style={styles.modalHeader}>
                            <Text style={styles.modalTitle}>Sửa ngân sách</Text>
                            <TouchableOpacity onPress={() => setEditModalVisible(false)}><MaterialIcons name="close" size={24} color={colors.text} /></TouchableOpacity>
                        </View>
                        <ScrollView style={{ padding: 20 }}>
                            <Text style={styles.inputLabel}>TÊN NGÂN SÁCH</Text>
                            <TextInput style={styles.textInput} value={editingBudget.name} onChangeText={(val) => setEditingBudget({ ...editingBudget, name: val })} />

                            <Text style={styles.inputLabel}>SỐ TIỀN MỤC TIÊU</Text>
                            <TextInput
                                style={styles.textInput}
                                value={editingBudget.targetAmount ? editingBudget.targetAmount.toLocaleString('vi-VN') : ''}
                                keyboardType="numeric"
                                placeholder="Nhập số tiền"
                                placeholderTextColor="#9CA3AF"
                                onChangeText={(val) => {
                                    let num = parseFloat(val.replace(/[^0-9]/g, '')) || 0;
                                    if (num > MAX_AMOUNT) {
                                        num = MAX_AMOUNT;
                                        Alert.alert('Giới hạn', 'Số tiền tối đa là 50 tỷ VNĐ');
                                    }
                                    setEditingBudget({ ...editingBudget, targetAmount: num as any });
                                }}
                            />
                            
                            <Text style={styles.inputLabel}>NGÀY KẾT THÚC</Text>
                            <TouchableOpacity style={styles.textInput} onPress={() => setShowDatePicker(true)}>
                                <Text style={{ color: editingBudget.endDate ? colors.text : '#9CA3AF' }}>
                                    {editingBudget.endDate ? formatDate(editingBudget.endDate) : 'Chọn ngày'}
                                </Text>
                            </TouchableOpacity>

                            {showDatePicker && (
                                <View style={styles.datePickerContainer}>
                                    <DateTimePicker
                                        value={editingBudget.endDate ? new Date(editingBudget.endDate) : new Date()}
                                        mode="date"
                                        display={Platform.OS === 'ios' ? 'spinner' : 'default'}
                                        onChange={onDateChange}
                                        minimumDate={new Date()}
                                    />
                                    {Platform.OS === 'ios' && (
                                        <TouchableOpacity style={styles.doneBtn} onPress={() => setShowDatePicker(false)}>
                                            <Text style={styles.doneBtnText}>Xác nhận ngày</Text>
                                        </TouchableOpacity>
                                    )}
                                </View>
                            )}

                            <TouchableOpacity style={styles.saveBtn} onPress={handleUpdateBudget}><Text style={styles.saveBtnText}>CẬP NHẬT</Text></TouchableOpacity>
                            <View style={{ height: 40 }} />
                        </ScrollView>
                    </KeyboardAvoidingView>
                </View>
            </Modal>

            {/* MODAL THÊM TIỀN (MỚI TÍCH HỢP) */}
            <Modal visible={isAddMoneyVisible} animationType="fade" transparent>
                <View style={styles.modalOverlay}>
                    <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={[styles.modalContent, { height: 'auto', paddingBottom: 40 }]}>
                        <View style={styles.modalHeader}>
                            <Text style={styles.modalTitle}>Thêm tiền</Text>
                            <TouchableOpacity onPress={() => setAddMoneyVisible(false)}>
                                <MaterialIcons name="close" size={24} color={colors.text} />
                            </TouchableOpacity>
                        </View>
                        <View style={{ padding: 20 }}>
                            <Text style={styles.inputLabel}>NHẬP SỐ TIỀN MUỐN THÊM</Text>
                            <TextInput
                                style={styles.textInput}
                                keyboardType="numeric"
                                autoFocus
                                value={addMoneyValue > 0 ? addMoneyValue.toLocaleString('vi-VN') : ''}
                                placeholder="0 VNĐ"
                                placeholderTextColor="#9CA3AF"
                                onChangeText={(val) => {
                                    const num = parseFloat(val.replace(/[^0-9]/g, '')) || 0;
                                    if (num > MAX_AMOUNT) {
                                        Alert.alert('Thông báo', 'Số tiền tối đa là 50 tỷ VNĐ');
                                        setAddMoneyValue(MAX_AMOUNT);
                                    } else {
                                        setAddMoneyValue(num);
                                    }
                                }}
                            />
                            <TouchableOpacity 
                                style={[styles.saveBtn, { opacity: addMoneyValue <= 0 ? 0.5 : 1 }]}
                                disabled={addMoneyValue <= 0}
                                onPress={() => {
                                    if (selectedItem) {
                                        const newTotal = selectedItem.currentAmount + addMoneyValue;
                                        if (newTotal > MAX_AMOUNT) {
                                            Alert.alert('Lỗi', 'Tổng số tiền sau khi thêm không được vượt quá 50 tỷ VNĐ');
                                            return;
                                        }
                                        updateMutation.mutate({
                                            id: selectedItem.id,
                                            payload: { currentAmount: newTotal }
                                        });
                                    }
                                }}
                            >
                                <Text style={styles.saveBtnText}>XÁC NHẬN THÊM</Text>
                            </TouchableOpacity>
                        </View>
                    </KeyboardAvoidingView>
                </View>
            </Modal>
        </SafeAreaView>
    );
}

const getStyles = (colors: any, isDark: boolean) => StyleSheet.create({
    container: { flex: 1, backgroundColor: colors.background },
    centerContainer: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: colors.background },
    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 24 },
    headerTitle: { fontSize: 32, fontWeight: 'bold', color: colors.text },
    addButton: { backgroundColor: '#1A237E', borderRadius: 15, width: 45, height: 45, justifyContent: 'center', alignItems: 'center' },
    tabsContainer: { flexDirection: 'row', paddingHorizontal: 20, marginBottom: 15 },
    tabBtn: { paddingVertical: 8, paddingHorizontal: 16, marginRight: 10 },
    tabBtnActive: { borderBottomWidth: 3, borderBottomColor: '#1A237E' },
    tabText: { fontSize: 14, fontWeight: '600', color: '#9CA3AF' },
    tabTextActive: { color: '#1A237E' },
    listContent: { paddingHorizontal: 20, paddingBottom: 100 },
    sectionTitle: { fontSize: 14, fontWeight: '800', color: '#9CA3AF', textTransform: 'uppercase', marginTop: 25, marginBottom: 12 },
    card: { backgroundColor: colors.surface, borderRadius: 24, padding: 20, marginBottom: 16, elevation: 4 },
    cardHeaderRow: { flexDirection: 'row', justifyContent: 'space-between' },
    cardHeaderLeft: { flexDirection: 'row', alignItems: 'center', flex: 1 },
    cardHeaderRight: { alignItems: 'flex-end' },
    iconBox: { width: 48, height: 48, borderRadius: 16, justifyContent: 'center', alignItems: 'center', marginRight: 16 },
    titleWrapper: { flex: 1 },
    cardTitle: { fontSize: 17, fontWeight: '700', color: colors.text },
    categoryNameText: { fontSize: 15, color: '#95a4bd', marginTop: 2 },
    cardAmount: { fontSize: 14, fontWeight: '600', color: '#1A237E', marginTop: 6 },
    totalAmountText: { color: '#9CA3AF', fontWeight: '400' },
    progressInfoRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-end', marginTop: 12 },
    percentageText: { fontSize: 13, fontWeight: 'bold', color: '#1A237E' },
    dateText: { fontSize: 11, color: '#9CA3AF' },
    moreButton: { padding: 4 },
    progressBarBackground: { height: 8, backgroundColor: isDark ? '#333' : '#F3F4F6', borderRadius: 4, marginTop: 6 },
    progressBarFill: { height: '100%', borderRadius: 4 },
    modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'flex-end' },
    modalContent: { backgroundColor: colors.surface, borderTopLeftRadius: 30, borderTopRightRadius: 30, maxHeight: '90%' },
    modalHeader: { flexDirection: 'row', justifyContent: 'space-between', padding: 24, borderBottomWidth: 1, borderBottomColor: '#EEE' },
    modalTitle: { fontSize: 20, fontWeight: 'bold', color: colors.text },
    inputLabel: { fontSize: 11, fontWeight: '700', color: '#9CA3AF', marginTop: 20, marginBottom: 8 },
    textInput: { backgroundColor: isDark ? '#1E1E2C' : '#F8F9FA', borderRadius: 12, padding: 16, color: colors.text, fontSize: 16, justifyContent: 'center' },
    saveBtn: { backgroundColor: '#1A237E', padding: 18, borderRadius: 16, alignItems: 'center', marginTop: 30 },
    saveBtnText: { color: '#FFF', fontWeight: 'bold' },
    datePickerContainer: { backgroundColor: isDark ? '#1E1E2C' : '#F8F9FA', borderRadius: 12, marginTop: 10, paddingBottom: 15 },
    doneBtn: { backgroundColor: '#1A237E', paddingVertical: 12, paddingHorizontal: 30, borderRadius: 12, alignSelf: 'center', marginTop: 10 },
    doneBtnText: { color: '#FFF', fontWeight: 'bold', fontSize: 14 }
});