import React from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet, SafeAreaView, Image } from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';

export default function ProfileScreen() {
    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <Text style={styles.headerTitle}>Hồ sơ</Text>
            </View>

            <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>

                {/* Info Card */}
                <View style={styles.profileCard}>
                    <Image source={{ uri: 'https://i.pravatar.cc/150?img=11' }} style={styles.avatar} />
                    <View style={styles.profileInfo}>
                        <Text style={styles.name}>Thành no.1</Text>
                        <Text style={styles.email}>thanh.no1@gmail.com</Text>
                    </View>
                    <TouchableOpacity style={styles.editBtn}>
                        <MaterialIcons name="edit" size={20} color="#1a237e" />
                    </TouchableOpacity>
                </View>

                {/* Cài đặt Tài khoản */}
                <Text style={styles.sectionTitle}>Tài khoản</Text>
                <View style={styles.menuGroup}>
                    <TouchableOpacity style={styles.menuItem}>
                        <View style={[styles.menuIconBox, { backgroundColor: '#e0e0ff' }]}>
                            <MaterialIcons name="person" size={22} color="#1a237e" />
                        </View>
                        <Text style={styles.menuText}>Thông tin cá nhân</Text>
                        <MaterialIcons name="chevron-right" size={24} color="#c6c5d4" />
                    </TouchableOpacity>
                    <View style={styles.divider} />
                    <TouchableOpacity style={styles.menuItem}>
                        <View style={[styles.menuIconBox, { backgroundColor: '#e0e0ff' }]}>
                            <MaterialIcons name="account-balance-wallet" size={22} color="#1a237e" />
                        </View>
                        <Text style={styles.menuText}>Quản lý ví & Tài khoản</Text>
                        <MaterialIcons name="chevron-right" size={24} color="#c6c5d4" />
                    </TouchableOpacity>
                </View>

                {/* Tùy chỉnh AI & Hệ thống */}
                <Text style={styles.sectionTitle}>Hệ thống & AI</Text>
                <View style={styles.menuGroup}>
                    <TouchableOpacity style={styles.menuItem}>
                        <View style={[styles.menuIconBox, { backgroundColor: '#f5f2fb' }]}>
                            <MaterialIcons name="smart-toy" size={22} color="#4c56af" />
                        </View>
                        <Text style={styles.menuText}>Cài đặt Trợ lý AI</Text>
                        <MaterialIcons name="chevron-right" size={24} color="#c6c5d4" />
                    </TouchableOpacity>
                    <View style={styles.divider} />
                    <TouchableOpacity style={styles.menuItem}>
                        <View style={[styles.menuIconBox, { backgroundColor: '#f5f2fb' }]}>
                            <MaterialIcons name="notifications" size={22} color="#4c56af" />
                        </View>
                        <Text style={styles.menuText}>Thông báoo</Text>
                        <MaterialIcons name="chevron-right" size={24} color="#c6c5d4" />
                    </TouchableOpacity>
                    <View style={styles.divider} />
                    <TouchableOpacity style={styles.menuItem}>
                        <View style={[styles.menuIconBox, { backgroundColor: '#f5f2fb' }]}>
                            <MaterialIcons name="dark-mode" size={22} color="#4c56af" />
                        </View>
                        <Text style={styles.menuText}>Giao diện sáng/tối</Text>
                        <MaterialIcons name="chevron-right" size={24} color="#c6c5d4" />
                    </TouchableOpacity>
                </View>

                {/* Nút Đăng xuất */}
                <TouchableOpacity style={styles.logoutBtn}>
                    <MaterialIcons name="logout" size={24} color="#ff635f" />
                    <Text style={styles.logoutText}>Đăng xuất</Text>
                </TouchableOpacity>

            </ScrollView>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#FBF8FF' },
    header: { paddingHorizontal: 24, paddingTop: 16, paddingBottom: 16 },
    headerTitle: { fontSize: 28, fontWeight: 'bold', color: '#000666' },
    scrollContent: { paddingHorizontal: 24, paddingBottom: 120 },

    profileCard: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#ffffff', padding: 20, borderRadius: 24, marginBottom: 32, shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 8, elevation: 2 },
    avatar: { width: 64, height: 64, borderRadius: 32, marginRight: 16 },
    profileInfo: { flex: 1 },
    name: { fontSize: 20, fontWeight: 'bold', color: '#1b1b21', marginBottom: 4 },
    email: { fontSize: 14, color: '#767683' },
    editBtn: { width: 40, height: 40, borderRadius: 20, backgroundColor: '#f5f2fb', alignItems: 'center', justifyContent: 'center' },

    sectionTitle: { fontSize: 14, fontWeight: 'bold', color: '#767683', textTransform: 'uppercase', marginBottom: 12, marginLeft: 8, letterSpacing: 1 },
    menuGroup: { backgroundColor: '#ffffff', borderRadius: 20, marginBottom: 24, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.03, shadowRadius: 4, elevation: 1 },
    menuItem: { flexDirection: 'row', alignItems: 'center', padding: 16 },
    menuIconBox: { width: 40, height: 40, borderRadius: 12, alignItems: 'center', justifyContent: 'center', marginRight: 16 },
    menuText: { flex: 1, fontSize: 16, fontWeight: '600', color: '#1b1b21' },
    divider: { height: 1, backgroundColor: '#f5f2fb', marginLeft: 72 },

    logoutBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'center', backgroundColor: '#ffe5e5', paddingVertical: 16, borderRadius: 20, gap: 8, marginTop: 16 },
    logoutText: { fontSize: 16, fontWeight: 'bold', color: '#ff635f' },
});