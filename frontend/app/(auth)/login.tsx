import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, SafeAreaView, StyleSheet, KeyboardAvoidingView, Platform, ActivityIndicator } from 'react-native';
import { useRouter } from 'expo-router';
import MaterialIcons from '@expo/vector-icons/build/MaterialIcons';
import { userService } from '../../src/api/userService';
import { storage } from '../../src/utils/storage';
import { Stack } from 'expo-router';

export default function LoginScreen() {
    const router = useRouter();

    // Đổi state từ email -> username để khớp với Backend
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');

    // Thêm state để hiển thị Loading
    const [isLoading, setIsLoading] = useState(false);

    const handleLogin = async () => {
        if (!username.trim() || !password.trim()) {
            alert('Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.');
            return;
        }

        try {
            setIsLoading(true);

            const payload = {
                username: username.trim(),
                password: password
            };

            // 2. Gọi API đăng nhập
            // 2. Gọi API đăng nhập
            const response = await userService.login(payload) as any;

            // BẮT TẬN TAY RESPONSE XEM NÓ CÓ GÌ TRONG NÀY:
            console.log("👉 Dữ liệu BE trả về nguyên cục:", JSON.stringify(response, null, 2));

            // Thử lấy token theo cấu trúc thực tế (Bạn hãy kiểm tra log để thay đổi chữ 'result' hoặc 'data' cho đúng nhé)
            const actualToken = response?.token || response?.result?.token || response?.data?.token;

            if (actualToken) {
                console.log("👉 Chuẩn bị lưu token:", actualToken);
                await storage.saveToken(actualToken);
                router.replace('/(tabs)'); // Điều hướng sau khi đăng nhập thành công
            } else {
                console.log("❌ CẢNH BÁO: Không tìm thấy token trong response!");
            }

        } catch (error: any) {
            // Bắt lỗi từ Backend trả về (Ví dụ: Sai mật khẩu)
            console.error('Lỗi đăng nhập:', error);
            alert(error.message || 'Tên đăng nhập hoặc mật khẩu không đúng!');
        } finally {
            setIsLoading(false);
        }
    }

    return (

        <SafeAreaView style={styles.container}>

            <KeyboardAvoidingView
                style={styles.container}
                behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
            >
                <View style={styles.content}>
                    <View style={styles.header}>
                        <View style={styles.iconCircle}>
                            <MaterialIcons name="account-balance-wallet" size={48} color="#1a237e" />
                        </View>
                        <Text style={styles.title}>APTK</Text>
                        <Text style={styles.subtitle}>Đăng nhập để quản lý tài chính</Text>
                    </View>

                    <View style={styles.form}>
                        {/* Đổi ô nhập Email thành Username */}
                        <View style={styles.inputGroup}>
                            <MaterialIcons name="person" size={20} color="#767683" style={styles.inputIcon} />
                            <TextInput
                                style={styles.input}
                                placeholder="Tên đăng nhập"
                                placeholderTextColor="#c6c5d4"
                                value={username}
                                onChangeText={setUsername}
                                autoCapitalize="none"
                            />
                        </View>

                        <View style={styles.inputGroup}>
                            <MaterialIcons name="lock" size={20} color="#767683" style={styles.inputIcon} />
                            <TextInput
                                style={styles.input}
                                placeholder="Mật khẩu"
                                placeholderTextColor="#c6c5d4"
                                value={password}
                                onChangeText={setPassword}
                                secureTextEntry
                            />
                        </View>

                        {/* Cập nhật nút Đăng nhập hiển thị Loading */}
                        <TouchableOpacity
                            style={[styles.loginBtn, isLoading && { opacity: 0.7 }]}
                            onPress={handleLogin}
                            disabled={isLoading}
                        >
                            {isLoading ? (
                                <ActivityIndicator color="#ffffff" />
                            ) : (
                                <Text style={styles.loginBtnText}>Đăng nhập</Text>
                            )}
                        </TouchableOpacity>

                        <TouchableOpacity
                            style={{ alignItems: 'center', marginTop: 24 }}
                            onPress={() => router.replace('/(auth)/register')}
                        >
                            <Text style={{ fontSize: 15, color: '#767683' }}>
                                Chưa có tài khoản? <Text style={{ fontWeight: 'bold', color: '#1a237e' }}>Đăng ký</Text>
                            </Text>
                        </TouchableOpacity>
                    </View>
                </View>
            </KeyboardAvoidingView>
        </SafeAreaView>
    );
}

// ... (Phần styles giữ nguyên) ...
const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#FBF8FF' },
    content: { flex: 1, justifyContent: 'center', paddingHorizontal: 24 },
    header: { alignItems: 'center', marginBottom: 48 },
    iconCircle: { width: 80, height: 80, borderRadius: 40, backgroundColor: '#e0e0ff', alignItems: 'center', justifyContent: 'center', marginBottom: 16 },
    title: { fontSize: 28, fontWeight: 'bold', color: '#1a237e', marginBottom: 8 },
    subtitle: { fontSize: 16, color: '#767683' },
    form: { gap: 16 },
    inputGroup: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#ffffff', borderRadius: 16, paddingHorizontal: 16, height: 56, borderWidth: 1, borderColor: '#e4e1ea' },
    inputIcon: { marginRight: 12 },
    input: { flex: 1, fontSize: 16, color: '#1b1b21' },
    loginBtn: { backgroundColor: '#1a237e', height: 56, borderRadius: 16, alignItems: 'center', justifyContent: 'center', marginTop: 16, shadowColor: '#1a237e', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.3, shadowRadius: 8, elevation: 4 },
    loginBtnText: { color: '#ffffff', fontSize: 16, fontWeight: 'bold' },
});