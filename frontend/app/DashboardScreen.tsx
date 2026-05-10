import React, { useState, useMemo } from 'react';
import {
    View,
    Text,
    StyleSheet,
    TouchableOpacity,
    ActivityIndicator,
    ScrollView,
    SafeAreaView,
    RefreshControl,
} from 'react-native';
import { MaterialIcons, FontAwesome5, Feather } from '@expo/vector-icons';
import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
// Import thư viện biểu đồ tròn
import { PieChart } from 'react-native-gifted-charts'; 

import { dashboardService } from '../src/api/dashboardService';
import { useTheme } from '../src/context/ThemeContext';
import { formatMoney } from '../src/utils/formatters';

// --- INTERFACES ---
interface ChartDataPoint {
    label: string;
    amount: string | number;
    forecast: boolean; 
}

interface DashboardData {
    currency?: string;
    currentBalance?: number;
    totalExpense?: number;   
    totalIncome?: number;    
    expenseChart?: ChartDataPoint[];
    incomeChart?: ChartDataPoint[];  
    chartData?: ChartDataPoint[];    
    aiAnalysis: string;
}

const formatShortMoney = (num: number) => {
    if (num === 0) return '0';
    const absNum = Math.abs(num);
    const sign = num < 0 ? '-' : '';
    
    if (absNum >= 1e12) return sign + (absNum / 1e12).toFixed(1) + ' NT';
    if (absNum >= 1e9) return sign + (absNum / 1e9).toFixed(1) + ' Tỷ';
    if (absNum >= 1e6) return sign + (absNum / 1e6).toFixed(1) + ' Tr';
    if (absNum >= 1e3) return sign + (absNum / 1e3).toFixed(1) + ' K';
    return sign + absNum.toString();
};

// --- MOCK DATA CHO PIE CHART ---
const mockPieExpense = [
    { value: 45, color: '#FF8A65', text: '45%', label: 'Ăn uống' },
    { value: 22, color: '#F48FB1', text: '22%', label: 'Người thân' },
    { value: 20, color: '#CE93D8', text: '20%', label: 'Chưa phân loại' },
    { value: 7,  color: '#EF9A9A', text: '7%',  label: 'Giải trí' },
    { value: 6,  color: '#80CBC4', text: '6%',  label: 'Hóa đơn' },
];

const mockPieIncome = [
    { value: 70, color: '#81C784', text: '70%', label: 'Lương' },
    { value: 20, color: '#64B5F6', text: '20%', label: 'Thưởng' },
    { value: 10, color: '#FFD54F', text: '10%', label: 'Khác' },
];

