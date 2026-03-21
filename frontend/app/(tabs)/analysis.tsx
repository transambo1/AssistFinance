import React from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet, SafeAreaView } from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';

export default function AnalysisScreen() {
    return (
        <SafeAreaView style={styles.container}>
            {/* Header */}
            <View style={styles.header}>
                <Text style={styles.headerTitle}>Phân tích & Dự báo</Text>
                <TouchableOpacity style={styles.headerBtn}>
                    <MaterialIcons name="calendar-today" size={24} color="#000666" />
                </TouchableOpacity>
            </View>

            <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>

                {/* Lời khuyên AI */}
                <View style={styles.aiCard}>
                    <View style={styles.aiHeader}>
                        <MaterialIcons name="auto-awesome" size={20} color="#1a237e" />
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
                        <View style={[styles.chartCircle, { borderColor: '#ff635f', borderTopColor: '#1a237e', borderRightColor: '#a0f399' }]} />
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
                                <View style={[styles.iconBox, { backgroundColor: '#ffe5e5' }]}>
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
                                <View style={[styles.iconBox, { backgroundColor: '#e0e0ff' }]}>
                                    <MaterialIcons name="home" size={20} color="#1a237e" />
                                </View>
                                <Text style={styles.breakdownName}>Sinh hoạt</Text>
                            </View>
                            <View style={styles.breakdownRight}>
                                <Text style={styles.breakdownAmount}>5.000.000</Text>
                                <Text style={styles.breakdownPercent}>28%</Text>
                            </View>
                        </View>
                        <View style={styles.progressBarBg}>
                            <View style={[styles.progressBarFill, { width: '28%', backgroundColor: '#1a237e' }]} />
                        </View>
                    </View>

                    {/* Item 3 */}
                    <View style={styles.breakdownItem}>
                        <View style={styles.breakdownHeader}>
                            <View style={styles.breakdownLeft}>
                                <View style={[styles.iconBox, { backgroundColor: '#e5f6e5' }]}>
                                    <MaterialIcons name="directions-car" size={20} color="#1b6d24" />
                                </View>
                                <Text style={styles.breakdownName}>Di chuyển</Text>
                            </View>
                            <View style={styles.breakdownRight}>
                                <Text style={styles.breakdownAmount}>1.500.000</Text>
                                <Text style={styles.breakdownPercent}>8%</Text>
                            </View>
                        </View>
                        <View style={styles.progressBarBg}>
                            <View style={[styles.progressBarFill, { width: '8%', backgroundColor: '#1b6d24' }]} />
                        </View>
                    </View>

                </View>
            </ScrollView>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#FBF8FF' },
    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 24, paddingTop: 16, paddingBottom: 16 },
    headerTitle: { fontSize: 24, fontWeight: 'bold', color: '#000666' },
    headerBtn: { padding: 8, backgroundColor: '#ffffff', borderRadius: 12, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2 },
    scrollContent: { paddingHorizontal: 24, paddingBottom: 130 },

    aiCard: { backgroundColor: '#e0e0ff', padding: 20, borderRadius: 20, marginBottom: 24 },
    aiHeader: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 8 },
    aiTitle: { fontSize: 16, fontWeight: 'bold', color: '#1a237e' },
    aiText: { fontSize: 14, color: '#000666', lineHeight: 22 },

    chartCard: { backgroundColor: '#ffffff', padding: 24, borderRadius: 24, alignItems: 'center', marginBottom: 24, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.03, shadowRadius: 8, elevation: 2 },
    chartTitle: { fontSize: 16, fontWeight: 'bold', color: '#1b1b21', marginBottom: 20, alignSelf: 'flex-start' },
    mockChartContainer: { width: 160, height: 160, justifyContent: 'center', alignItems: 'center' },
    chartCircle: { position: 'absolute', width: 160, height: 160, borderRadius: 80, borderWidth: 24 },
    chartCenter: { alignItems: 'center', backgroundColor: '#ffffff', width: 110, height: 110, borderRadius: 55, justifyContent: 'center', shadowColor: '#000', shadowOffset: { width: 0, height: 0 }, shadowOpacity: 0.1, shadowRadius: 10, elevation: 4 },
    chartCenterText: { fontSize: 20, fontWeight: '900', color: '#1b1b21' },
    chartCenterSub: { fontSize: 12, color: '#767683' },

    breakdownSection: { gap: 16 },
    sectionTitle: { fontSize: 18, fontWeight: 'bold', color: '#000666', marginBottom: 8 },
    breakdownItem: { backgroundColor: '#ffffff', padding: 16, borderRadius: 16, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.03, shadowRadius: 4, elevation: 1 },
    breakdownHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 },
    breakdownLeft: { flexDirection: 'row', alignItems: 'center', gap: 12 },
    iconBox: { width: 40, height: 40, borderRadius: 20, alignItems: 'center', justifyContent: 'center' },
    breakdownName: { fontSize: 15, fontWeight: 'bold', color: '#1b1b21' },
    breakdownRight: { alignItems: 'flex-end' },
    breakdownAmount: { fontSize: 15, fontWeight: 'bold', color: '#1b1b21' },
    breakdownPercent: { fontSize: 12, color: '#767683' },
    progressBarBg: { height: 8, backgroundColor: '#f5f2fb', borderRadius: 4, overflow: 'hidden' },
    progressBarFill: { height: '100%', borderRadius: 4 },
});