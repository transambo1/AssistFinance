import React, { useState } from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet, SafeAreaView, Image, Switch, TextInput, ActivityIndicator, Alert } from 'react-native'; // THÊM MỚI: ActivityIndicator, Alert
import { MaterialIcons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useTheme } from '../src/context/ThemeContext';
import { User } from "@/src/types/index";
import { userService } from "@/src/api/userService";
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { formatMoney } from '../src/utils/formatters';
import { aiService } from '@/src/api/aiService'; // THÊM MỚI: Import file aiService của Groq

export default function WalletManagementScreen() {
    const { isDark } = useTheme();
    const router = useRouter();
    const queryClient = useQueryClient();

    const { data: userData, isLoading } = useQuery<User>({
        queryKey: ['userProfile'],
        queryFn: async () => {
            const response = await userService.getProfile() as any;
            return (response?.data || response?.result || response) as User;
        },
    });

    // Các State cho Cấu hình lương
    const [salaryAmount, setSalaryAmount] = useState('35.000.000');
    const [selectedDay, setSelectedDay] = useState('10');
    const [isActive, setIsActive] = useState(true);

    const salaryDays = ['01', '05', '10', '15', '20', '25', '30'];

    // THÊM MỚI: State lưu Tầm nhìn tài chính
    const [visionData, setVisionData] = useState({
        title: 'Tầm nhìn Tài chính 2026',
        description: 'Dựa trên cấu hình lương hiện tại, bạn đang đi đúng lộ trình tiết kiệm 25% thu nhập hàng năm. Hãy cân nhắc tối ưu hóa các khoản chi định kỳ trong phần Recurring.'
    });

    // THÊM MỚI: Hàm gọi Groq AI
    const generateVisionMutation = useMutation({
        mutationFn: () => aiService.generateFinancialVision(salaryAmount, selectedDay),
        onSuccess: (data) => {
            setVisionData(data); // Cập nhật giao diện khi có data mới
        },
        onError: () => {
            Alert.alert("Lỗi", "AI đang bận, vui lòng thử lại sau.");
        }
    });

    // Màu sắc theo Theme
    const bgColor = isDark ? '#121212' : '#F8F6FA';
    const surfaceColor = isDark ? '#1E1E1E' : '#ffffff';
    const textColor = isDark ? '#ffffff' : '#1b1b21';
    const textDimColor = isDark ? '#A0A0A0' : '#767683';
    const primaryColor = isDark ? '#8c9eff' : '#1a237e';

    return (
        <SafeAreaView style={[styles.container, { backgroundColor: bgColor }]}>
            {/* Header Mặc định */}
            <View style={styles.header}>
                <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
                    <MaterialIcons name="arrow-back" size={24} color={textColor} />
                </TouchableOpacity>
                <View style={styles.headerRight}>
                    <Image source={{ uri: 'https://i.pravatar.cc/150?img=11' }} style={styles.headerAvatar} />
                    <Text style={[styles.headerLogo, { color: primaryColor }]}>Aeon Ledger</Text>
                </View>
                <TouchableOpacity>
                    <MaterialIcons name="notifications" size={24} color={textColor} />
                </TouchableOpacity>
            </View>

            <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>

                {/* Tiêu đề trang */}
                <View style={styles.pageTitleContainer}>
                    <Text style={[styles.pageSubtitle, { color: textDimColor }]}>THIẾT LẬP TÀI KHOẢN</Text>
                    <Text style={[styles.pageTitle, { color: primaryColor }]}>Cá nhân & Cấu hình{'\n'}Lương</Text>
                </View>

                {/* Card Thông tin user */}
                <View style={[styles.userCard, { backgroundColor: surfaceColor }]}>
                    <View style={styles.avatarWrapper}>
                        <Image source={{ uri: 'https://i.pravatar.cc/150?img=11' }} style={styles.mainAvatar} />
                    </View>
                    <Text style={[styles.userName, { color: textColor }]}>{userData?.fullName || userData?.displayName}</Text>
                    <Text style={[styles.userEmail, { color: textDimColor }]}>{userData?.email}</Text>

                    <View style={styles.balanceBox}>
                        <Text style={styles.balanceLabel}>SỐ DƯ HIỆN TẠI</Text>
                        <View style={styles.balanceRow}>
                            <Text style={[styles.balanceAmount, { color: primaryColor }]}>{formatMoney(userData?.currentBalance || 0)}</Text>
                            <Text style={[styles.balanceCurrency, { color: primaryColor }]}>VND</Text>
                        </View>
                    </View>
                </View>

                {/* 2 Card Nhỏ: Tăng trưởng & Bảo mật */}
                <View style={styles.statsRow}>
                    <View style={[styles.statCard, { backgroundColor: isDark ? '#1b3b22' : '#eafaf1' }]}>
                        <MaterialIcons name="trending-up" size={20} color="#2e7d32" />
                        <Text style={[styles.statValue, { color: '#2e7d32' }]}>+12%</Text>
                        <Text style={[styles.statLabel, { color: '#2e7d32' }]}>TĂNG TRƯỞNG</Text>
                    </View>
                    <View style={[styles.statCard, { backgroundColor: isDark ? '#2a2438' : '#f4f2f6' }]}>
                        <MaterialIcons name="verified-user" size={20} color={primaryColor} />
                        <Text style={[styles.statValue, { color: textColor }]}>Bảo mật</Text>
                        <Text style={[styles.statLabel, { color: textDimColor }]}>CẤP ĐỘ 3</Text>
                    </View>
                </View>

                {/* Card Cấu hình lương */}
                <View style={[styles.salaryCard, { backgroundColor: surfaceColor }]}>
                    <View style={styles.salaryHeader}>
                        <View style={[styles.iconBox, { backgroundColor: primaryColor }]}>
                            <MaterialIcons name="account-balance-wallet" size={20} color="#fff" />
                        </View>
                        <View>
                            <Text style={[styles.salaryTitle, { color: textColor }]}>Cấu hình lương định kỳ</Text>
                            <Text style={[styles.salaryDesc, { color: textDimColor }]}>Thiết lập thu nhập hàng tháng của bạn</Text>
                        </View>
                    </View>

                    <Text style={[styles.inputLabel, { color: primaryColor }]}>SỐ TIỀN (VND)</Text>
                    <View style={[styles.inputContainer, { backgroundColor: isDark ? '#2C2C3E' : '#f5f2fb' }]}>
                        <TextInput
                            style={[styles.input, { color: textColor }]}
                            value={salaryAmount}
                            onChangeText={setSalaryAmount}
                            keyboardType="numeric"
                        />
                        <MaterialIcons name="edit" size={20} color={textDimColor} />
                    </View>

                    <Text style={[styles.inputLabel, { color: primaryColor, marginTop: 16 }]}>NGÀY NHẬN LƯƠNG HÀNG THÁNG</Text>
                    <View style={styles.daysRow}>
                        {salaryDays.map((day) => {
                            const isSelected = selectedDay === day;
                            return (
                                <TouchableOpacity
                                    key={day}
                                    style={[
                                        styles.dayBtn,
                                        { backgroundColor: isSelected ? primaryColor : (isDark ? '#2C2C3E' : '#f5f2fb') },
                                        isSelected && styles.dayBtnSelected
                                    ]}
                                    onPress={() => setSelectedDay(day)}
                                >
                                    <Text style={[
                                        styles.dayText,
                                        { color: isSelected ? '#fff' : textColor }
                                    ]}>{day}</Text>
                                </TouchableOpacity>
                            )
                        })}
                    </View>
                    <Text style={[styles.helperText, { color: textDimColor }]}>Lương sẽ tự động cộng vào số dư vào ngày {selectedDay} mỗi tháng.</Text>

                    <View style={[styles.switchRow, { backgroundColor: isDark ? '#2C2C3E' : '#f5f2fb' }]}>
                        <View style={styles.switchLeft}>
                            <Text style={[styles.switchText, { color: textColor }]}>Trạng thái hoạt động</Text>
                        </View>
                        <Switch
                            value={isActive}
                            onValueChange={setIsActive}
                            trackColor={{ false: "#c6c5d4", true: primaryColor }}
                            thumbColor={"#ffffff"}
                        />
                    </View>

                    <TouchableOpacity style={[styles.saveBtn, { backgroundColor: primaryColor }]}>
                        <MaterialIcons name="save" size={20} color="#fff" />
                        <Text style={styles.saveBtnText}>Lưu cấu hình</Text>
                    </TouchableOpacity>
                </View>

                {/* THÊM MỚI: Giao diện AI Card */}
                <View style={[styles.visionCard, { backgroundColor: surfaceColor }]}>
                    <Image
                        source={{ uri: 'https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?q=80&w=1000&auto=format&fit=crop' }}
                        style={styles.visionImage}
                    />

                    {/* Nút Cập nhật AI */}
                    <TouchableOpacity
                        style={styles.aiUpdateBtn}
                        onPress={() => generateVisionMutation.mutate()}
                        disabled={generateVisionMutation.isPending}
                    >
                        {generateVisionMutation.isPending ? (
                            <ActivityIndicator color={primaryColor} size="small" />
                        ) : (
                            <>
                                <MaterialIcons name="psychology" size={16} color={primaryColor} />
                                <Text style={[styles.aiUpdateBtnText, { color: primaryColor }]}>Phân tích với AI</Text>
                            </>
                        )}
                    </TouchableOpacity>

                    <Text style={[styles.visionTitle, { color: primaryColor }]}>{visionData.title}</Text>
                    <Text style={[styles.visionDesc, { color: textDimColor }]}>
                        {/* Nếu câu trả lời có chữ Recurring thì in đậm nó lên */}
                        {visionData.description.includes('Recurring') ? (
                            <>
                                {visionData.description.split('Recurring')[0]}
                                <Text style={{ fontWeight: 'bold', color: textColor }}>Recurring</Text>
                                {visionData.description.split('Recurring')[1]}
                            </>
                        ) : (
                            visionData.description
                        )}
                    </Text>
                    <TouchableOpacity style={styles.visionLink}>
                        <Text style={[styles.visionLinkText, { color: primaryColor }]}>Xem báo cáo chi tiết</Text>
                        <MaterialIcons name="arrow-forward" size={16} color={primaryColor} />
                    </TouchableOpacity>
                </View>

            </ScrollView>
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

    userCard: { borderRadius: 24, padding: 24, alignItems: 'center', marginTop: 40, marginBottom: 16, shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.05, shadowRadius: 12, elevation: 3 },
    avatarWrapper: { width: 88, height: 88, borderRadius: 44, backgroundColor: '#1a237e', alignItems: 'center', justifyContent: 'center', marginTop: -68, marginBottom: 16, borderWidth: 4, borderColor: '#fff' },
    mainAvatar: { width: 80, height: 80, borderRadius: 40 },
    userName: { fontSize: 22, fontWeight: 'bold', marginBottom: 4 },
    userEmail: { fontSize: 14, marginBottom: 24 },
    balanceBox: { width: '100%', backgroundColor: '#f5f2fb', padding: 20, borderRadius: 16 },
    balanceLabel: { fontSize: 11, fontWeight: '600', color: '#767683', letterSpacing: 1, marginBottom: 8 },
    balanceRow: { flexDirection: 'row', alignItems: 'baseline', gap: 8 },
    balanceAmount: { fontSize: 32, fontWeight: '900' },
    balanceCurrency: { fontSize: 16, fontWeight: 'bold' },

    statsRow: { flexDirection: 'row', gap: 16, marginBottom: 24 },
    statCard: { flex: 1, padding: 16, borderRadius: 20, gap: 8 },
    statValue: { fontSize: 20, fontWeight: 'bold' },
    statLabel: { fontSize: 11, fontWeight: '600', letterSpacing: 1 },

    salaryCard: { borderRadius: 24, padding: 24, marginBottom: 24, shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.05, shadowRadius: 12, elevation: 3 },
    salaryHeader: { flexDirection: 'row', gap: 16, alignItems: 'center', marginBottom: 24 },
    iconBox: { width: 40, height: 40, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
    salaryTitle: { fontSize: 18, fontWeight: 'bold', marginBottom: 4 },
    salaryDesc: { fontSize: 13 },

    inputLabel: { fontSize: 12, fontWeight: '700', marginBottom: 8, letterSpacing: 0.5 },
    inputContainer: { flexDirection: 'row', alignItems: 'center', height: 56, borderRadius: 16, paddingHorizontal: 16 },
    input: { flex: 1, fontSize: 18, fontWeight: '600' },

    daysRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 12 },
    dayBtn: { width: 40, height: 40, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
    dayBtnSelected: { borderWidth: 2, borderColor: '#fff', shadowColor: '#1a237e', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.3, shadowRadius: 4, elevation: 4 },
    dayText: { fontSize: 15, fontWeight: '600' },
    helperText: { fontSize: 12, fontStyle: 'italic', marginBottom: 24, lineHeight: 18 },

    switchRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 16, paddingVertical: 12, borderRadius: 16, marginBottom: 24 },
    switchLeft: { flexDirection: 'row', alignItems: 'center', gap: 12 },
    switchText: { fontSize: 15, fontWeight: '600' },

    saveBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', height: 56, borderRadius: 16, gap: 8 },
    saveBtnText: { color: '#fff', fontSize: 16, fontWeight: 'bold' },

    visionCard: { borderRadius: 24, overflow: 'hidden', paddingBottom: 20, shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.05, shadowRadius: 12, elevation: 3 },
    visionImage: { width: '100%', height: 160, marginBottom: 16 },
    visionTitle: { fontSize: 18, fontWeight: 'bold', paddingHorizontal: 20, marginBottom: 8 },
    visionDesc: { fontSize: 14, lineHeight: 22, paddingHorizontal: 20, marginBottom: 16 },
    visionLink: { flexDirection: 'row', alignItems: 'center', gap: 4, paddingHorizontal: 20 },
    visionLinkText: { fontSize: 14, fontWeight: 'bold' },

    // THÊM MỚI: Nút bấm AI
    aiUpdateBtn: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 6,
        paddingHorizontal: 12,
        paddingVertical: 6,
        borderRadius: 20,
        backgroundColor: 'rgba(26, 35, 126, 0.1)',
        alignSelf: 'flex-start',
        marginLeft: 20,
        marginBottom: 12,
        marginTop: 4
    },
    aiUpdateBtnText: {
        fontSize: 12,
        fontWeight: '700',
    },
});