export default function DashboardScreen() {
    const router = useRouter();
    const { isDark, colors } = useTheme();
    const styles = getStyles(colors, isDark);

    const [currentYear, setCurrentYear] = useState(new Date().getFullYear());
    const [isBalanceHidden, setIsBalanceHidden] = useState(false);
    const [activeTab, setActiveTab] = useState<'expense' | 'income'>('expense');
    
    // STATE MỚI: Quản lý xem đang hiển thị biểu đồ nào
    const [chartType, setChartType] = useState<'bar' | 'pie'>('pie');

    // --- FETCH DATA ---
    const { data: dashboardData, isLoading, isRefetching, refetch } = useQuery({
        queryKey: ['dashboard', currentYear],
        queryFn: async () => {
            try {
                const res = await dashboardService.getForecast(currentYear);
                const finalData = res?.data ? res.data : res;
                return (finalData as DashboardData) || { chartData: [], aiAnalysis: '', currentBalance: 0 };
            } catch (error) {
                console.error("Lỗi fetch dashboard:", error);
                throw error;
            }
        },
    });

    // --- LOGIC TÁCH BIỆT DỮ LIỆU BIỂU ĐỒ CỘT ---
    const { chartItems, maxAmount, totalDisplayAmount } = useMemo(() => {
        const rawData = activeTab === 'expense' 
            ? (dashboardData?.expenseChart || dashboardData?.chartData || [])
            : (dashboardData?.incomeChart || []); 

        const items = rawData.map(item => {
            const val = parseFloat(item.amount?.toString()) || 0;
            return { ...item, numericAmount: val, absAmount: Math.abs(val) };
        });

        const total = items.reduce((sum, i) => sum + i.absAmount, 0);
        const max = items.reduce((prev, curr) => Math.max(prev, curr.absAmount), 0) || 1;

        return { chartItems: items, maxAmount: max, totalDisplayAmount: total };
    }, [dashboardData, activeTab]);

    const BAR_MAX_HEIGHT = 150;

    return (
        <SafeAreaView style={styles.safeArea}>
            <ScrollView
                style={styles.container}
                showsVerticalScrollIndicator={false}
                refreshControl={<RefreshControl refreshing={isRefetching} onRefresh={refetch} tintColor={isDark ? "#FFF" : "#A92759"} />}
            >
                {/* --- HEADER --- */}
                <View style={styles.headerBackground}>
                    <View style={styles.headerTop}>
                        <TouchableOpacity onPress={() => router.back()} style={styles.iconBtn}>
                            <MaterialIcons name="arrow-back" size={24} color="#FFF" />
                        </TouchableOpacity>
                        <Text style={styles.headerTitle}>Tình hình thu chi</Text>
                        
                        {/* NÚT TOGGLE CHUYỂN ĐỔI CHART */}
                        <TouchableOpacity onPress={() => setChartType(prev => prev === 'bar' ? 'pie' : 'bar')} style={styles.iconBtn}>
                            <Feather name={chartType === 'bar' ? "pie-chart" : "bar-chart-2"} size={22} color="#FFF" />
                        </TouchableOpacity>
                    </View>

                    <View style={styles.balanceWrapper}>
                        <Text style={styles.balanceLabel}>Tổng số dư hiện tại</Text>
                        <View style={styles.balanceAmountRow}>
                            <Text style={styles.balanceAmount} numberOfLines={1}>
                                {isBalanceHidden ? '******' : formatMoney(dashboardData?.currentBalance || 0)}
                            </Text>
                            <TouchableOpacity onPress={() => setIsBalanceHidden(!isBalanceHidden)} style={{ marginLeft: 10 }}>
                                <MaterialIcons name={isBalanceHidden ? "visibility-off" : "visibility"} size={22} color="#FFF" />
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>

                {/* --- NỘI DUNG CHÍNH --- */}
                <View style={styles.contentBody}>
                    <View style={styles.yearSelector}>
                        {/* Các nút chọn năm giữ nguyên */}
                        <TouchableOpacity onPress={() => setCurrentYear(y => y - 1)} style={styles.yearBtn}>
                            <MaterialIcons name="chevron-left" size={24} color={colors.text} />
                        </TouchableOpacity>
                        <View style={styles.yearLabelWrapper}>
                            <MaterialIcons name="calendar-today" size={16} color={colors.text} style={{marginRight: 6}} />
                            <Text style={styles.yearText}>Năm {currentYear}</Text>
                        </View>
                        <TouchableOpacity onPress={() => setCurrentYear(y => y + 1)} style={styles.yearBtn} disabled={currentYear >= new Date().getFullYear()}>
                            <MaterialIcons name="chevron-right" size={24} color={currentYear >= new Date().getFullYear() ? '#CCC' : colors.text} />
                        </TouchableOpacity>
                    </View>

                    {/* HAI THẺ THU NHẬP / CHI TIÊU KHU VỰC KHOANH ĐỎ TRONG HÌNH */}
                    <View style={styles.tabContainer}>
                        <TouchableOpacity 
                            style={[styles.tabBox, activeTab === 'expense' && styles.tabExpenseActive]}
                            onPress={() => setActiveTab('expense')}
                        >
                            <View style={styles.tabHeader}>
                                <Feather name="trending-down" size={16} color={activeTab === 'expense' ? '#A92759' : '#9CA3AF'} />
                                <Text style={[styles.tabTitle, activeTab === 'expense' && { color: '#A92759' }]}>Chi tiêu</Text>
                            </View>
                            <Text style={[styles.tabAmount, activeTab === 'expense' && { color: '#A92759' }]} numberOfLines={1}>
                                {formatShortMoney(activeTab === 'expense' ? totalDisplayAmount : (dashboardData?.totalExpense || 0))}
                            </Text>
                        </TouchableOpacity>

                        <TouchableOpacity 
                            style={[styles.tabBox, activeTab === 'income' && styles.tabIncomeActive]}
                            onPress={() => setActiveTab('income')}
                        >
                            <View style={styles.tabHeader}>
                                <Feather name="trending-up" size={16} color={activeTab === 'income' ? '#10B981' : '#9CA3AF'} />
                                <Text style={[styles.tabTitle, activeTab === 'income' && { color: '#10B981' }]}>Thu nhập</Text>
                            </View>
                            <Text style={[styles.tabAmount, activeTab === 'income' && { color: '#10B981' }]} numberOfLines={1}>
                                {formatShortMoney(activeTab === 'income' ? totalDisplayAmount : (dashboardData?.totalIncome || 0))}
                            </Text>
                        </TouchableOpacity>
                    </View>

                    {/* KHU VỰC RENDER CHART: KIỂM TRA CHART TYPE */}
                    <View style={styles.chartCard}>
                        {isLoading ? (
                            <ActivityIndicator color="#A92759" size="large" style={{ marginVertical: 50 }} />
                        ) : chartType === 'pie' ? (
                            
                            // ---------------- BIỂU ĐỒ TRÒN (PIE) ----------------
                            <View style={styles.pieContainer}>
                                <PieChart
                                    donut
                                    innerRadius={70}
                                    radius={110}
                                    data={activeTab === 'expense' ? mockPieExpense : mockPieIncome}
                                    centerLabelComponent={() => (
                                        <View style={{justifyContent: 'center', alignItems: 'center'}}>
                                            <Text style={{fontSize: 22, color: colors.text, fontWeight: 'bold'}}>100%</Text>
                                            <Text style={{fontSize: 12, color: '#9CA3AF'}}>Tổng cộng</Text>
                                        </View>
                                    )}
                                />
                                
                                {/* CHÚ THÍCH (LEGEND) CHO PIE CHART */}
                                <View style={styles.legendContainer}>
                                    {(activeTab === 'expense' ? mockPieExpense : mockPieIncome).map((item, index) => (
                                        <View key={index} style={styles.legendItem}>
                                            <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                                                <View style={[styles.legendDot, { backgroundColor: item.color }]} />
                                                <Text style={styles.legendLabel}>{item.label}</Text>
                                            </View>
                                            <Text style={styles.legendValue}>{item.text}</Text>
                                        </View>
                                    ))}
                                </View>
                            </View>
                        ) : chartItems.length === 0 ? (
                            <Text style={styles.emptyText}>Chưa có dữ liệu mục này</Text>
                        ) : (
                            
                            // ---------------- BIỂU ĐỒ CỘT (BAR - CODE CŨ) ----------------
                            <View style={styles.chartLayout}>
                                <View style={styles.yAxisContainer}>
                                    <View style={styles.yAxisRow}><Text style={styles.yAxisLabel}>{formatShortMoney(maxAmount)}</Text><View style={styles.gridLine} /></View>
                                    <View style={styles.yAxisRow}><Text style={styles.yAxisLabel}>{formatShortMoney(maxAmount / 2)}</Text><View style={styles.gridLine} /></View>
                                    <View style={[styles.yAxisRow, { borderBottomWidth: 0 }]}><Text style={styles.yAxisLabel}>0</Text><View style={[styles.gridLine, { borderBottomColor: isDark ? '#555' : '#CCC' }]} /></View>
                                </View>

                                <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.chartScrollArea}>
                                    <View style={styles.barsWrapper}>
                                        {chartItems.map((item, index) => {
                                            const calculatedHeight = (item.absAmount / maxAmount) * BAR_MAX_HEIGHT;
                                            const finalBarHeight = item.absAmount > 0 ? Math.max(calculatedHeight, 8) : 0;
                                            const columnColor = activeTab === 'expense' ? '#A92759' : '#10B981';

                                            return (
                                                <View key={`${item.label}-${index}`} style={styles.barColumn}>
                                                    <View style={styles.barValueWrapper}>
                                                        <Text style={[styles.barValueText, item.numericAmount < 0 && { color: '#EF4444' }]} numberOfLines={1}>
                                                            {formatShortMoney(item.numericAmount)}
                                                        </Text>
                                                    </View>
                                                    <View style={[styles.bar, { height: finalBarHeight, backgroundColor: columnColor, opacity: item.forecast ? 0.5 : 1 }]} />
                                                    <Text style={[styles.barLabel, item.forecast && { fontWeight: 'bold' }]}>{item.label}</Text>
                                                </View>
                                            );
                                        })}
                                    </View>
                                </ScrollView>
                            </View>
                        )}
                    </View>

                    {/* AI ANALYSIS */}
                    {dashboardData?.aiAnalysis ? (
                        <View style={styles.aiCard}>
                            <View style={styles.aiHeader}>
                                <View style={styles.aiIconBox}><FontAwesome5 name="magic" size={14} color="#A92759" /></View>
                                <Text style={styles.aiTitle}>Phân tích {activeTab === 'expense' ? 'chi tiêu' : 'thu nhập'}</Text>
                            </View>
                            <Text style={styles.aiText}>{dashboardData.aiAnalysis}</Text>
                        </View>
                    ) : null}
                    <View style={{ height: 40 }} />
                </View>
            </ScrollView>
        </SafeAreaView>
    );
}

