import React from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet, SafeAreaView } from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';
import { useTheme } from '.././src/context/ThemeContext'; // Import Hook Theme

export default function AnalysisScreen() {
    // 1. Lấy màu từ Context
    const { isDark, colors } = useTheme();

    // 2. Truyền vào hàm tạo Style
    const styles = getStyles(colors, isDark);

    return (
        <SafeAreaView style={styles.container}>
            {/* Header */}
            <View style={styles.header}>
                <Text style={styles.headerTitle}>Phân tích & Dự báo</Text>
                <TouchableOpacity style={styles.headerBtn}>
                    <MaterialIcons name="calendar-today" size={24} color={isDark ? '#ffffff' : '#000666'} />
                </TouchableOpacity>
            </View>

            <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>

                {/* Lời khuyên AI */}
                <View style={styles.aiCard}>
                    <View style={styles.aiHeader}>
                        <MaterialIcons name="auto-awesome" size={20} color={isDark ? '#a0abff' : '#1a237e'} />
                        <Text style={styles.aiTitle}>AI Insight</Text>
                    </View>
                    <Text style={styles.aiText}>
                        Bạn đang có xu hướng chi tiêu nhiều hơn 12% vào cuối tuần. Nếu cắt giảm 1 ly cafe mỗi ngày, bạn sẽ tiết kiệm thêm được 1.200.000đ tháng này!
                    </Text>
                </View>

                {/* Tổng quan tháng (Mock Chart) */}
                <View style={styles.chartCard}>
                    <Text style={styles.chartTitle}>Cơ cấu chi tiêu Tháng 3</Text>
                    <View style={styles.mockChartContainer}>
                        {/* Giả lập biểu đồ tròn bằng các khối màu */}
                        <View style={[styles.chartCircle, { borderColor: '#ff635f', borderTopColor: isDark ? '#a0abff' : '#1a237e', borderRightColor: isDark ? '#a0f399' : '#1b6d24' }]} />

                        {/* Lõi biểu đồ - Màu nền tự chuyển theo thẻ Card */}
                        <View style={styles.chartCenter}>
                            <Text style={styles.chartCenterText}>17.850k</Text>
                            <Text style={styles.chartCenterSub}>Tổng chi</Text>
                        </View>
                    </View>
                </View>

                {/* Danh sách phân bổ chi tiêu */}
                <View style={styles.breakdownSection}>
                    <Text style={styles.sectionTitle}>Phân bổ theo danh mục</Text>

                    {/* Item 1 */}
                    <View style={styles.breakdownItem}>
                        <View style={styles.breakdownHeader}>
                            <View style={styles.breakdownLeft}>
                                <View style={[styles.iconBox, { backgroundColor: isDark ? '#2C2C3E' : '#ffe5e5' }]}>
                                    <MaterialIcons name="restaurant" size={20} color="#ff635f" />
                                </View>
                                <Text style={styles.breakdownName}>Ăn uống</Text>
                            </View>
                            <View style={styles.breakdownRight}>
                                <Text style={styles.breakdownAmount}>8.500.000</Text>
                                <Text style={styles.breakdownPercent}>48%</Text>
                            </View>
                        </View>
                        <View style={styles.progressBarBg}>
                            <View style={[styles.progressBarFill, { width: '48%', backgroundColor: '#ff635f' }]} />
                        </View>
                    </View>

                    {/* Item 2 */}
                    <View style={styles.breakdownItem}>
                        <View style={styles.breakdownHeader}>
                            <View style={styles.breakdownLeft}>
                                <View style={[styles.iconBox, { backgroundColor: isDark ? '#2C2C3E' : '#e0e0ff' }]}>
                                    <MaterialIcons name="home" size={20} color={isDark ? '#a0abff' : '#1a237e'} />
                                </View>
                                <Text style={styles.breakdownName}>Sinh hoạt</Text>
                            </View>
                            <View style={styles.breakdownRight}>
                                <Text style={styles.breakdownAmount}>5.000.000</Text>
                                <Text style={styles.breakdownPercent}>28%</Text>
                            </View>
                        </View>
                        <View style={styles.progressBarBg}>
                            <View style={[styles.progressBarFill, { width: '28%', backgroundColor: isDark ? '#a0abff' : '#1a237e' }]} />
                        </View>
                    </View>

                    {/* Item 3 */}
                    <View style={styles.breakdownItem}>
                        <View style={styles.breakdownHeader}>
                            <View style={styles.breakdownLeft}>
                                <View style={[styles.iconBox, { backgroundColor: isDark ? '#2C2C3E' : '#e5f6e5' }]}>
                                    <MaterialIcons name="directions-car" size={20} color={isDark ? '#a0f399' : '#1b6d24'} />
                                </View>
                                <Text style={styles.breakdownName}>Di chuyển</Text>
                            </View>
                            <View style={styles.breakdownRight}>
                                <Text style={styles.breakdownAmount}>1.500.000</Text>
                                <Text style={styles.breakdownPercent}>8%</Text>
                            </View>
                        </View>
                        <View style={styles.progressBarBg}>
                            <View style={[styles.progressBarFill, { width: '8%', backgroundColor: isDark ? '#a0f399' : '#1b6d24' }]} />
                        </View>
                    </View>

                </View>
            </ScrollView>
        </SafeAreaView>
    );
}

