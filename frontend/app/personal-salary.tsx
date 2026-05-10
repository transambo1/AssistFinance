import React, { useState } from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet, SafeAreaView, Image, Switch, TextInput, ActivityIndicator, Alert, Modal, Platform } from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useTheme } from '../src/context/ThemeContext';
import { User } from "@/src/types/index";
import { userService } from "@/src/api/userService";
import { categoryService } from "@/src/api/categoryService"; 
import { salaryConfigService } from "@/src/api/salaryConfigService"; 
import { useQuery, useMutation } from '@tanstack/react-query';
import { aiService } from '@/src/api/aiService';
import { Calendar, LocaleConfig } from 'react-native-calendars';

// Cấu hình tiếng Việt cho lịch
LocaleConfig.locales['vi'] = {
    monthNames: ['Tháng 1', 'Tháng 2', 'Tháng 3', 'Tháng 4', 'Tháng 5', 'Tháng 6', 'Tháng 7', 'Tháng 8', 'Tháng 9', 'Tháng 10', 'Tháng 11', 'Tháng 12'],
    monthNamesShort: ['Th.1', 'Th.2', 'Th.3', 'Th.4', 'Th.5', 'Th.6', 'Th.7', 'Th.8', 'Th.9', 'Th.10', 'Th.11', 'Th.12'],
    dayNames: ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'],
    dayNamesShort: ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'],
    today: "Hôm nay"
};
LocaleConfig.defaultLocale = 'vi';

// Giới hạn 50 tỷ
const MAX_SALARY = 50000000000;

