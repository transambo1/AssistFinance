import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, TextInput, ScrollView, SafeAreaView, Platform } from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';

export default function AddTransactionScreen() {
    const router = useRouter();
    const [type, setType] = useState<'expense' | 'income'>('expense');
    const [amount, setAmount] = useState('');
    const [note, setNote] = useState('');
    const [category, setCategory] = useState('Ăn uống');

    const categories = [
        { name: 'Ăn uống', icon: 'restaurant', color: '#ff635f' },
        { name: 'Di chuyển', icon: 'directions-car', color: '#1b6d24' },
        { name: 'Mua sắm', icon: 'shopping-cart', color: '#4c56af' },
        { name: 'Nhà cửa', icon: 'home', color: '#1a237e' },
        { name: 'Giải trí', icon: 'confirmation-number', color: '#ff9800' },
        { name: 'Y tế', icon: 'medical-services', color: '#e91e63' },
    ];

    return (
        <SafeAreaView style={styles.container}>
            {/* Header */}
            <View style={styles.header}>
                <TouchableOpacity onPress={() => router.back()} style={styles.closeBtn}>
                    <MaterialIcons name="close" size={28} color="#000666" />
                </TouchableOpacity>
                <Text style={styles.headerTitle}>Giao dịch mới</Text>
                <View style={{ width: 40 }} />
            </View>

            <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
                {/* Toggle Thu nhập / Chi tiêu */}
                <View style={styles.typeSelector}>
                    <TouchableOpacity
                        style={[styles.typeBtn, type === 'expense' && styles.typeBtnActiveExpense]}
                        onPress={() => setType('expense')}
                    >
                        <Text style={[styles.typeText, type === 'expense' && styles.typeTextActive]}>Chi tiêu</Text>
                    </TouchableOpacity>
                    <TouchableOpacity
                        style={[styles.typeBtn, type === 'income' && styles.typeBtnActiveIncome]}
                        onPress={() => setType('income')}
                    >
                        <Text style={[styles.typeText, type === 'income' && styles.typeTextActive]}>Thu nhập</Text>
                    </TouchableOpacity>
                </View>

                {/* Input Số tiền */}
                <View style={styles.inputSection}>
                    <Text style={styles.inputLabel}>Số tiền (VND)</Text>
                    <TextInput
                        style={[styles.amountInput, { color: type === 'expense' ? '#ff635f' : '#1b6d24' }]}
                        placeholder="0"
                        placeholderTextColor="#c6c5d4"
                        keyboardType="numeric"
                        value={amount}
                        onChangeText={setAmount}
                    />
                </View>

                {/* Chọn Danh mục */}
                <Text style={styles.sectionTitle}>Danh mục</Text>
                <View style={styles.categoryGrid}>
                    {categories.map((cat) => (
                        <TouchableOpacity
                            key={cat.name}
                            style={[styles.categoryItem, category === cat.name && { borderColor: cat.color, backgroundColor: cat.color + '10' }]}
                            onPress={() => setCategory(cat.name)}
                        >
                            <View style={[styles.iconCircle, { backgroundColor: cat.color + '20' }]}>
                                <MaterialIcons name={cat.icon as any} size={24} color={cat.color} />
                            </View>
                            <Text style={styles.categoryText}>{cat.name}</Text>
                        </TouchableOpacity>
                    ))}
                </View>

                {/* Ghi chú */}
                <View style={styles.inputSection}>
                    <Text style={styles.inputLabel}>Ghi chú</Text>
                    <TextInput
                        style={styles.noteInput}
                        placeholder="Bạn đã chi cho việc gì?..."
                        placeholderTextColor="#767683"
                        value={note}
                        onChangeText={setNote}
                        multiline
                    />
                </View>

                {/* Nút Lưu */}
                <TouchableOpacity style={styles.saveBtn} onPress={() => router.back()}>
                    <Text style={styles.saveBtnText}>Lưu giao dịchh</Text>
                </TouchableOpacity>
            </ScrollView>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#ffffff' },
    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 20, paddingTop: 10 },
    headerTitle: { fontSize: 18, fontWeight: 'bold', color: '#000666' },
    closeBtn: { padding: 8 },
    scrollContent: { padding: 24, gap: 32 },

    typeSelector: { flexDirection: 'row', backgroundColor: '#f5f2fb', borderRadius: 16, padding: 4 },
    typeBtn: { flex: 1, paddingVertical: 12, alignItems: 'center', borderRadius: 12 },
    typeBtnActiveExpense: { backgroundColor: '#ff635f' },
    typeBtnActiveIncome: { backgroundColor: '#1b6d24' },
    typeText: { fontSize: 15, fontWeight: 'bold', color: '#767683' },
    typeTextActive: { color: '#ffffff' },

    inputSection: {
        gap: 3,
        borderWidth: 0.5,
        borderRadius: 25,
        borderColor: '#858585',
        alignItems: 'flex-start',
        paddingHorizontal: 24,
        paddingVertical: 12,
    },
    inputLabel: { fontSize: 13, fontWeight: 'bold', color: '#767683', textTransform: 'uppercase', borderBottomWidth: 1, borderBottomColor: '#858585' },
    amountInput: { fontSize: 28, fontWeight: 'bold', paddingVertical: 5 },
    noteInput: { backgroundColor: '#f5f2fb', borderRadius: 16, padding: 16, fontSize: 16, minHeight: 100, textAlignVertical: 'top', width: '100%' },

    sectionTitle: { fontSize: 16, fontWeight: 'bold', color: '#000666' },
    categoryGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12, justifyContent: 'space-between', width: '100%', height: 200 },
    categoryItem: { width: '30%', aspectRatio: 1, borderRadius: 20, borderWidth: 1, borderColor: '#f5f2fb', backgroundColor: '#f5f2fb', alignItems: 'center', justifyContent: 'center', gap: 8, height: 20 },
    iconCircle: { width: 40, height: 40, borderRadius: 22, alignItems: 'center', justifyContent: 'center' },
    categoryText: { fontSize: 12, fontWeight: '600', color: '#454652' },

    saveBtn: { backgroundColor: '#1a237e', paddingVertical: 18, borderRadius: 20, alignItems: 'center', shadowColor: '#1a237e', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.3, shadowRadius: 8, elevation: 4 },
    saveBtnText: { color: '#ffffff', fontSize: 16, fontWeight: 'bold' },
});