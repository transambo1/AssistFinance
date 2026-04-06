import React, { useState } from 'react';
import { View, Text, ScrollView, TouchableOpacity, KeyboardAvoidingView, StyleSheet, SafeAreaView, Image, Switch, Modal, TextInput, ActivityIndicator, Alert, Keyboard, Platform } from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';
import { useRouter } from 'expo-router';
import { useTheme } from '../../src/context/ThemeContext';
import { userService } from '../../src/api/userService';
import { User } from '@/src/types';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

export default function ProfileScreen() {
    const { isDark, toggleTheme } = useTheme();
    const router = useRouter();
    const queryClient = useQueryClient();

    const [isEditModalVisible, setEditModalVisible] = useState(false);
    const [editForm, setEditForm] = useState({
        fullName: '',
        email: '',
        phone: '',
        displayName: ''
    });

    const handleLogout = () => {
        alert('Đăng xuất thành công!');
        router.replace('/(auth)/login');
    }

    const { data: userData, isLoading } = useQuery<User>({
        queryKey: ['userProfile'],
        queryFn: async () => {
            const response = await userService.getProfile() as any;
            return (response?.data || response?.result || response) as User;
        },
    });

    const updateMutation = useMutation({
        mutationFn: (profileData: any) => userService.updateUserProfile(profileData),
        onSuccess: () => {

            queryClient.invalidateQueries({ queryKey: ['userProfile'] });
            Alert.alert("Thành công", "Cập nhật thông tin thành công!");
            setEditModalVisible(false);
        },
        onError: (error: any) => {
            console.error("Lỗi cập nhật:", error);
            Alert.alert("Lỗi", "Không thể cập nhật thông tin. Vui lòng thử lại!");
        }
    });

    const handleOpenEditModal = () => {
        setEditForm({
            fullName: userData?.fullName || '',
            email: userData?.email || '',
            phone: userData?.phone || '',
            displayName: userData?.displayName || ''
        });
        setEditModalVisible(true);
    };

    const handleSaveChanges = () => {
        updateMutation.mutate(editForm);
    };

    const bgColor = isDark ? '#121212' : '#FBF8FF';
    const surfaceColor = isDark ? '#1E1E1E' : '#ffffff';
    const textColor = isDark ? '#ffffff' : '#1b1b21';
    const textDimColor = isDark ? '#A0A0A0' : '#767683';
    const primaryColor = isDark ? '#8c9eff' : '#1a237e';
    const iconBgColor = isDark ? '#2C2C3E' : '#e0e0ff';
    const iconBgColorAlt = isDark ? '#2C2C3E' : '#f5f2fb';
    const dividerColor = isDark ? '#2A2A2A' : '#f5f2fb';

    return (
        <SafeAreaView style={[styles.container, { backgroundColor: bgColor }]}>
            <View style={styles.header}>
                <Text style={[styles.headerTitle, { color: isDark ? '#ffffff' : '#000666' }]}>Hồ sơ</Text>
            </View>

            <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
                {/* Info Card */}
                <View style={[styles.profileCard, { backgroundColor: surfaceColor }]}>
                    <Image source={{ uri: userData?.photoUrl || 'https://i.pravatar.cc/150?img=11' }} style={styles.avatar} />
                    <View style={styles.profileInfo}>
                        <Text style={[styles.name, { color: textColor }]}>{userData?.fullName || userData?.username || 'Đang tải...'}</Text>
                        <Text style={[styles.email, { color: textDimColor }]}>{userData?.email}</Text>
                        <Text style={[styles.email, { color: textDimColor }]}>{userData?.phone}</Text>
                    </View>
                    <TouchableOpacity style={[styles.editBtn, { backgroundColor: iconBgColorAlt }]} onPress={handleOpenEditModal}>
                        <MaterialIcons name="edit" size={20} color={primaryColor} />
                    </TouchableOpacity>
                </View>

                {/* Cài đặt Tài khoản */}
                <Text style={[styles.sectionTitle, { color: textDimColor }]}>Tài khoản</Text>
                <View style={[styles.menuGroup, { backgroundColor: surfaceColor }]}>
                    {/* BẤM VÀO ĐÂY ĐỂ MỞ MODAL */}
                    <TouchableOpacity style={styles.menuItem} onPress={handleOpenEditModal}>
                        <View style={[styles.menuIconBox, { backgroundColor: iconBgColor }]}>
                            <MaterialIcons name="person" size={22} color={primaryColor} />
                        </View>
                        <Text style={[styles.menuText, { color: textColor }]}>Thông tin cá nhân</Text>
                        <MaterialIcons name="chevron-right" size={24} color={textDimColor} />
                    </TouchableOpacity>

                    <View style={[styles.divider, { backgroundColor: dividerColor }]} />

                    <TouchableOpacity style={styles.menuItem} onPress={() => router.push('/personal-salary')} >
                        <View style={[styles.menuIconBox, { backgroundColor: iconBgColor }]}>
                            <MaterialIcons name="account-balance-wallet" size={22} color={primaryColor} />
                        </View>
                        <Text style={[styles.menuText, { color: textColor }]}>Quản lý ví & Tài khoản</Text>
                        <MaterialIcons name="chevron-right" size={24} color={textDimColor} />
                    </TouchableOpacity>
                </View>

                {/* Tùy chỉnh AI & Hệ thống */}
                <Text style={[styles.sectionTitle, { color: textDimColor }]}>Hệ thống & AI</Text>
                <View style={[styles.menuGroup, { backgroundColor: surfaceColor }]}>
                    {/* ... (Giữ nguyên các menu hệ thống của bạn) ... */}
                    <View style={styles.menuItem}>
                        <View style={[styles.menuIconBox, { backgroundColor: iconBgColorAlt }]}>
                            <MaterialIcons name={isDark ? "dark-mode" : "light-mode"} size={22} color={isDark ? '#a0abff' : '#4c56af'} />
                        </View>
                        <Text style={[styles.menuText, { color: textColor }]}>Giao diện {isDark ? "tối" : "sáng"}</Text>
                        <Switch
                            value={isDark}
                            onValueChange={toggleTheme}
                            trackColor={{ false: "#c6c5d4", true: primaryColor }}
                            thumbColor={"#ffffff"}
                        />
                    </View>
                </View>

                <TouchableOpacity style={styles.logoutBtn} onPress={handleLogout}>
                    <MaterialIcons name="logout" size={24} color="#ff635f" />
                    <Text style={styles.logoutText}>Đăng xuất</Text>
                </TouchableOpacity>
            </ScrollView>

            <Modal
                visible={isEditModalVisible}
                animationType="slide"
                transparent={true}
                onRequestClose={() => setEditModalVisible(false)}
            >
                <KeyboardAvoidingView style={styles.modalOverlay}
                    behavior={Platform.OS === 'ios' ? 'padding' : 'height'}

                >
                    <View>
                        <View style={[styles.modalContent, { backgroundColor: surfaceColor }]}>
                            <View style={styles.modalHeader}>
                                <Text style={[styles.modalTitle, { color: textColor }]}>Chỉnh sửa thông tin</Text>
                                <TouchableOpacity onPress={() => setEditModalVisible(false)}>
                                    <MaterialIcons name="close" size={24} color={textDimColor} />
                                </TouchableOpacity>
                            </View>

                            {/* Form Nhập Liệu */}
                            <View style={styles.formGroup}>
                                <Text style={[styles.inputLabel, { color: textDimColor }]}>Họ và tên</Text>
                                <TextInput
                                    style={[styles.input, { backgroundColor: iconBgColorAlt, color: textColor }]}
                                    value={editForm.fullName}
                                    onChangeText={(text) => setEditForm({ ...editForm, fullName: text })}
                                    placeholder="Nhập họ và tên..."
                                    placeholderTextColor={textDimColor}
                                />
                            </View>

                            <View style={styles.formGroup}>
                                <Text style={[styles.inputLabel, { color: textDimColor }]}>Tên cá tính </Text>
                                <TextInput
                                    style={[styles.input, { backgroundColor: iconBgColorAlt, color: textColor }]}
                                    value={editForm.displayName}
                                    onChangeText={(text) => setEditForm({ ...editForm, displayName: text })}
                                    placeholder="Nhập tên cá nhân của mình nào"
                                    placeholderTextColor={textDimColor}
                                />
                            </View>

                            <View style={styles.formGroup}>
                                <Text style={[styles.inputLabel, { color: textDimColor }]}>Email</Text>
                                <TextInput
                                    style={[styles.input, { backgroundColor: iconBgColorAlt, color: textColor }]}
                                    value={editForm.email}
                                    onChangeText={(text) => setEditForm({ ...editForm, email: text })}
                                    placeholder="Nhập email..."
                                    keyboardType="email-address"
                                    placeholderTextColor={textDimColor}
                                />
                            </View>

                            <View style={styles.formGroup}>
                                <Text style={[styles.inputLabel, { color: textDimColor }]}>Số điện thoại</Text>
                                <TextInput
                                    style={[styles.input, { backgroundColor: iconBgColorAlt, color: textColor }]}
                                    value={editForm.phone}
                                    onChangeText={(text) => setEditForm({ ...editForm, phone: text })}
                                    placeholder="Nhập số điện thoại..."
                                    keyboardType="phone-pad"
                                    placeholderTextColor={textDimColor}
                                />
                            </View>

                            <TouchableOpacity
                                style={[styles.saveBtn, { backgroundColor: primaryColor, opacity: updateMutation.isPending ? 0.7 : 1 }]}
                                onPress={handleSaveChanges}
                                disabled={updateMutation.isPending}
                            >
                                {updateMutation.isPending ? (
                                    <ActivityIndicator color="#ffffff" />
                                ) : (
                                    <Text style={styles.saveBtnText}>Lưu thay đổi</Text>
                                )}
                            </TouchableOpacity>
                        </View>
                    </View>
                </KeyboardAvoidingView>
            </Modal>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1 },
    header: { paddingHorizontal: 24, paddingTop: 16, paddingBottom: 16 },
    headerTitle: { fontSize: 28, fontWeight: 'bold' },
    scrollContent: { paddingHorizontal: 24, paddingBottom: 120 },

    profileCard: { flexDirection: 'row', alignItems: 'center', padding: 20, borderRadius: 24, marginBottom: 32, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 8, elevation: 2 },
    avatar: { width: 64, height: 64, borderRadius: 32, marginRight: 16 },
    profileInfo: { flex: 1 },
    name: { fontSize: 20, fontWeight: 'bold', marginBottom: 4 },
    email: { fontSize: 14 },
    editBtn: { width: 40, height: 40, borderRadius: 20, alignItems: 'center', justifyContent: 'center' },

    sectionTitle: { fontSize: 14, fontWeight: 'bold', textTransform: 'uppercase', marginBottom: 12, marginLeft: 8, letterSpacing: 1 },
    menuGroup: { borderRadius: 20, marginBottom: 24, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.03, shadowRadius: 4, elevation: 1 },
    menuItem: { flexDirection: 'row', alignItems: 'center', padding: 16 },
    menuIconBox: { width: 40, height: 40, borderRadius: 12, alignItems: 'center', justifyContent: 'center', marginRight: 16 },
    menuText: { flex: 1, fontSize: 16, fontWeight: '600' },
    divider: { height: 1, marginLeft: 72 },

    logoutBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', backgroundColor: '#ffe5e5', paddingVertical: 16, borderRadius: 20, gap: 8, marginTop: 16 },
    logoutText: { fontSize: 16, fontWeight: 'bold', color: '#ff635f' },

    /* Styles cho Modal */
    modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'flex-end' },
    modalContent: { borderTopLeftRadius: 24, borderTopRightRadius: 24, padding: 24, paddingBottom: 40 },
    modalHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 },
    modalTitle: { fontSize: 20, fontWeight: 'bold' },
    formGroup: { marginBottom: 16 },
    inputLabel: { fontSize: 14, marginBottom: 8, fontWeight: '500' },
    input: { height: 50, borderRadius: 12, paddingHorizontal: 16, fontSize: 16 },
    saveBtn: { height: 56, borderRadius: 16, alignItems: 'center', justifyContent: 'center', marginTop: 24 },
    saveBtnText: { color: '#ffffff', fontSize: 16, fontWeight: 'bold' }
});