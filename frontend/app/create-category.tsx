import React, { useState } from 'react';
import {
    View,
    Text,
    StyleSheet,
    TextInput,
    TouchableOpacity,
    ScrollView,
    SafeAreaView,
    Alert,
    ActivityIndicator,
    Platform,
    KeyboardAvoidingView
} from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { categoryService } from '@/src/api/categoryService';

// DANH SÁCH MÀU SẮC GỢI Ý (Chuẩn UI Tài chính)
const PRESET_COLORS = [
    '#EF4444', '#F59E0B', '#10B981', '#3B82F6', '#6366F1',
    '#8B5CF6', '#EC4899', '#6B7280', '#06B6D4', '#F43F5E'
];

// DANH SÁCH ICON GỢI Ý
const PRESET_ICONS = [
    'restaurant', 'shopping-cart', 'directions-car', 'home',
    'medical-services', 'fitness-center', 'payments', 'local-atm',
    'school', 'flight', 'movie', 'fastfood', 'checkroom', 'work'
];

export default function CreateCategoryScreen() {
    const router = useRouter();
    const queryClient = useQueryClient();

    // --- STATES ---
    const [name, setName] = useState('');
    const [type, setType] = useState<'EXPENSE' | 'INCOME'>('EXPENSE');
    const [selectedColor, setSelectedColor] = useState(PRESET_COLORS[3]); // Mặc định Blue
    const [selectedIcon, setSelectedIcon] = useState(PRESET_ICONS[0]);

    // --- MUTATION ---
    const createMutation = useMutation({
        mutationFn: (data: any) => categoryService.create(data),
        onSuccess: () => {
            Alert.alert('Thành công', 'Đã tạo danh mục mới!');
            queryClient.invalidateQueries({ queryKey: ['categories'] });
            router.back();
        },
        onError: (error: any) => {
            Alert.alert('Lỗi', error?.message || 'Không thể tạo danh mục.');
        }
    });

    const handleSave = () => {
        if (!name.trim()) {
            Alert.alert('Lỗi', 'Vui lòng nhập tên danh mục.');
            return;
        }

        const payload = {
            name: name.trim(),
            type: type,
            icon: selectedIcon,
            color: selectedColor
        };

        createMutation.mutate(payload);
    };

    return (
        <SafeAreaView style={styles.container}>
            {/* Header */}
            <View style={styles.header}>
                <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
                    <MaterialIcons name="arrow-back" size={26} color="#1a237e" />
                </TouchableOpacity>
                <Text style={styles.headerTitle}>Tạo danh mục mới</Text>
                <View style={{ width: 40 }} />
            </View>

            <KeyboardAvoidingView
                behavior={Platform.OS === 'ios' ? 'padding' : undefined}
                style={{ flex: 1 }}
            >
                <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>

                    {/* 1. Preview Icon & Name */}
                    <View style={styles.previewContainer}>
                        <View style={[styles.previewCircle, { backgroundColor: selectedColor + '20' }]}>
                            <MaterialIcons name={selectedIcon as any} size={40} color={selectedColor} />
                        </View>
                        <TextInput
                            style={styles.nameInput}
                            placeholder="Tên danh mục..."
                            placeholderTextColor="#9E9E9E"
                            value={name}
                            onChangeText={setName}
                            autoFocus
                        />
                    </View>

                    {/* 2. Loại danh mục */}
                    <View style={styles.section}>
                        <Text style={styles.sectionLabel}>LOẠI DANH MỤC</Text>
                        <View style={styles.typeSelector}>
                            <TouchableOpacity
                                style={[styles.typeBtn, type === 'EXPENSE' && styles.typeBtnActiveExpense]}
                                onPress={() => setType('EXPENSE')}
                            >
                                <Text style={[styles.typeText, type === 'EXPENSE' && styles.typeTextActive]}>Chi tiêu</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={[styles.typeBtn, type === 'INCOME' && styles.typeBtnActiveIncome]}
                                onPress={() => setType('INCOME')}
                            >
                                <Text style={[styles.typeText, type === 'INCOME' && styles.typeTextActive]}>Thu nhập</Text>
                            </TouchableOpacity>
                        </View>
                    </View>

                    {/* 3. Chọn Màu sắc */}
                    <View style={styles.section}>
                        <Text style={styles.sectionLabel}>MÀU SẮC</Text>
                        <View style={styles.grid}>
                            {PRESET_COLORS.map((color) => (
                                <TouchableOpacity
                                    key={color}
                                    style={[styles.colorItem, { backgroundColor: color }, selectedColor === color && styles.colorItemSelected]}
                                    onPress={() => setSelectedColor(color)}
                                >
                                    {selectedColor === color && <MaterialIcons name="check" size={16} color="#fff" />}
                                </TouchableOpacity>
                            ))}
                        </View>
                    </View>

                    {/* 4. Chọn Biểu tượng */}
                    <View style={styles.section}>
                        <Text style={styles.sectionLabel}>BIỂU TƯỢNG</Text>
                        <View style={styles.grid}>
                            {PRESET_ICONS.map((icon) => (
                                <TouchableOpacity
                                    key={icon}
                                    style={[
                                        styles.iconItem,
                                        selectedIcon === icon && { borderColor: selectedColor, backgroundColor: selectedColor + '10' }
                                    ]}
                                    onPress={() => setSelectedIcon(icon)}
                                >
                                    <MaterialIcons name={icon as any} size={24} color={selectedIcon === icon ? selectedColor : '#767683'} />
                                </TouchableOpacity>
                            ))}
                        </View>
                    </View>

                    {/* Button Lưu */}
                    <TouchableOpacity
                        style={[styles.saveBtn, { backgroundColor: selectedColor }]}
                        onPress={handleSave}
                        disabled={createMutation.isPending}
                    >
                        {createMutation.isPending ? (
                            <ActivityIndicator color="#fff" />
                        ) : (
                            <Text style={styles.saveBtnText}>Lưu danh mục</Text>
                        )}
                    </TouchableOpacity>

                </ScrollView>
            </KeyboardAvoidingView>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#F8F9FA' },
    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 20, paddingVertical: 12, backgroundColor: '#fff' },
    headerTitle: { fontSize: 18, fontWeight: 'bold', color: '#1a237e' },
    backBtn: { padding: 4 },
    scrollContent: { padding: 24, gap: 28 },

    previewContainer: { alignItems: 'center', gap: 16, marginBottom: 8 },
    previewCircle: { width: 80, height: 80, borderRadius: 40, alignItems: 'center', justifyContent: 'center' },
    nameInput: { fontSize: 24, fontWeight: 'bold', color: '#1a237e', textAlign: 'center', width: '100%', padding: 0 },

    section: { gap: 12 },
    sectionLabel: { fontSize: 11, fontWeight: '700', color: '#9E9E9E', letterSpacing: 1 },

    typeSelector: { flexDirection: 'row', backgroundColor: '#E8EAF6', borderRadius: 12, padding: 4 },
    typeBtn: { flex: 1, paddingVertical: 10, alignItems: 'center', borderRadius: 10 },
    typeBtnActiveExpense: { backgroundColor: '#EF4444' },
    typeBtnActiveIncome: { backgroundColor: '#10B981' },
    typeText: { fontSize: 14, fontWeight: '700', color: '#767683' },
    typeTextActive: { color: '#ffffff' },

    grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
    colorItem: { width: 44, height: 44, borderRadius: 22, alignItems: 'center', justifyContent: 'center' },
    colorItemSelected: { borderWidth: 3, borderColor: '#fff', elevation: 4, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.2, shadowRadius: 4 },

    iconItem: { width: '22%', height: '22%', aspectRatio: 1, borderRadius: 50, backgroundColor: '#fff', alignItems: 'center', justifyContent: 'center', borderWidth: 1, borderColor: '#E8EAF6' },

    saveBtn: { paddingVertical: 18, borderRadius: 24, alignItems: 'center', shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.2, shadowRadius: 8, elevation: 4, marginTop: 10 },
    saveBtnText: { color: '#ffffff', fontSize: 16, fontWeight: 'bold' },
});