// --- STYLES ---
const getStyles = (colors: any, isDark: boolean) => StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: isDark ? '#121212' : '#A92759' },
    container: { flex: 1, backgroundColor: isDark ? '#121212' : '#F9FAFB' },
    headerBackground: { backgroundColor: isDark ? '#1E1E1E' : '#A92759', paddingTop: 10, paddingBottom: 30, paddingHorizontal: 20 },
    headerTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 },
    iconBtn: { padding: 4, width: 40, alignItems: 'center' },
    headerTitle: { fontSize: 18, fontWeight: 'bold', color: '#FFF' },
    balanceWrapper: { alignItems: 'center', marginBottom: 10 },
    balanceLabel: { fontSize: 13, color: '#FFD1DC', marginBottom: 6 },
    balanceAmountRow: { flexDirection: 'row', alignItems: 'center' },
    balanceAmount: { fontSize: 32, fontWeight: 'bold', color: '#FFF' },
    contentBody: { flex: 1, backgroundColor: isDark ? '#121212' : '#F9FAFB', borderTopLeftRadius: 24, borderTopRightRadius: 24, paddingHorizontal: 16, paddingTop: 20 },
    yearSelector: { flexDirection: 'row', justifyContent: 'center', alignItems: 'center', marginBottom: 20 },
    yearBtn: { padding: 8, backgroundColor: isDark ? '#2C2C2C' : '#FFF', borderRadius: 12, elevation: 1 },
    yearLabelWrapper: { flexDirection: 'row', alignItems: 'center', marginHorizontal: 20 },
    yearText: { fontSize: 16, fontWeight: 'bold', color: colors.text },
    tabContainer: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 20 },
    tabBox: { flex: 1, backgroundColor: colors.surface, padding: 16, borderRadius: 16, borderWidth: 1.5, borderColor: isDark ? '#333' : '#E5E7EB', marginHorizontal: 4, elevation: 2 },
    tabExpenseActive: { borderColor: '#A92759', backgroundColor: isDark ? '#2A1A24' : '#FFF0F5' },
    tabIncomeActive: { borderColor: '#10B981', backgroundColor: isDark ? '#064E3B' : '#ECFDF5' },
    tabHeader: { flexDirection: 'row', alignItems: 'center', marginBottom: 8 },
    tabTitle: { fontSize: 13, color: '#6B7280', marginLeft: 6, fontWeight: '600' },
    tabAmount: { fontSize: 19, fontWeight: 'bold', color: colors.text },
    chartCard: { backgroundColor: colors.surface, borderRadius: 20, paddingVertical: 20, paddingHorizontal: 10, marginBottom: 20, elevation: 2 },
    emptyText: { textAlign: 'center', color: '#9CA3AF', paddingVertical: 30 },
    
    // Bar Chart
    chartLayout: { position: 'relative', height: 220, marginTop: 10 },
    yAxisContainer: { position: 'absolute', top: 30, left: 0, right: 0, height: 150, justifyContent: 'space-between' },
    yAxisRow: { flexDirection: 'row', alignItems: 'flex-end', flex: 1 },
    yAxisLabel: { width: 45, fontSize: 10, color: '#9CA3AF', textAlign: 'right', paddingRight: 8, paddingBottom: 2 },
    gridLine: { flex: 1, borderBottomWidth: 1, borderBottomColor: isDark ? '#333' : '#F3F4F6', borderStyle: 'dashed' },
    chartScrollArea: { marginLeft: 45 }, 
    barsWrapper: { flexDirection: 'row', alignItems: 'flex-end', height: 190 }, 
    barColumn: { alignItems: 'center', marginHorizontal: 12, width: 45, justifyContent: 'flex-end', height: '100%' }, 
    barValueWrapper: { height: 20, justifyContent: 'flex-end', marginBottom: 4 },
    barValueText: { fontSize: 10, color: '#6B7280', fontWeight: 'bold' },
    bar: { width: 26, borderTopLeftRadius: 6, borderTopRightRadius: 6 },
    barLabel: { fontSize: 11, color: '#9CA3AF', marginTop: 8 },
    
    // Pie Chart
    pieContainer: { alignItems: 'center', marginTop: 10 },
    legendContainer: { width: '100%', marginTop: 24, paddingHorizontal: 10 },
    legendItem: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 },
    legendDot: { width: 12, height: 12, borderRadius: 6, marginRight: 10 },
    legendLabel: { fontSize: 14, color: colors.text },
    legendValue: { fontSize: 14, fontWeight: 'bold', color: colors.text },

    aiCard: { backgroundColor: isDark ? '#1E1E1E' : '#FFF', borderRadius: 20, padding: 20, borderWidth: 1, borderColor: isDark ? '#333' : '#FCE7F3' },
    aiHeader: { flexDirection: 'row', alignItems: 'center', marginBottom: 12 },
    aiIconBox: { backgroundColor: '#FCE7F3', width: 32, height: 32, borderRadius: 16, justifyContent: 'center', alignItems: 'center', marginRight: 12 },
    aiTitle: { fontSize: 15, fontWeight: 'bold', color: isDark ? '#E5E7EB' : '#A92759' },
    aiText: { fontSize: 14, color: isDark ? '#D1D5DB' : '#4B5563', lineHeight: 22 },
});