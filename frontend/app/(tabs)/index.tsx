import React from 'react';
import { View, Text, ScrollView, TouchableOpacity, Image, StyleSheet, SafeAreaView } from 'react-native';
import { USER_DATA, MOCK_TRANSACTIONS } from '../../src/mock/data'; // Import data
import { MaterialIcons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
export default function DashboardScreen() {
  const router = useRouter();
  const recentTransactions = MOCK_TRANSACTIONS.slice(0, 3);
  return (

    <SafeAreaView style={styles.container}>
      {/* TopAppBar */}
      <View style={styles.header}>
        <View style={styles.headerLeft}>
          <Image
            source={{ uri: USER_DATA.avatar }}
            style={styles.avatar}
          />
          <Text style={styles.headerTitle}>{USER_DATA.name}</Text>
        </View>
        <TouchableOpacity>
          <MaterialIcons name="notifications" size={28} color="#000666" />
        </TouchableOpacity>
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>

        {/* Total Balance Section */}
        <View style={styles.section}>
          <Text style={styles.sectionLabel}>TỔNG SỐ DƯ KHẢ DỤNG</Text>
          <View style={styles.balanceCard}>
            <MaterialIcons name="account-balance-wallet" size={100} color="rgba(255,255,255,0.1)" style={styles.balanceIconBg} />
            <Text style={styles.balanceAmount}>{USER_DATA.totalBalance.toLocaleString()} </Text>
            <Text style={styles.balanceCurrency}>VND</Text>
            <View style={styles.trendBadge}>
              <MaterialIcons name="trending-up" size={16} color="#a3f69c" />
              <Text style={styles.trendText}>+12.5% so với tháng trước</Text>
            </View>
          </View>
        </View>

        {/* Bento Grid: Income & Expenses Summary */}
        <View style={styles.bentoGrid}>
          <View style={styles.bentoCard}>
            <View style={[styles.iconBox, { backgroundColor: 'rgba(160, 243, 153, 0.3)' }]}>
              <MaterialIcons name="arrow-downward" size={24} color="#1b6d24" />
            </View>
            <Text style={styles.bentoLabel}>Thu nhập</Text>
            <Text style={styles.bentoValue}>42.850.000</Text>
          </View>
          <View style={styles.bentoCard}>
            <View style={[styles.iconBox, { backgroundColor: 'rgba(103, 0, 10, 0.1)' }]}>
              <MaterialIcons name="arrow-upward" size={24} color="#ff635f" />
            </View>
            <Text style={styles.bentoLabel}>Chi tiêu</Text>
            <Text style={styles.bentoValue}>17.850.000</Text>
          </View>
        </View>

        {/* AI Insight Card */}
        <View style={styles.aiCard}>
          <View style={styles.aiIconBox}>
            <MaterialIcons name="smart-toy" size={28} color="#ffffff" />
          </View>
          <View style={styles.aiTextContainer}>
            <Text style={styles.aiTitle}>Phân tích AI từ Aeon</Text>
            <Text style={styles.aiDesc}>Bạn đã chi tiêu ít hơn 15% cho mục Ăn uống so với tuần trước. Tiếp tục duy trì nhé!</Text>
          </View>
        </View>

        {/* Recent Transactions */}

        {/* Phần tiêu đề của mục giao dịch gần đây */}
        <View style={styles.sectionHeaderRow}>
          <Text style={styles.sectionTitle}>Giao dịch gần đây</Text>
          <TouchableOpacity onPress={() => router.push('/history')}>
            <Text style={styles.seeAllText}>Tất cả</Text>
          </TouchableOpacity>
        </View>

        {/* Bắt đầu Map dữ liệu ra từng hàng */}
        {recentTransactions.map((item) => (
          <TouchableOpacity key={item.id} style={styles.transactionItem} onPress={() => router.push('/history')}>

            <View style={styles.transactionLeft}>
              {/* Icon động dựa trên dữ liệu */}
              <View style={[styles.transactionIconBox, { backgroundColor: item.color + '20' }]}>
                <MaterialIcons name={item.icon as any} size={24} color={item.color} />
              </View>
              <View>
                <Text style={styles.transactionName}>{item.name}</Text>
                <Text style={styles.transactionDate}>{item.date}</Text>
              </View>
            </View>

            {/* Số tiền động, màu sắc thay đổi theo loại Thu/Chi */}
            <Text style={[
              styles.transactionAmount,
              { color: item.type === 'expense' ? '#ff635f' : '#1b6d24' }
            ]}>
              {item.type === 'expense' ? '-' : '+'}{item.amount.toLocaleString()}
            </Text>
          </TouchableOpacity>
        ))}

      </ScrollView>

      {/* Floating Action Button */}
      <TouchableOpacity style={styles.fab} onPress={() => router.push('/add-transaction')}>
        <MaterialIcons name="add" size={32} color="#ffffff" />
      </TouchableOpacity>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#FBF8FF' },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 24, paddingTop: 16, paddingBottom: 16 },
  headerLeft: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  avatar: { width: 40, height: 40, borderRadius: 20 },
  headerTitle: { fontSize: 20, fontWeight: 'bold', color: '#000666' },
  scrollContent: { paddingHorizontal: 24, paddingBottom: 120, paddingTop: 8, gap: 24 },
  section: { gap: 12 },
  sectionLabel: { fontSize: 11, fontWeight: '600', color: '#767683', letterSpacing: 1, textTransform: 'uppercase' },
  balanceCard: { backgroundColor: '#1a237e', padding: 24, borderRadius: 28, overflow: 'hidden', position: 'relative' },
  balanceIconBg: { position: 'absolute', top: 16, right: 16 },
  balanceAmount: { fontSize: 44, fontWeight: '900', color: '#ffffff', marginBottom: 4 },
  balanceCurrency: { fontSize: 18, color: 'rgba(255,255,255,0.8)', fontWeight: '500' },
  trendBadge: { flexDirection: 'row', alignItems: 'center', backgroundColor: 'rgba(255,255,255,0.15)', paddingHorizontal: 12, paddingVertical: 8, borderRadius: 20, alignSelf: 'flex-start', marginTop: 24, gap: 6 },
  trendText: { color: '#ffffff', fontSize: 13, fontWeight: '600' },
  bentoGrid: { flexDirection: 'row', gap: 16 },
  bentoCard: { flex: 1, backgroundColor: '#ffffff', padding: 20, borderRadius: 24, gap: 8, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 8, elevation: 2 },
  iconBox: { width: 40, height: 40, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
  bentoLabel: { color: '#454652', fontSize: 13, fontWeight: '500' },
  bentoValue: { fontSize: 20, fontWeight: 'bold', color: '#1b1b21' },
  aiCard: { flexDirection: 'row', backgroundColor: '#f5f2fb', padding: 20, borderRadius: 24, borderWidth: 1, borderColor: '#e4e1ea', alignItems: 'center', gap: 16 },
  aiIconBox: { width: 48, height: 48, borderRadius: 24, backgroundColor: '#1a237e', alignItems: 'center', justifyContent: 'center' },
  aiTextContainer: { flex: 1 },
  aiTitle: { fontSize: 15, fontWeight: 'bold', color: '#000666', marginBottom: 4 },
  aiDesc: { fontSize: 13, color: '#454652', lineHeight: 20 },
  sectionHeaderRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  sectionTitle: { fontSize: 20, fontWeight: 'bold', color: '#000666' },
  seeAllText: { fontSize: 14, fontWeight: '600', color: '#000666' },
  transactionItem: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', backgroundColor: '#ffffff', padding: 16, borderRadius: 16, marginBottom: 8, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.03, shadowRadius: 4, elevation: 1 },
  transactionLeft: { flexDirection: 'row', alignItems: 'center', gap: 16 },
  transactionIconBox: { width: 48, height: 48, borderRadius: 24, alignItems: 'center', justifyContent: 'center' },
  transactionName: { fontSize: 15, fontWeight: 'bold', color: '#1b1b21', marginBottom: 4 },
  transactionDate: { fontSize: 12, color: '#767683' },
  transactionAmount: { fontSize: 16, fontWeight: 'bold' },
  fab: { position: 'absolute', right: 24, bottom: 100, width: 56, height: 56, borderRadius: 28, backgroundColor: '#000666', alignItems: 'center', justifyContent: 'center', shadowColor: '#000666', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.3, shadowRadius: 8, elevation: 6 },
});