export default function WalletManagementScreen() {
    const { isDark } = useTheme();
    const router = useRouter();

    // 1. LẤY THÔNG TIN USER
    const { data: userData } = useQuery({
        queryKey: ['userProfile'],
        queryFn: async () => {
            const response = await userService.getProfile() as any;
            return (response?.data || response?.result || response) as User;
        },
    });

    // 2. LẤY DANH MỤC
    const { data: categoryData } = useQuery({
        queryKey: ['categories'],
        queryFn: () => categoryService.getAll(),
    });
    const categories = Array.isArray(categoryData?.data) ? categoryData.data : (categoryData?.data?.data || []);

    // States
    // Khởi tạo có format dấu chấm sẵn: 35.000.000
    const [salaryAmount, setSalaryAmount] = useState('35.000.000');
    const [isActive, setIsActive] = useState(true);
    const [selectedDate, setSelectedDate] = useState('2026-04-10');
    const [showCalendar, setShowCalendar] = useState(false);
    const [isSaving, setIsSaving] = useState(false);

    const dayDisplay = selectedDate.split('-')[2];

    const [visionData, setVisionData] = useState({
        title: 'Tầm nhìn Tài chính 2026',
        description: 'Dựa trên cấu hình lương hiện tại, bạn đang đi đúng lộ trình tiết kiệm 25% thu nhập hàng năm.'
    });

    // --- LOGIC MỚI: FORMAT TIỀN & GIỚI HẠN 50 TỶ ---
    const handleAmountChange = (text: string) => {
        // Chỉ giữ lại số
        const cleanNumber = text.replace(/[^0-9]/g, '');
        
        if (cleanNumber === '') {
            setSalaryAmount('');
            return;
        }

        const parsedNumber = parseInt(cleanNumber, 10);

        // Chặn nếu vượt quá 50 tỷ
        if (parsedNumber > MAX_SALARY) {
            Alert.alert("Giới hạn", "Số tiền cấu hình không được vượt quá 50 tỷ VNĐ.");
            return;
        }

        // Định dạng dấu chấm kiểu VN: 1.000.000
        const formatted = cleanNumber.replace(/\B(?=(\d{3})+(?!\d))/g, ".");
        setSalaryAmount(formatted);
    };

    // --- LOGIC LƯU: GỬI SỐ NGUYÊN LÊN BE ---
    const handleSaveConfig = async () => {
        if (!userData?.id) {
            Alert.alert("Lỗi", "Không tìm thấy thông tin người dùng.");
            return;
        }

        const incomeCategory = categories.find((c: any) => c.type === 'INCOME');
        if (!incomeCategory?.id) {
            Alert.alert("Lỗi", "Bạn chưa có danh mục Thu nhập (INCOME) nào.");
            return;
        }

        setIsSaving(true);
    try {
        const amountNum = Number(salaryAmount.replace(/\./g, ''));
        
        const payload = {
            id: null,
            userId: userData.id,
            categoryId: incomeCategory.id,
            amount: amountNum,
            payDay: parseInt(dayDisplay, 10), 
            frequency: "MONTHLY",
            type: "INCOME",
            description: "Lương tự động hàng tháng"
        };

        // GỌI ĐÚNG HÀM UPSERT
        const response = await salaryConfigService.upsert(payload);
        
        if (response) {
            Alert.alert("Thành công", "Đã lưu cấu hình lương tự động.");
        }
    } catch (error) {
        console.error(error);
        Alert.alert("Lỗi", "Không tìm thấy đường dẫn API (404) hoặc lỗi server.");
    } finally {
        setIsSaving(false);
    }
    };

    const generateVisionMutation = useMutation({
        mutationFn: () => aiService.generateFinancialVision(salaryAmount, dayDisplay),
        onSuccess: (data) => setVisionData(data),
        onError: () => Alert.alert("Lỗi", "AI đang bận, vui lòng thử lại sau.")
    });

    const bgColor = isDark ? '#121212' : '#F8F6FA';
    const surfaceColor = isDark ? '#1E1E1E' : '#ffffff';
    const textColor = isDark ? '#ffffff' : '#1b1b21';
    const textDimColor = isDark ? '#A0A0A0' : '#767683';
    const primaryColor = isDark ? '#1a237e' : '#1a237e';

    return (
        <SafeAreaView style={[styles.container, { backgroundColor: bgColor }]}>
            <View style={styles.header}>
                <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
                    <MaterialIcons name="arrow-back" size={24} color={textColor} />
                </TouchableOpacity>
                <View style={styles.headerRight}>
                    <Image source={{ uri: 'https://i.pravatar.cc/150?img=11' }} style={styles.headerAvatar} />
                    <Text style={[styles.headerLogo, { color: primaryColor }]}>Aeon Ledger</Text>
                </View>
                <TouchableOpacity><MaterialIcons name="notifications" size={24} color={textColor} /></TouchableOpacity>
            </View>

            <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
                <View style={styles.pageTitleContainer}>
                    <Text style={[styles.pageSubtitle, { color: textDimColor }]}>THIẾT LẬP TÀI KHOẢN</Text>
                    <Text style={[styles.pageTitle, { color: primaryColor }]}>Cá nhân & Cấu hình{'\n'}Lương</Text>
                </View>

                <View style={[styles.salaryCard, { backgroundColor: surfaceColor }]}>
                    <View style={styles.salaryHeader}>
                        <View style={[styles.iconBox, { backgroundColor: primaryColor }]}>
                            <MaterialIcons name="account-balance-wallet" size={20} color="#fff" />
                        </View>
                        <View>
                            <Text style={[styles.salaryTitle, { color: textColor }]}>Cấu hình lương định kỳ</Text>
                            <Text style={[styles.salaryDesc, { color: textDimColor }]}>Thiết lập thu nhập hàng tháng</Text>
                        </View>
                    </View>

                    <Text style={[styles.inputLabel, { color: primaryColor }]}>SỐ TIỀN (VND)</Text>
                    <View style={[styles.inputContainer, { backgroundColor: isDark ? '#2C2C3E' : '#f5f2fb' }]}>
                        <TextInput
                            style={[styles.input, { color: textColor }]}
                            value={salaryAmount}
                            onChangeText={handleAmountChange} // Dùng hàm format mới
                            keyboardType="numeric"
                        />
                        <Text style={{ color: textDimColor, fontWeight: 'bold' }}>đ</Text>
                    </View>

                    <Text style={[styles.inputLabel, { color: primaryColor, marginTop: 16 }]}>NGÀY NHẬN LƯƠNG HÀNG THÁNG</Text>

                    <TouchableOpacity
                        style={[styles.inputContainer, { backgroundColor: isDark ? '#2C2C3E' : '#f5f2fb', marginBottom: 12, justifyContent: 'space-between' }]}
                        onPress={() => setShowCalendar(true)}
                    >
                        <Text style={[styles.input, { color: textColor, marginTop: Platform.OS === 'ios' ? 0 : 16 }]}>Ngày {dayDisplay} hàng tháng</Text>
                        <MaterialIcons name="calendar-month" size={24} color={primaryColor} />
                    </TouchableOpacity>

                    <Text style={[styles.helperText, { color: textDimColor }]}>Tiền sẽ tự động cộng vào ví ngày {dayDisplay} mỗi tháng.</Text>

                    <View style={[styles.switchRow, { backgroundColor: isDark ? '#2C2C3E' : '#f5f2fb' }]}>
                        <View style={styles.switchLeft}><Text style={[styles.switchText, { color: textColor }]}>Trạng thái hoạt động</Text></View>
                        <Switch value={isActive} onValueChange={setIsActive} trackColor={{ false: "#c6c5d4", true: primaryColor }} thumbColor={"#ffffff"} />
                    </View>

                    <TouchableOpacity 
                        style={[styles.saveBtn, { backgroundColor: primaryColor, opacity: isSaving ? 0.7 : 1 }]}
                        onPress={handleSaveConfig}
                        disabled={isSaving}
                    >
                        {isSaving ? (
                            <ActivityIndicator color="#fff" size="small" />
                        ) : (
                            <>
                                <MaterialIcons name="save" size={20} color="#fff" />
                                <Text style={styles.saveBtnText}>Lưu cấu hình</Text>
                            </>
                        )}
                    </TouchableOpacity>
                </View>

                {/* AI Vision Card */}
                <View style={[styles.visionCard, { backgroundColor: surfaceColor }]}>
                    <Image source={{ uri: 'https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?q=80&w=1000' }} style={styles.visionImage} />
                    <TouchableOpacity style={styles.aiUpdateBtn} onPress={() => generateVisionMutation.mutate()} disabled={generateVisionMutation.isPending}>
                        {generateVisionMutation.isPending ? <ActivityIndicator color={primaryColor} size="small" /> : <Text style={[styles.aiUpdateBtnText, { color: primaryColor }]}>Phân tích với AI</Text>}
                    </TouchableOpacity>
                    <Text style={[styles.visionTitle, { color: primaryColor }]}>{visionData.title}</Text>
                    <Text style={[styles.visionDesc, { color: textDimColor }]}>{visionData.description}</Text>
                </View>
            </ScrollView>

            <Modal visible={showCalendar} transparent animationType="fade">
                <View style={styles.modalOverlay}>
                    <View style={styles.calendarContainer}>
                        <View style={styles.calendarHeader}>
                            <Text style={styles.calendarTitle}>Chọn ngày nhận lương</Text>
                            <TouchableOpacity onPress={() => setShowCalendar(false)}>
                                <MaterialIcons name="close" size={24} color={textColor} />
                            </TouchableOpacity>
                        </View>

                        <Calendar
                            current={selectedDate}
                            markedDates={{
                                [selectedDate]: { selected: true, disableTouchEvent: true, selectedColor: primaryColor }
                            }}
                            onDayPress={(day: any) => {
                                setSelectedDate(day.dateString);
                                setShowCalendar(false); 
                            }}
                            theme={{
                                calendarBackground: '#ffffff',
                                selectedDayBackgroundColor: primaryColor,
                                selectedDayTextColor: '#ffffff',
                                todayTextColor: primaryColor,
                                dayTextColor: '#2d4150',
                                arrowColor: primaryColor,
                                monthTextColor: primaryColor,
                                textDayFontWeight: '500',
                                textMonthFontWeight: 'bold',
                                textDayHeaderFontWeight: 'bold',
                            }}
                        />
                    </View>
                </View>
            </Modal>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1 },
    header: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 20, paddingVertical: 12 },
    backBtn: { padding: 4 },
    headerRight: { flexDirection: 'row', alignItems: 'center', gap: 8 },
    headerAvatar: { width: 32, height: 32, borderRadius: 16 },
    headerLogo: { fontSize: 18, fontWeight: 'bold' },
    scrollContent: { paddingHorizontal: 20, paddingBottom: 40 },
    pageTitleContainer: { marginTop: 16, marginBottom: 24 },
    pageSubtitle: { fontSize: 12, fontWeight: '600', letterSpacing: 1, marginBottom: 4 },
    pageTitle: { fontSize: 32, fontWeight: 'bold', lineHeight: 38 },
    salaryCard: { borderRadius: 24, padding: 24, marginBottom: 24, shadowColor: '#000', elevation: 3 },
    salaryHeader: { flexDirection: 'row', gap: 16, alignItems: 'center', marginBottom: 24 },
    iconBox: { width: 40, height: 40, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
    salaryTitle: { fontSize: 18, fontWeight: 'bold' },
    salaryDesc: { fontSize: 13 },
    inputLabel: { fontSize: 12, fontWeight: '700', marginBottom: 8, letterSpacing: 0.5 },
    inputContainer: { flexDirection: 'row', alignItems: 'center', height: 56, borderRadius: 16, paddingHorizontal: 16 },
    input: { flex: 1, fontSize: 18, fontWeight: '600' },
    helperText: { fontSize: 12, fontStyle: 'italic', marginBottom: 24, lineHeight: 18 },
    switchRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16, paddingVertical: 12, borderRadius: 16, marginBottom: 24 },
    switchLeft: { flexDirection: 'row', alignItems: 'center', gap: 12 },
    switchText: { fontSize: 15, fontWeight: '600' },
    saveBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', height: 56, borderRadius: 16, gap: 8 },
    saveBtnText: { color: '#fff', fontSize: 16, fontWeight: 'bold' },
    visionCard: { borderRadius: 24, overflow: 'hidden', paddingBottom: 20, elevation: 3, marginBottom: 20 },
    visionImage: { width: '100%', height: 160, marginBottom: 16 },
    visionTitle: { fontSize: 18, fontWeight: 'bold', paddingHorizontal: 20, marginBottom: 8 },
    visionDesc: { fontSize: 14, lineHeight: 22, paddingHorizontal: 20, marginBottom: 16 },
    aiUpdateBtn: { paddingHorizontal: 12, paddingVertical: 6, borderRadius: 20, backgroundColor: 'rgba(26, 35, 126, 0.1)', alignSelf: 'flex-start', marginLeft: 20, marginBottom: 12 },
    aiUpdateBtnText: { fontSize: 12, fontWeight: '700' },
    modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'center', alignItems: 'center' },
    calendarContainer: { width: '90%', backgroundColor: '#fff', borderRadius: 24, padding: 20, shadowColor: '#000', elevation: 10 },
    calendarHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 15 },
    calendarTitle: { fontSize: 18, fontWeight: 'bold', color: '#1b1b21' },
});