// 3. Hàm tạo Style: Nhận `colors` và thay thế toàn bộ mã cứng
const getStyles = (colors: any, isDark: boolean) => StyleSheet.create({
    container: { flex: 1, backgroundColor: colors.background },

    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 24, paddingTop: 16, paddingBottom: 16 },
    headerTitle: { fontSize: 24, fontWeight: 'bold', color: isDark ? '#ffffff' : '#000666' },
    headerBtn: { padding: 8, backgroundColor: colors.surface, borderRadius: 12, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2 },

    scrollContent: { paddingHorizontal: 24, paddingBottom: 130 },

    aiCard: { backgroundColor: isDark ? '#2C2C3E' : '#e0e0ff', padding: 20, borderRadius: 20, marginBottom: 24 },
    aiHeader: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 8 },
    aiTitle: { fontSize: 16, fontWeight: 'bold', color: isDark ? '#ffffff' : '#1a237e' },
    aiText: { fontSize: 14, color: colors.text, lineHeight: 22 },

    chartCard: { backgroundColor: colors.surface, padding: 24, borderRadius: 24, alignItems: 'center', marginBottom: 24, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.03, shadowRadius: 8, elevation: 2 },
    chartTitle: { fontSize: 16, fontWeight: 'bold', color: colors.text, marginBottom: 20, alignSelf: 'flex-start' },
    mockChartContainer: { width: 160, height: 160, justifyContent: 'center', alignItems: 'center' },
    chartCircle: { position: 'absolute', width: 160, height: 160, borderRadius: 80, borderWidth: 24 },

    // Lõi biểu đồ tròn lấy màu nền của Card để che phần ở giữa
    chartCenter: { alignItems: 'center', backgroundColor: colors.surface, width: 110, height: 110, borderRadius: 55, justifyContent: 'center', shadowColor: '#000', shadowOffset: { width: 0, height: 0 }, shadowOpacity: 0.1, shadowRadius: 10, elevation: 4 },
    chartCenterText: { fontSize: 20, fontWeight: '900', color: colors.text },
    chartCenterSub: { fontSize: 12, color: colors.textDim },

    breakdownSection: { gap: 16 },
    sectionTitle: { fontSize: 18, fontWeight: 'bold', color: isDark ? '#ffffff' : '#000666', marginBottom: 8 },
    breakdownItem: { backgroundColor: colors.surface, padding: 16, borderRadius: 16, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.03, shadowRadius: 4, elevation: 1 },
    breakdownHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 },
    breakdownLeft: { flexDirection: 'row', alignItems: 'center', gap: 12 },
    iconBox: { width: 40, height: 40, borderRadius: 20, alignItems: 'center', justifyContent: 'center' },
    breakdownName: { fontSize: 15, fontWeight: 'bold', color: colors.text },
    breakdownRight: { alignItems: 'flex-end' },
    breakdownAmount: { fontSize: 15, fontWeight: 'bold', color: colors.text },
    breakdownPercent: { fontSize: 12, color: colors.textDim },

    progressBarBg: { height: 8, backgroundColor: isDark ? '#333333' : '#f5f2fb', borderRadius: 4, overflow: 'hidden' },
    progressBarFill: { height: '100%', borderRadius: 4 },
});