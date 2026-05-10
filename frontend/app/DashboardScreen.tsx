import React, { useState, useMemo } from 'react';
import {
    View, Text, StyleSheet, TouchableOpacity, ActivityIndicator,
    ScrollView, SafeAreaView, RefreshControl,
} from 'react-native';
import { MaterialIcons, FontAwesome5, Feather } from '@expo/vector-icons';
import { useQuery } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import { PieChart } from 'react-native-gifted-charts'; 
import { formatMoney } from '../src/utils/formatters';
import { 
    dashboardService, 
    DashboardForecastResponse, 
    DashboardAnalyticsResponse,
    CategoryDataPoint,
    MonthlyDataPoint 
} from '../src/api/dashboardService'; 
import { useTheme } from '../src/context/ThemeContext';

interface ComputedChartItem extends MonthlyDataPoint {
    numericAmount: number;
    absAmount: number;
}

const formatShortMoney = (val: number | string | undefined) => {
    const num = typeof val === 'string' ? parseFloat(val) : (val || 0);
    if (!num || isNaN(num) || num === 0) return '0đ';
    const absNum = Math.abs(num);
    const sign = num < 0 ? '-' : '';
    if (absNum >= 1e12) return sign + (absNum / 1e12).toFixed(1) + ' Tỷ';
    if (absNum >= 1e9) return sign + (absNum / 1e9).toFixed(1) + ' Tỷ';
    if (absNum >= 1e6) return sign + (absNum / 1e6).toFixed(1) + ' Tr';
    if (absNum >= 1e3) return sign + (absNum / 1e3).toFixed(1) + ' K';
    return sign + Math.round(absNum).toLocaleString() + 'đ';
};

// Hàm helper xử lý data biểu đồ cột
const computeChartData = (forecastData?: DashboardForecastResponse) => {
    const rawData: MonthlyDataPoint[] = forecastData?.chartData || [];
    const items: ComputedChartItem[] = rawData.map((item: MonthlyDataPoint) => {
        const val = typeof item.amount === 'string' ? parseFloat(item.amount) : (item.amount || 0);
        return { ...item, numericAmount: val, absAmount: Math.abs(val) };
    });
    const maxAmount = items.reduce((prev: number, curr: ComputedChartItem) => Math.max(prev, curr.absAmount), 0) || 1;
    
    // Tính tổng tiền thực tế (bỏ qua forecast)
    const totalAmount = items.reduce((sum, item) => {
        if (item.forecast) return sum;
        return sum + item.absAmount;
    }, 0);

    return { chartItems: items, maxAmount, totalAmount };
};

