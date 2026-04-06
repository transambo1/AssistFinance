import React, { useState, useEffect } from 'react';
import {
    View,
    Text,
    StyleSheet,
    TextInput,
    TouchableOpacity,
    ScrollView,
    SafeAreaView,
    KeyboardAvoidingView,
    Platform,
    Alert,
    ActivityIndicator,
    Modal,
    FlatList
} from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import DateTimePicker from '@react-native-community/datetimepicker';

import { budgetService } from '../src/api/budgetService';
import { categoryService } from '../src/api/categoryService'; // Import API Danh mục của bạn

export default function CreateBudgetScreen() {
    const router = useRouter();
    const queryClient = useQueryClient();

    // --- STATES FORM ---
    const [name, setName] = useState('');
    const [targetAmount, setTargetAmount] = useState('');
    const [currentAmount, setCurrentAmount] = useState('');
    const [type, setType] = useState<'LIMIT' | 'SAVING'>('LIMIT');

    // --- STATES DANH MỤC ---
    const [selectedCategory, setSelectedCategory] = useState<any>(null);
    const [isCategoryModalVisible, setCategoryModalVisible] = useState(false);

    // --- STATES THỜI GIAN ---
    const [startDate, setStartDate] = useState(new Date());

    // Mặc định kết thúc sau 1 tháng
    const defaultEndDate = new Date();
    defaultEndDate.setMonth(defaultEndDate.getMonth() + 1);
    defaultEndDate.setDate(defaultEndDate.getDate() - 1);
    const [endDate, setEndDate] = useState(defaultEndDate);

    const [activeDurationChip, setActiveDurationChip] = useState<number | null>(1); // 1, 3, 12, 24 hoặc null nếu tự chọn ngày
    const [showDatePicker, setShowDatePicker] = useState<'START' | 'END' | null>(null);


    const { data: categories = [], isLoading: isLoadingCategories } = useQuery({
        queryKey: ['categories', 'EXPENSE'],
        queryFn: async () => {
            // Chỉ lấy danh mục CHI TIÊU cho ngân sách Hạn mức
            const res = await categoryService.getAll({ type: 'EXPENSE' });
            return res?.data?.data || res?.data?.items || res?.data || [];
        }
    });

    // --- FORMATTERS ---
    const formatNumber = (numStr: string) => {
        const cleaned = numStr.replace(/\D/g, '');
        return cleaned === '' ? '' : Number(cleaned).toLocaleString('vi-VN');
    };

    const getRawNumber = (formattedStr: string) => {
        return Number(formattedStr.replace(/\D/g, ''));
    };

    const formatDateUI = (date: Date) => {
        return `${date.getDate().toString().padStart(2, '0')}/${(date.getMonth() + 1).toString().padStart(2, '0')}/${date.getFullYear()}`;
    };

    // --- LOGIC CHỌN THỜI GIAN ---
    const handleSelectDurationChip = (months: number) => {
        setActiveDurationChip(months);
        const newStart = new Date();
        const newEnd = new Date();
        newEnd.setMonth(newStart.getMonth() + months);
        newEnd.setDate(newEnd.getDate() - 1); // Trừ 1 ngày cho tròn tháng

        setStartDate(newStart);
        setEndDate(newEnd);
    };

    const handleDateChange = (event: any, selectedDate?: Date) => {
        // Đóng picker trên Android
        if (Platform.OS === 'android') {
            setShowDatePicker(null);
        }
        if (event?.type === 'dismissed') {
            return;
        }

        if (selectedDate) {
            setActiveDurationChip(null); // Người dùng tự chọn ngày -> Hủy trạng thái của Chip 1T, 3T...
            if (showDatePicker === 'START') {
                setStartDate(selectedDate);
                if (selectedDate > endDate) setEndDate(selectedDate); // Cập nhật luôn ngày kết thúc nếu nó bé hơn ngày bắt đầu
            } else if (showDatePicker === 'END') {
                setEndDate(selectedDate);
            }
        }

        // Đóng picker trên iOS
        if (Platform.OS === 'ios') {
            setShowDatePicker(null);
        }
    };

    // --- MUTATION GỌI API TẠO NGÂN SÁCH ---
    const createMutation = useMutation({
        mutationFn: (data: any) => budgetService.create(data),
        onSuccess: () => {
            Alert.alert('Thành công', 'Đã tạo ngân sách mới!', [
                {
                    text: 'OK',
                    onPress: () => {
                        queryClient.invalidateQueries({ queryKey: ['budgets'] });
                        router.back();
                    }
                }
            ]);
        },
        onError: (error: any) => {
            Alert.alert('Lỗi', error?.message || 'Không thể tạo ngân sách. Vui lòng kiểm tra lại!');
        }
    });

    const handleSave = () => {
        const rawTarget = getRawNumber(targetAmount);
        if (!rawTarget || rawTarget <= 0) return Alert.alert('Lỗi', 'Số tiền mục tiêu phải lớn hơn 0.');
        if (!name.trim()) return Alert.alert('Lỗi', 'Vui lòng nhập tên ngân sách.');
        if (type === 'LIMIT' && !selectedCategory) return Alert.alert('Lỗi', 'Vui lòng chọn danh mục.');
        if (endDate < startDate) return Alert.alert('Lỗi', 'Ngày kết thúc không được nhỏ hơn ngày bắt đầu.');

        const payload = {
            name: name.trim(),
            targetAmount: rawTarget,
            currentAmount: getRawNumber(currentAmount) || 0,
            type: type,
            categoryId: type === 'LIMIT' ? selectedCategory.id : null,

            // Gửi Epoch Milli cho BE
            startDate: startDate.getTime(),
            endDate: endDate.getTime(),

            // Nếu user bấm chip thì gửi duration, tự chọn ngày thì gửi null để BE lấy startDate/endDate
            durationMonths: activeDurationChip
        };

        createMutation.mutate(payload);
    };


    const renderCategoryItem = ({ item }: { item: any }) => (
        <TouchableOpacity
            style={styles.categoryItem}
            onPress={() => {
                setSelectedCategory(item);
                setCategoryModalVisible(false);
            }}
        >
            <View style={[styles.iconBox, { backgroundColor: item.color ? item.color + '20' : '#E8EAF6' }]}>
                <MaterialIcons name={item.icon || 'category'} size={24} color={item.color || '#1a237e'} />
            </View>
            <Text style={styles.categoryNameItem}>{item.name}</Text>
            {selectedCategory?.id === item.id && (
                <MaterialIcons name="check-circle" size={24} color="#4CAF50" />
            )}
        </TouchableOpacity>
    );

    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <TouchableOpacity onPress={() => router.back()} style={styles.backButton}>
                    <MaterialIcons name="arrow-back" size={24} color="#1A1A2C" />
                </TouchableOpacity>
                <Text style={styles.headerTitle}>Tạo Ngân Sách</Text>
                <View style={{ width: 32 }} />
            </View>

            <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
                <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.scrollContent}>

                    {/* Card 1: Số tiền */}
                    <View style={styles.amountCard}>
                        <Text style={styles.label}>SỐ TIỀN MỤC TIÊU</Text>
                        <View style={styles.amountInputContainer}>
                            <TextInput
                                style={styles.amountInput}
                                value={targetAmount === '' ? '' : formatNumber(targetAmount)}
                                onChangeText={setTargetAmount}
                                keyboardType="numeric"
                                placeholder="0"
                                placeholderTextColor="#1a237e80"
                            />
                            <Text style={styles.currencyText}>VND</Text>
                        </View>
                    </View>

                    {/* Card 2: Thông tin */}
                    <View style={styles.card}>
                        <Text style={styles.label}>TÊN NGÂN SÁCH</Text>
                        <TextInput
                            style={styles.input}
                            placeholder="e.g., Chi tiêu ăn uống tháng này"
                            placeholderTextColor="#9E9E9E"
                            value={name}
                            onChangeText={setName}
                        />

                        <Text style={[styles.label, { marginTop: 20 }]}>LOẠI NGÂN SÁCH</Text>
                        <View style={styles.toggleContainer}>
                            <TouchableOpacity
                                style={[styles.toggleBtn, type === 'LIMIT' && styles.toggleBtnActive]}
                                onPress={() => setType('LIMIT')}
                            >
                                <Text style={[styles.toggleText, type === 'LIMIT' && styles.toggleTextActive]}>Hạn mức</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={[styles.toggleBtn, type === 'SAVING' && styles.toggleBtnActive]}
                                onPress={() => setType('SAVING')}
                            >
                                <Text style={[styles.toggleText, type === 'SAVING' && styles.toggleTextActive]}>Tiết kiệm</Text>
                            </TouchableOpacity>
                        </View>
                    </View>

                    {/* Card 3: Danh mục & Số dư */}
                    <View style={styles.card}>
                        {type === 'LIMIT' && (
                            <TouchableOpacity style={styles.categorySelector} onPress={() => setCategoryModalVisible(true)}>
                                <View style={styles.categoryLeft}>
                                    <View style={styles.iconBox}>
                                        <MaterialIcons
                                            name={selectedCategory?.icon || 'help-outline'}
                                            size={24}
                                            color="#1a237e"
                                        />
                                    </View>
                                    <View>
                                        <Text style={styles.label}>DANH MỤC</Text>
                                        <Text style={styles.categoryName}>
                                            {selectedCategory ? selectedCategory.name : "Chạm để chọn danh mục"}
                                        </Text>
                                    </View>
                                </View>
                                <MaterialIcons name="chevron-right" size={24} color="#9E9E9E" />
                            </TouchableOpacity>
                        )}

                        <Text style={[styles.label, type === 'LIMIT' && { marginTop: 20 }]}>SỐ DƯ ĐÃ TIÊU (TÙY CHỌN)</Text>
                        <View style={styles.inputWithSuffix}>
                            <TextInput
                                style={styles.inputRaw}
                                placeholder="0"
                                placeholderTextColor="#9E9E9E"
                                keyboardType="numeric"
                                value={currentAmount === '' ? '' : formatNumber(currentAmount)}
                                onChangeText={setCurrentAmount}
                            />
                            <Text style={styles.suffixText}>VND</Text>
                        </View>
                    </View>

                    {/* Card 4: Thời gian */}
                    <View style={styles.card}>
                        <Text style={styles.label}>CHỌN NHANH THỜI HẠN</Text>
                        <View style={styles.durationSelector}>
                            {[1, 3, 12, 24].map((m) => (
                                <TouchableOpacity
                                    key={m}
                                    style={[styles.durationChip, activeDurationChip === m && styles.durationChipActive]}
                                    onPress={() => handleSelectDurationChip(m)}
                                >
                                    <Text style={[styles.durationText, activeDurationChip === m && styles.durationTextActive]}>
                                        {m} Tháng
                                    </Text>
                                </TouchableOpacity>
                            ))}
                        </View>

                        <View style={styles.dateContainer}>
                            <TouchableOpacity style={styles.dateBox} onPress={() => setShowDatePicker('START')}>
                                <Text style={styles.label}>NGÀY BẮT ĐẦU</Text>
                                <View style={styles.dateValueBox}>
                                    <MaterialIcons name="calendar-today" size={16} color="#333" />
                                    <Text style={styles.dateText}>{formatDateUI(startDate)}</Text>
                                </View>
                            </TouchableOpacity>

                            <TouchableOpacity style={styles.dateBox} onPress={() => setShowDatePicker('END')}>
                                <Text style={styles.label}>NGÀY KẾT THÚC</Text>
                                <View style={styles.dateValueBox}>
                                    <MaterialIcons name="calendar-today" size={16} color="#333" />
                                    <Text style={styles.dateText}>{formatDateUI(endDate)}</Text>
                                </View>
                            </TouchableOpacity>
                        </View>
                    </View>

                    {/* Nút lưu */}
                    <TouchableOpacity style={styles.saveButton} onPress={handleSave} disabled={createMutation.isPending}>
                        {createMutation.isPending ? (
                            <ActivityIndicator color="#fff" />
                        ) : (
                            <Text style={styles.saveButtonText}>Lưu ngân sách</Text>
                        )}
                    </TouchableOpacity>
                </ScrollView>
            </KeyboardAvoidingView>
            {/* --- XỬ LÝ LỊCH CHO ANDROID --- */}
            {Platform.OS === 'android' && showDatePicker && (
                <DateTimePicker
                    value={showDatePicker === 'START' ? startDate : endDate}
                    mode="date"
                    display="default"
                    onChange={handleDateChange}
                    minimumDate={showDatePicker === 'END' ? startDate : undefined}
                />
            )}

            {/* --- XỬ LÝ LỊCH CHO iOS (Có nút XONG đàng hoàng) --- */}
            {Platform.OS === 'ios' && showDatePicker && (
                <Modal transparent animationType="fade">
                    <View style={{ flex: 1, justifyContent: 'flex-end', backgroundColor: 'rgba(0,0,0,0.5)' }}>
                        <View style={{ backgroundColor: '#FFF', paddingBottom: 20, borderTopLeftRadius: 20, borderTopRightRadius: 20 }}>
                            {/* Thanh công cụ chứa nút Xong */}
                            <View style={{ flexDirection: 'row', justifyContent: 'flex-end', padding: 16, borderBottomWidth: 1, borderBottomColor: '#F0F0F0' }}>
                                <TouchableOpacity onPress={() => setShowDatePicker(null)}>
                                    <Text style={{ color: '#1a237e', fontWeight: 'bold', fontSize: 16 }}>Xong</Text>
                                </TouchableOpacity>
                            </View>

                            <DateTimePicker
                                value={showDatePicker === 'START' ? startDate : endDate}
                                mode="date"
                                display="spinner"
                                onChange={handleDateChange}
                                minimumDate={showDatePicker === 'END' ? startDate : undefined}
                            />
                        </View>
                    </View>
                </Modal>
            )}
            {/* BOTTOM SHEET: CHỌN DANH MỤC */}
            <Modal visible={isCategoryModalVisible} transparent animationType="slide">
                <View style={styles.modalOverlay}>
                    <TouchableOpacity style={{ flex: 1 }} onPress={() => setCategoryModalVisible(false)} />
                    <View style={styles.bottomSheet}>
                        <View style={styles.sheetHeader}>
                            <Text style={styles.sheetTitle}>Chọn Danh Mục</Text>
                            <TouchableOpacity onPress={() => setCategoryModalVisible(false)}>
                                <MaterialIcons name="close" size={24} color="#333" />
                            </TouchableOpacity>
                        </View>

                        {isLoadingCategories ? (
                            <ActivityIndicator size="large" color="#1a237e" style={{ padding: 40 }} />
                        ) : categories.length === 0 ? (
                            <Text style={{ textAlign: 'center', padding: 20, color: '#999' }}>Chưa có danh mục Chi tiêu nào.</Text>
                        ) : (
                            <FlatList
                                data={categories}
                                keyExtractor={(item) => item.id.toString()}
                                renderItem={renderCategoryItem}
                                showsVerticalScrollIndicator={false}
                                contentContainerStyle={{ paddingBottom: 40 }}
                            />
                        )}
                    </View>
                </View>
            </Modal>

        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#F8F9FA' },
    header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 20, paddingVertical: 16 },
    backButton: { padding: 4 },
    headerTitle: { fontSize: 20, fontWeight: 'bold', color: '#1A1A2C' },
    scrollContent: { padding: 20, paddingBottom: 40 },

    amountCard: { backgroundColor: '#e9e9e9', borderRadius: 24, padding: 24, marginBottom: 16 },
    label: { fontSize: 12, fontWeight: '600', color: '#555', letterSpacing: 0.5, marginBottom: 8, textTransform: 'uppercase' },
    amountInputContainer: { flexDirection: 'row', alignItems: 'baseline' },
    amountInput: { fontSize: 35, fontWeight: 'bold', color: '#1a237e', padding: 0, minWidth: 60 },
    currencyText: { fontSize: 20, fontWeight: 'bold', color: '#6e76a6', marginLeft: 2 },

    card: { backgroundColor: '#FFFFFF', borderRadius: 24, padding: 20, marginBottom: 16, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.03, shadowRadius: 8, elevation: 2 },
    input: { backgroundColor: '#F5F7FA', borderRadius: 16, padding: 16, fontSize: 16, color: '#333' },
    inputWithSuffix: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#F5F7FA', borderRadius: 16, paddingHorizontal: 16 },
    inputRaw: { flex: 1, paddingVertical: 16, fontSize: 16, color: '#333' },
    suffixText: { fontSize: 16, color: '#6e76a6', fontWeight: '500' },

    toggleContainer: { flexDirection: 'row', backgroundColor: '#F5F7FA', borderRadius: 16, padding: 4 },
    toggleBtn: { flex: 1, paddingVertical: 12, alignItems: 'center', borderRadius: 12 },
    toggleBtnActive: { backgroundColor: '#FFFFFF', shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2 },
    toggleText: { fontSize: 15, fontWeight: '600', color: '#9E9E9E' },
    toggleTextActive: { color: '#1a237e' },

    categorySelector: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
    categoryLeft: { flexDirection: 'row', alignItems: 'center' },
    iconBox: { width: 48, height: 48, borderRadius: 24, backgroundColor: '#E8EAF6', alignItems: 'center', justifyContent: 'center', marginRight: 16 },
    categoryName: { fontSize: 16, fontWeight: 'bold', color: '#1A1A2C' },

    durationSelector: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 20 },
    durationChip: { flex: 1, marginHorizontal: 4, paddingVertical: 10, borderRadius: 12, backgroundColor: '#F5F7FA', alignItems: 'center' },
    durationChipActive: { backgroundColor: '#1a237e' },
    durationText: { fontSize: 13, fontWeight: 'bold', color: '#9E9E9E' },
    durationTextActive: { color: '#FFFFFF' },

    dateContainer: { flexDirection: 'row', gap: 16 },
    dateBox: { flex: 1 },
    dateValueBox: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#F5F7FA', padding: 12, borderRadius: 12 },
    dateText: { marginLeft: 8, fontSize: 14, color: '#333', fontWeight: '500' },

    saveButton: { backgroundColor: '#1a237e', borderRadius: 24, paddingVertical: 18, alignItems: 'center', marginTop: 8, shadowColor: '#1a237e', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.3, shadowRadius: 8, elevation: 4 },
    saveButtonText: { color: '#FFFFFF', fontSize: 16, fontWeight: 'bold' },

    /* Bottom Sheet Category Styles */
    modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'flex-end' },
    bottomSheet: { backgroundColor: '#FFF', borderTopLeftRadius: 24, borderTopRightRadius: 24, padding: 24, maxHeight: '70%' },
    sheetHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 },
    sheetTitle: { fontSize: 18, fontWeight: 'bold', color: '#1A1A2C' },
    categoryItem: { flexDirection: 'row', alignItems: 'center', paddingVertical: 12, borderBottomWidth: 1, borderBottomColor: '#F0F0F0' },
    categoryNameItem: { flex: 1, fontSize: 16, color: '#333', fontWeight: '500' }
});