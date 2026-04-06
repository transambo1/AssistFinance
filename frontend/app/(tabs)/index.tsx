import React, { useState, useMemo } from 'react';
import {
  View, Text, ScrollView, TouchableOpacity, Image, StyleSheet,
  ActivityIndicator, Modal, Alert
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { USER_DATA } from '../../src/mock/data';
import { MaterialIcons, Ionicons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useTheme } from '../../src/context/ThemeContext';
import { userService } from '../../src/api/userService';
import { useQuery } from '@tanstack/react-query';
import { User, Transaction } from '../../src/types/index';
import { formatMoney } from '../../src/utils/formatters';
import { aiService } from '../../src/api/aiService';
import { transactionService } from '@/src/api/transactionService';


export default function DashboardScreen() {
  const router = useRouter();
  const { isDark, colors } = useTheme();
  const styles = getStyles(colors, isDark);

  const [aiResult, setAiResult] = useState<{ title: string; description: string } | null>(null);
  const [isAiLoading, setIsAiLoading] = useState(false);
  const [showAiModal, setShowAiModal] = useState(false);
  // Thêm state này vào đầu component AIChatScreen
  const [editingTransaction, setEditingTransaction] = useState<any>(null); // Lưu thông tin giao dịch đang sửa
  // --- QUERIES CHỐNG XUNG ĐỘT CACHE ---
  const { data: rawUserData, isLoading: isUserLoading } = useQuery<any>({
    queryKey: ['userProfile'],
    queryFn: () => userService.getProfile(),
  });

  const { data: rawTxData, isLoading: isTransactionsLoading } = useQuery<any>({
    queryKey: ['transactions'],
    queryFn: () => transactionService.getAll(),
  });

  // --- BÓC TÁCH DỮ LIỆU AN TOÀN ---
  const userData: User | null = useMemo(() => {
    if (!rawUserData) return null;
    return rawUserData?.data || rawUserData?.result || rawUserData;
  }, [rawUserData]);

  const transactionsData = useMemo(() => {
    if (!rawTxData) return [];
    if (Array.isArray(rawTxData)) return rawTxData;
    return rawTxData?.data?.data || rawTxData?.result?.data || rawTxData?.data || [];
  }, [rawTxData]);

  const recentTransactions = Array.isArray(transactionsData) ? transactionsData.slice(0, 5) : [];

  // --- 1. GIỚI HẠN HIỂN THỊ TỔNG SỐ DƯ (MAX 999 TỶ) ---
  const formatCappedBalance = (balance: number) => {
    const MAX_LIMIT = 999000000000;
    const finalBalance = balance > MAX_LIMIT ? MAX_LIMIT : balance;
    return formatMoney(finalBalance);
  };

  // --- 2. TÍNH THU NHẬP / CHI TIÊU HÔM NAY ---
  const todayStats = useMemo(() => {
    let income = 0;
    let expense = 0;

    if (Array.isArray(transactionsData) && transactionsData.length > 0) {
      const today = new Date();
      const todayStr = new Date(today.getTime() - (today.getTimezoneOffset() * 60000)).toISOString().split('T')[0];

      transactionsData.forEach((t: any) => {
        const dateStr = t.transactionDate || t.createdAt || "";
        const txDate = String(dateStr).substring(0, 10);

        if (txDate === todayStr) {
          if (t.type === 'INCOME') income += t.amount;
          if (t.type === 'EXPENSE') expense += t.amount;
        }
      });
    }
    return { income, expense };
  }, [transactionsData]);

  // --- 3. TÍNH % BIẾN ĐỘNG THU NHẬP SO VỚI THÁNG TRƯỚC ---
  const trendData = useMemo(() => {
    if (!Array.isArray(transactionsData) || transactionsData.length === 0) return { displayPercent: "0", isUp: true };

    const now = new Date();
    const currentMonth = now.getMonth();
    const currentYear = now.getFullYear();

    const lastMonth = currentMonth === 0 ? 11 : currentMonth - 1;
    const lastMonthYear = currentMonth === 0 ? currentYear - 1 : currentYear;

    let currentMonthIncome = 0;
    let lastMonthIncome = 0;

    transactionsData.forEach((t: any) => {
      if (t.type !== 'INCOME') return;

      const dateStr = t.transactionDate || t.createdAt || "";
      const txDate = new Date(String(dateStr).replace(' ', 'T'));

      if (isNaN(txDate.getTime())) return;

      if (txDate.getMonth() === currentMonth && txDate.getFullYear() === currentYear) {
        currentMonthIncome += t.amount;
      } else if (txDate.getMonth() === lastMonth && txDate.getFullYear() === lastMonthYear) {
        lastMonthIncome += t.amount;
      }
    });

    if (lastMonthIncome === 0) return { displayPercent: currentMonthIncome > 0 ? "100" : "0", isUp: true };

    const diff = currentMonthIncome - lastMonthIncome;
    const percentChange = (Math.abs(diff) / lastMonthIncome) * 100;

    let displayPercent = Math.round(percentChange).toString();
    if (percentChange > 999) displayPercent = "999+";

    return {
      displayPercent: displayPercent,
      isUp: diff >= 0
    };
  }, [transactionsData]);

  const handleAiInsight = async () => {
    setIsAiLoading(true);
    setShowAiModal(true);
    const balance = userData?.currentBalance?.toString() || "0";
    const result = await aiService.generateFinancialVision(balance, "05");
    setAiResult(result);
    setIsAiLoading(false);
  };

  const aiServices = [
    { id: 'transactions', name: 'Giao dịch', icon: 'receipt', color: '#6200ee', route: '/add-transaction' },
    { id: 'budget', name: 'Ngân sách', icon: 'account-balance', color: '#00c853', route: '/budget' },
    { id: 'chatAI', name: 'Gợi ý AI', icon: 'tips-and-updates', color: '#ffab00', action: handleAiInsight },
    { id: 'hub', name: 'Thống kê', icon: 'pie-chart', color: '#d500f9', route: '/history' },
  ];

  if (isUserLoading || isTransactionsLoading) {
    return (
      <SafeAreaView style={[styles.container, { justifyContent: 'center', alignItems: 'center' }]}>
        <ActivityIndicator size="large" color="#1a237e" />
        <Text style={{ marginTop: 12, color: colors.textDim }}>Đang tải dữ liệu...</Text>
      </SafeAreaView>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <View style={styles.headerLeft}>
          <Image source={{ uri: userData?.photoUrl || USER_DATA.avatar }} style={styles.avatar} />
          <Text style={styles.headerTitle}>{userData?.fullName || userData?.username || 'Khách'}</Text>
        </View>
        <TouchableOpacity><MaterialIcons name="notifications" size={28} color={colors.text} /></TouchableOpacity>
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>

        {/* --- TỔNG SỐ DƯ (Full số, max 999 tỷ) --- */}
        <View style={styles.section}>

          <View style={styles.balanceCard}>
            <MaterialIcons name="account-balance-wallet" size={100} color="rgba(255,255,255,0.1)" style={styles.balanceIconBg} />
            <Text style={styles.sectionLabel}>TỔNG SỐ DƯ KHẢ DỤNG</Text>
            <Text style={styles.balanceAmount} numberOfLines={1} adjustsFontSizeToFit>
              {formatCappedBalance(userData?.currentBalance || 0)}
            </Text>
            <Text style={styles.balanceCurrency}>VND</Text>

            {/* --- TREND BADGE (Phần Trăm Làm Gọn) --- */}
            <View style={[styles.trendBadge, { backgroundColor: trendData.isUp ? 'rgba(115, 255, 122, 0.2)' : 'rgba(255, 99, 95, 0.15)' }]}>
              <MaterialIcons name={trendData.isUp ? "trending-up" : "trending-down"} size={16} color={trendData.isUp ? "#a3f69c" : "#ff635f"} />
              <Text style={[styles.trendText, { color: trendData.isUp ? "#a3f69c" : "#ff635f" }]}>
                {trendData.isUp ? '+' : '-'}{trendData.displayPercent}% thu nhập so với tháng trước
              </Text>
            </View>
          </View>
        </View>

        <View style={styles.aiHubContainer}>
          <Text style={styles.sectionTitleSmall}>DỊCH VỤ THÔNG MINH</Text>
          <View style={styles.servicesGrid}>
            {aiServices.map((service) => (
              <TouchableOpacity
                key={service.id}
                style={styles.serviceItem}
                onPress={() => service.action ? service.action() : router.push(service.route as any)}
              >
                <View style={[styles.serviceIconCircle, { backgroundColor: isDark ? '#1a237e' : service.color + '15' }]}>
                  <MaterialIcons name={service.icon as any} size={26} color={isDark ? '#a0abff' : service.color} />
                </View>
                <Text style={styles.serviceLabel}>{service.name}</Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        {/* --- BENTO GRID (Full số, không rớt dòng) --- */}
        <View style={styles.sectionHeaderRow}>
          <Text style={styles.sectionTitleSmall}>GIAO DỊCH HÔM NAY</Text>
        </View>
        <View style={styles.bentoGrid}>
          <View style={styles.bentoCard}>
            <View style={[styles.iconBox, { backgroundColor: 'rgba(160, 243, 153, 0.3)' }]}>
              <MaterialIcons name="arrow-downward" size={24} color={isDark ? '#a0f399' : '#1b6d24'} />
            </View>
            <Text style={styles.bentoLabel}>Thu nhập</Text>
            <Text style={styles.bentoValue} numberOfLines={1} adjustsFontSizeToFit>
              {formatMoney(todayStats.income)}
            </Text>
          </View>
          <View style={styles.bentoCard}>
            <View style={[styles.iconBox, { backgroundColor: 'rgba(255, 99, 95, 0.15)' }]}>
              <MaterialIcons name="arrow-upward" size={24} color="#ff635f" />
            </View>
            <Text style={styles.bentoLabel}>Chi tiêu</Text>
            <Text style={styles.bentoValue} numberOfLines={1} adjustsFontSizeToFit>
              {formatMoney(todayStats.expense)}
            </Text>
          </View>
        </View>

        <View style={styles.sectionHeaderRow}>
          <Text style={styles.sectionTitle}>Giao dịch gần đây</Text>
          <TouchableOpacity onPress={() => router.push('/history')}><Text style={styles.seeAllText}>Tất cả</Text></TouchableOpacity>
        </View>

        {recentTransactions.map((item: any) => (
          <TouchableOpacity key={item.id} style={styles.transactionItem} onPress={() => router.push(`/transaction-detail?id=${item.id}`)}>
            <View style={styles.transactionLeft}>
              <View style={[styles.transactionIconBox, { backgroundColor: isDark ? '#2C2C3E' : `${item.categoryColor ?? '#1a237e'}20` }]}>
                <MaterialIcons name={(item.categoryIcon as any) || 'receipt'} size={24} color={isDark ? '#a0abff' : (item.categoryColor ?? '#1a237e')} />
              </View>
              <View>
                <Text style={styles.transactionName}>{item.categoryName}</Text>
                <Text style={styles.transactionDate}>{item.transactionDate || item.createdAt}</Text>
              </View>
            </View>
            <Text style={[styles.transactionAmount, { color: item.type === 'EXPENSE' ? '#ff635f' : (isDark ? '#a0f399' : '#1b6d24') }]}>
              {item.type === 'EXPENSE' ? '-' : '+'}{item.amount.toLocaleString('vi-VN')}
            </Text>
          </TouchableOpacity>
        ))}
        {recentTransactions.length === 0 && (
          <Text style={{ textAlign: 'center', color: colors.textDim, marginTop: 10, fontStyle: 'italic' }}>Chưa có giao dịch nào.</Text>
        )}
      </ScrollView>

      <Modal visible={showAiModal} transparent animationType="fade">
        <View style={styles.modalOverlay}>
          <View style={styles.aiModalContent}>
            {isAiLoading ? (
              <View style={styles.aiLoadingContainer}>
                <ActivityIndicator size="large" color="#1a237e" />
                <Text style={styles.aiLoadingText}>Aeon AI đang phân tích ví của bạn...</Text>
              </View>
            ) : (
              <View>
                <View style={styles.aiModalHeader}>
                  <MaterialIcons name="auto-awesome" size={24} color="#ffab00" />
                  <Text style={styles.aiModalTitle}>{aiResult?.title}</Text>
                </View>
                <Text style={styles.aiModalDesc}>{aiResult?.description}</Text>
                <TouchableOpacity style={styles.aiCloseBtn} onPress={() => setShowAiModal(false)}>
                  <Text style={styles.aiCloseBtnText}>Đã hiểu</Text>
                </TouchableOpacity>
              </View>
            )}
          </View>
        </View>
      </Modal>

      <TouchableOpacity style={styles.fab} onPress={() => router.push('/add-transaction')}>
        <MaterialIcons name="add" size={32} color="#ffffff" />
      </TouchableOpacity>
    </SafeAreaView>
  );
}

const getStyles = (colors: any, isDark: boolean) => StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 24, paddingTop: 16, paddingBottom: 16 },
  headerLeft: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  avatar: { width: 40, height: 40, borderRadius: 20 },
  headerTitle: { fontSize: 20, fontWeight: 'bold', color: colors.text },
  scrollContent: { paddingHorizontal: 24, paddingBottom: 120, paddingTop: 8, gap: 20 },
  section: { gap: 12 },
  sectionLabel: { fontSize: 11, fontWeight: '900', letterSpacing: 1, textTransform: 'uppercase', color: colors.textDim },
  balanceCard: { backgroundColor: '#1a237e', padding: 24, borderRadius: 28, overflow: 'hidden', position: 'relative' },
  balanceIconBg: { position: 'absolute', top: 16, right: 16 },
  balanceAmount: { fontSize: 40, maxWidth: '100%', fontWeight: '700', color: '#ffffff', marginBottom: 4 },
  balanceCurrency: { fontSize: 18, color: 'rgba(255,255,255,0.8)', fontWeight: '500' },
  trendBadge: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 12, paddingVertical: 8, borderRadius: 20, alignSelf: 'flex-start', marginTop: 24, gap: 6 },
  trendText: { color: '#ffffff', fontSize: 13, fontWeight: '600' },
  bentoGrid: { flexDirection: 'row', gap: 16 },
  bentoCard: { flex: 1, padding: 20, borderRadius: 24, gap: 8, backgroundColor: colors.surface, elevation: 2, overflow: 'hidden' },
  iconBox: { width: 40, height: 40, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
  bentoLabel: { fontSize: 13, fontWeight: '500', color: colors.textDim },
  bentoValue: { fontSize: 22, fontWeight: 'bold', color: colors.text },
  aiHubContainer: { backgroundColor: colors.surface, padding: 20, borderRadius: 28, gap: 16, elevation: 3 },
  sectionTitleSmall: { fontSize: 11, fontWeight: '700', letterSpacing: 1, color: colors.textDim, textTransform: 'uppercase' },
  servicesGrid: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' },
  serviceItem: { width: '22%', alignItems: 'center', gap: 8 },
  serviceIconCircle: { width: 50, height: 50, borderRadius: 18, alignItems: 'center', justifyContent: 'center' },
  serviceLabel: { fontSize: 11, fontWeight: '600', textAlign: 'center', color: colors.text, lineHeight: 14 },
  sectionHeaderRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8, marginTop: 10 },
  sectionTitle: { fontSize: 20, fontWeight: 'bold', color: isDark ? '#ffffff' : '#000666' },
  seeAllText: { fontSize: 14, fontWeight: '600', color: isDark ? '#ffffff' : '#000666' },
  transactionItem: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', padding: 16, borderRadius: 16, marginBottom: 8, backgroundColor: colors.surface, elevation: 1 },
  transactionLeft: { flexDirection: 'row', alignItems: 'center', gap: 16 },
  transactionIconBox: { width: 48, height: 48, borderRadius: 24, alignItems: 'center', justifyContent: 'center' },
  transactionName: { fontSize: 15, fontWeight: 'bold', marginBottom: 4, color: colors.text },
  transactionDate: { fontSize: 12, color: colors.textDim },
  transactionAmount: { fontSize: 16, fontWeight: 'bold' },
  fab: { position: 'absolute', right: 24, bottom: 100, width: 56, height: 56, borderRadius: 28, backgroundColor: '#1a237e', alignItems: 'center', justifyContent: 'center', elevation: 6 },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'center', alignItems: 'center', padding: 24 },
  aiModalContent: { backgroundColor: '#fff', width: '100%', borderRadius: 32, padding: 24, elevation: 10 },
  aiLoadingContainer: { alignItems: 'center', paddingVertical: 20 },
  aiLoadingText: { marginTop: 16, color: '#1a237e', fontWeight: '600' },
  aiModalHeader: { flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 16 },
  aiModalTitle: { fontSize: 20, fontWeight: 'bold', color: '#1a237e' },
  aiModalDesc: { fontSize: 15, lineHeight: 24, color: '#454652', marginBottom: 24 },
  aiCloseBtn: { backgroundColor: '#1a237e', paddingVertical: 14, borderRadius: 16, alignItems: 'center' },
  aiCloseBtnText: { color: '#fff', fontWeight: 'bold', fontSize: 16 }
});