export default function DashboardScreen() {
    const router = useRouter();
    const { isDark, colors } = useTheme();
    const styles = getStyles(colors, isDark);

    const [currentYear, setCurrentYear] = useState(new Date().getFullYear());
    const [currentMonth, setCurrentMonth] = useState(new Date().getMonth() + 1);
    const [isYearlyView, setIsYearlyView] = useState(true);
    const [isBalanceHidden, setIsBalanceHidden] = useState(false);

    // --- QUERIES ---
    // 1. Gọi API Chi tiêu
    const { 
        data: expenseData, isLoading: isExpLoading, isRefetching: isExpRefetching, refetch: refetchExp 
    } = useQuery<DashboardForecastResponse, Error>({
        queryKey: ['dashboard-expense', currentYear], 
        queryFn: () => dashboardService.getForecast(currentYear, 'expense'), 
    });

    // 2. Gọi API Thu nhập
    const { 
        data: incomeData, isLoading: isIncLoading, isRefetching: isIncRefetching, refetch: refetchInc 
    } = useQuery<DashboardForecastResponse, Error>({
        queryKey: ['dashboard-income', currentYear], 
        queryFn: () => dashboardService.getForecast(currentYear, 'income'), 
    });

    // 3. Gọi API Analytics (Pie Chart) - Lắng nghe theo isYearlyView và currentMonth
    const { 
        data: analyticsData, isLoading: isAnalyticsLoading, isRefetching: isAnalyticsRefetching, refetch: refetchAnalytics 
    } = useQuery<DashboardAnalyticsResponse, Error>({
        queryKey: ['dashboard-analytics', currentYear, isYearlyView ? 0 : currentMonth], 
        queryFn: () => dashboardService.getAnalytics(currentYear, isYearlyView ? 0 : currentMonth),
    });

    const isScreenLoading = isExpLoading || isIncLoading || isAnalyticsLoading;
    const isScreenRefetching = isExpRefetching || isIncRefetching || isAnalyticsRefetching;

    // --- MAP DỮ LIỆU ---
    const expenseChart = useMemo(() => computeChartData(expenseData), [expenseData]);
    const incomeChart = useMemo(() => computeChartData(incomeData), [incomeData]);

    const pieData = useMemo(() => {
        const details: CategoryDataPoint[] = analyticsData?.categoryDistribution?.details || [];
        return details.map((item: CategoryDataPoint) => {
            const pValue = typeof item.percentage === 'string' ? parseFloat(item.percentage) : (item.percentage || 0);
            return {
                value: pValue,
                color: item.color || '#A92759',
                label: item.categoryName,
                text: pValue > 5 ? `${Math.round(pValue)}%` : '',
            };
        });
    }, [analyticsData]);

    const onRefresh = () => {
        refetchExp();
        refetchInc();
        refetchAnalytics();
    };

    const CHART_TOP_PADDING = 20; 
    const BAR_MAX_HEIGHT = 130; 
    const CHART_TOTAL_HEIGHT = CHART_TOP_PADDING + BAR_MAX_HEIGHT; 

    const totalExpenseVal = typeof analyticsData?.categoryDistribution?.totalExpense === 'string' 
        ? parseFloat(analyticsData.categoryDistribution.totalExpense) 
        : (analyticsData?.categoryDistribution?.totalExpense || expenseChart.totalAmount);

    // --- HÀM RENDER BIỂU ĐỒ CỘT DÙNG CHUNG ---
    const renderBarChart = (title: string, data: any, color: string, aiAnalysis?: string) => (
        <View style={{ marginBottom: 20 }}>
            <View style={styles.chartCard}>
                <Text style={[styles.sectionTitle, { color: colors.text }]}>{title}</Text>
                <View style={{ flexDirection: 'row', height: 210, marginTop: 10 }}>
                    <View style={{ width: 45, height: CHART_TOTAL_HEIGHT, position: 'relative' }}>
                        <Text style={[styles.yAxisLabel, { position: 'absolute', top: CHART_TOP_PADDING - 6, right: 8 }]}>{formatShortMoney(data.maxAmount)}</Text>
                        <Text style={[styles.yAxisLabel, { position: 'absolute', top: CHART_TOP_PADDING + BAR_MAX_HEIGHT / 2 - 6, right: 8 }]}>{formatShortMoney(data.maxAmount / 2)}</Text>
                        <Text style={[styles.yAxisLabel, { position: 'absolute', top: CHART_TOTAL_HEIGHT - 6, right: 8 }]}>0đ</Text>
                    </View>
                    <View style={{ flex: 1, position: 'relative' }}>
                        <View style={{ position: 'absolute', top: CHART_TOP_PADDING, left: 0, right: 0, height: 1, backgroundColor: isDark ? '#333' : '#E5E7EB', borderStyle: 'dashed' }} />
                        <View style={{ position: 'absolute', top: CHART_TOP_PADDING + BAR_MAX_HEIGHT / 2, left: 0, right: 0, height: 1, backgroundColor: isDark ? '#333' : '#E5E7EB', borderStyle: 'dashed' }} />
                        <View style={{ position: 'absolute', top: CHART_TOTAL_HEIGHT, left: 0, right: 0, height: 1, backgroundColor: isDark ? '#333' : '#E5E7EB' }} />
                        <ScrollView horizontal showsHorizontalScrollIndicator={false}>
                            <View style={{ flexDirection: 'row', alignItems: 'flex-end', height: CHART_TOTAL_HEIGHT }}>
                                {data.chartItems.map((item: any, index: number) => {
                                    const barHeight = (item.absAmount / data.maxAmount) * BAR_MAX_HEIGHT;
                                    const finalHeight = item.absAmount === 0 ? 2 : Math.max(barHeight, 2);
                                    return (
                                        <View key={index} style={{ width: 45, marginHorizontal: 10, alignItems: 'center', justifyContent: 'flex-end', height: CHART_TOTAL_HEIGHT }}>
                                            <Text style={{ fontSize: 10, color: '#6B7280', marginBottom: 4 }} numberOfLines={1}>{formatShortMoney(item.numericAmount)}</Text>
                                            <View style={{ height: finalHeight, width: 26, backgroundColor: color, opacity: item.forecast ? 0.4 : 1, borderTopLeftRadius: 6, borderTopRightRadius: 6 }} />
                                            <Text style={{ position: 'absolute', top: CHART_TOTAL_HEIGHT + 10, fontSize: 11, color: item.forecast ? (isDark ? '#FFF' : '#000') : '#9CA3AF', fontWeight: item.forecast ? 'bold' : 'normal' }}>
                                                {item.label}
                                            </Text>
                                        </View>
                                    );
                                })}
                            </View>
                        </ScrollView>
                    </View>
                </View>
            </View>
            {aiAnalysis ? (
                <View style={[styles.aiCard, { marginTop: -10 }]}>
                    <View style={styles.aiHeader}>
                        <View style={styles.aiIconBox}><FontAwesome5 name="magic" size={14} color={color} /></View>
                        <Text style={[styles.aiTitle, { color: isDark ? '#E5E7EB' : color }]}>Phân tích AI</Text>
                    </View>
                    <Text style={styles.aiText}>{aiAnalysis}</Text>
                </View>
            ) : null}
        </View>
    );

    return (
        <SafeAreaView style={styles.safeArea}>
            <ScrollView
                style={styles.container}
                showsVerticalScrollIndicator={false}
                refreshControl={<RefreshControl refreshing={isScreenRefetching} onRefresh={onRefresh} tintColor="#A92759" />}
            >
                {/* --- HEADER --- */}
                <View style={styles.headerBackground}>
                    <View style={styles.headerTop}>
                        <TouchableOpacity onPress={() => router.back()} style={styles.iconBtn}>
                            <MaterialIcons name="arrow-back" size={24} color="#FFF" />
                        </TouchableOpacity>
                        <Text style={styles.headerTitle}>Tình hình thu chi</Text>
                        <View style={styles.iconBtn} /> 
                    </View>
                    <View style={styles.balanceWrapper}>
                        <Text style={styles.balanceLabel}>Tổng số dư hiện tại</Text>
                        <View style={styles.balanceAmountRow}>
                            <Text style={styles.balanceAmount}>{isBalanceHidden ? '******' : formatMoney(expenseData?.currentBalance || incomeData?.currentBalance || 0)}</Text>
                            <TouchableOpacity onPress={() => setIsBalanceHidden(!isBalanceHidden)} style={{ marginLeft: 10 }}>
                                <MaterialIcons name={isBalanceHidden ? "visibility-off" : "visibility"} size={22} color="#FFF" />
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>

                {/* --- BODY --- */}
                <View style={styles.contentBody}>
                    <View style={styles.yearSelector}>
                        <TouchableOpacity onPress={() => setCurrentYear(y => y - 1)} style={styles.yearBtn}>
                            <MaterialIcons name="chevron-left" size={24} color={colors.text} />
                        </TouchableOpacity>
                        <View style={styles.yearLabelWrapper}>
                            <Text style={styles.yearText}>Năm {currentYear}</Text>
                        </View>
                        <TouchableOpacity onPress={() => setCurrentYear(y => y + 1)} style={styles.yearBtn} disabled={currentYear >= new Date().getFullYear()}>
                            <MaterialIcons name="chevron-right" size={24} color={currentYear >= new Date().getFullYear() ? '#CCC' : colors.text} />
                        </TouchableOpacity>
                    </View>

                    {/* --- THẺ TỔNG QUAN --- */}
                    <View style={styles.tabContainer}>
                        <View style={[styles.tabBox, styles.tabExpenseActive]}>
                            <View style={styles.tabHeader}>
                                <Feather name="trending-down" size={16} color="#A92759" />
                                <Text style={[styles.tabTitle, { color: '#A92759' }]}>Tổng Chi</Text>
                            </View>
                            <Text style={[styles.tabAmount, { color: '#A92759' }]}>{formatShortMoney(totalExpenseVal)}</Text>
                        </View>

                        <View style={[styles.tabBox, styles.tabIncomeActive]}>
                            <View style={styles.tabHeader}>
                                <Feather name="trending-up" size={16} color="#10B981" />
                                <Text style={[styles.tabTitle, { color: '#10B981' }]}>Tổng Thu</Text>
                            </View>
                            <Text style={[styles.tabAmount, { color: '#10B981' }]}>{formatShortMoney(incomeChart.totalAmount)}</Text>
                        </View>
                    </View>

                    {isScreenLoading ? (
                        <ActivityIndicator color="#A92759" size="large" style={{ marginVertical: 50 }} />
                    ) : (
                        <>
                            {/* BIỂU ĐỒ CỘT */}
                            {renderBarChart("Xu hướng Chi tiêu", expenseChart, "#A92759", expenseData?.aiAnalysis)}
                            {renderBarChart("Xu hướng Thu nhập", incomeChart, "#10B981", incomeData?.aiAnalysis)}

                            {/* BIỂU ĐỒ TRÒN CƠ CẤU */}
                            <View style={styles.chartCard}>
                                <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 15, paddingHorizontal: 10 }}>
                                    <Text style={[styles.sectionTitle, { color: colors.text, marginBottom: 0, paddingHorizontal: 0 }]}>
                                        Cơ cấu chi tiêu
                                    </Text>
                                    <TouchableOpacity 
                                        style={{ backgroundColor: isDark ? '#333' : '#F3F4F6', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 12 }}
                                        onPress={() => setIsYearlyView(!isYearlyView)}
                                    >
                                        <Text style={{ color: '#A92759', fontWeight: 'bold', fontSize: 12 }}>
                                            {isYearlyView ? 'Cả năm' : `Tháng ${currentMonth}`}
                                        </Text>
                                    </TouchableOpacity>
                                </View>

                                {/* Thanh chọn Tháng */}
                                {!isYearlyView && (
                                    <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ marginBottom: 15, paddingHorizontal: 10 }}>
                                        {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map((m) => (
                                            <TouchableOpacity 
                                                key={m}
                                                onPress={() => setCurrentMonth(m)}
                                                style={{
                                                    paddingHorizontal: 16, paddingVertical: 8, borderRadius: 20,
                                                    backgroundColor: currentMonth === m ? '#A92759' : (isDark ? '#2C2C2C' : '#F3F4F6'),
                                                    marginRight: 8
                                                }}
                                            >
                                                <Text style={{ color: currentMonth === m ? '#FFF' : colors.text, fontWeight: '600' }}>T{m}</Text>
                                            </TouchableOpacity>
                                        ))}
                                    </ScrollView>
                                )}

                                <View style={styles.pieContainer}>
                                    {pieData.length > 0 ? (
                                        <>
                                            <PieChart 
                                                donut showText textColor="white" innerRadius={70} radius={110} data={pieData} 
                                                centerLabelComponent={() => (
                                                    <View style={{ justifyContent: 'center', alignItems: 'center' }}>
                                                        <Text style={{ fontSize: 22, color: colors.text, fontWeight: 'bold' }}>100%</Text>
                                                        <Text style={{ fontSize: 10, color: '#9CA3AF' }}>Tổng cộng</Text>
                                                    </View>
                                                )} 
                                            />
                                            <View style={styles.legendContainer}>
                                                {analyticsData?.categoryDistribution?.details.map((item, index) => (
                                                    <View key={index} style={styles.legendItem}>
                                                        <View style={{ flexDirection: 'row', alignItems: 'center', flex: 1 }}>
                                                            <View style={[styles.legendDot, { backgroundColor: item.color || '#A92759' }]} />
                                                            <Text style={[styles.legendLabel, { color: colors.text }]} numberOfLines={1}>{item.categoryName}</Text>
                                                        </View>
                                                        <View style={{ alignItems: 'flex-end' }}>
                                                            <Text style={[styles.legendValue, { color: colors.text }]}>{formatShortMoney(item.amount)}</Text>
                                                        </View>
                                                    </View>
                                                ))}
                                            </View>
                                        </>
                                    ) : (
                                        <View style={{ paddingVertical: 30, alignItems: 'center' }}>
                                            <Feather name="pie-chart" size={40} color="#CCC" />
                                            <Text style={[styles.emptyText, { marginTop: 10 }]}>Chưa có dữ liệu phân tích</Text>
                                        </View>
                                    )}
                                </View>
                            </View>
                        </>
                    )}
                    
                    <View style={{ height: 40 }} />
                </View>
            </ScrollView>
        </SafeAreaView>
    );
}

const getStyles = (colors: any, isDark: boolean) => StyleSheet.create({
    safeArea: { flex: 1, backgroundColor: isDark ? '#121212' : '#A92759' },
    container: { flex: 1, backgroundColor: isDark ? '#121212' : '#F9FAFB' },
    headerBackground: { backgroundColor: isDark ? '#1E1E1E' : '#A92759', padding: 20, paddingTop: 10, paddingBottom: 30 },
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
    tabBox: { flex: 1, backgroundColor: colors.surface, padding: 16, borderRadius: 16, borderWidth: 1.5, marginHorizontal: 4, elevation: 2 },
    tabExpenseActive: { borderColor: '#A92759', backgroundColor: isDark ? '#2A1A24' : '#FFF0F5' },
    tabIncomeActive: { borderColor: '#10B981', backgroundColor: isDark ? '#064E3B' : '#ECFDF5' },
    tabHeader: { flexDirection: 'row', alignItems: 'center', marginBottom: 8 },
    tabTitle: { fontSize: 13, color: '#6B7280', marginLeft: 6, fontWeight: '600' },
    tabAmount: { fontSize: 19, fontWeight: 'bold', color: colors.text },
    chartCard: { backgroundColor: colors.surface, borderRadius: 20, paddingVertical: 20, paddingHorizontal: 10, marginBottom: 20, elevation: 2 },
    sectionTitle: { fontSize: 16, fontWeight: 'bold', marginBottom: 10, paddingHorizontal: 10 },
    emptyText: { textAlign: 'center', color: '#9CA3AF' },
    yAxisLabel: { fontSize: 10, color: '#9CA3AF', textAlign: 'right', width: '100%' },
    pieContainer: { alignItems: 'center', marginTop: 10 },
    legendContainer: { width: '100%', marginTop: 24, paddingHorizontal: 10 },
    legendItem: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 },
    legendDot: { width: 12, height: 12, borderRadius: 6, marginRight: 10 },
    legendLabel: { fontSize: 14, fontWeight: '500' },
    legendValue: { fontSize: 14, fontWeight: 'bold' },
    aiCard: { backgroundColor: isDark ? '#1E1E1E' : '#FFF', borderRadius: 20, padding: 20, borderWidth: 1, borderColor: isDark ? '#333' : '#FCE7F3' },
    aiHeader: { flexDirection: 'row', alignItems: 'center', marginBottom: 12 },
    aiIconBox: { backgroundColor: '#FCE7F3', width: 32, height: 32, borderRadius: 16, justifyContent: 'center', alignItems: 'center', marginRight: 12 },
    aiTitle: { fontSize: 15, fontWeight: 'bold' },
    aiText: { fontSize: 14, color: isDark ? '#D1D5DB' : '#4B5563', lineHeight: 22 },
});