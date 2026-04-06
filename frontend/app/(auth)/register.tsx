import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, SafeAreaView, KeyboardAvoidingView, Platform, ScrollView, ActivityIndicator } from 'react-native';
import { useRouter } from 'expo-router';
import { MaterialIcons } from '@expo/vector-icons';
import { userService } from '../../src/api/userService';

export default function RegisterScreen() {
    const router = useRouter();

    // 1. State lưu dữ liệu (Đã thêm username)
    const [fullName, setFullName] = useState('');
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');

    // 2. State lưu lỗi
    const [fullNameError, setFullNameError] = useState('');
    const [usernameError, setUsernameError] = useState('');
    const [emailError, setEmailError] = useState('');
    const [passwordError, setPasswordError] = useState('');
    const [confirmError, setConfirmError] = useState('');

    // State loading
    const [isLoading, setIsLoading] = useState(false);

    const isValidEmail = (emailString: string) => {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(emailString);
    };

    const handleRegister = async () => {
        let isValid = true;

        // Reset lỗi
        setFullNameError('');
        setUsernameError('');
        setEmailError('');
        setPasswordError('');
        setConfirmError('');

        // Validate Full Name
        if (!fullName.trim()) {
            setFullNameError('Vui lòng nhập họ tên');
            isValid = false;
        }

        // Validate Username
        if (!username.trim()) {
            setUsernameError('Vui lòng nhập tên đăng nhập');
            isValid = false;
        } else if (username.includes(' ')) {
            setUsernameError('Tên đăng nhập không được chứa khoảng trắng');
            isValid = false;
        } else if (username.length < 3) {
            setUsernameError('Tên đăng nhập phải có ít nhất 3 ký tự');
            isValid = false;
        }

        // Validate Email
        if (!email.trim()) {
            setEmailError('Vui lòng nhập email');
            isValid = false;
        } else if (!isValidEmail(email)) {
            setEmailError('Email không hợp lệ');
            isValid = false;
        }

        // Validate Password
        if (!password.trim()) {
            setPasswordError('Vui lòng nhập mật khẩu');
            isValid = false;
        } else if (password.length < 6) {
            setPasswordError('Mật khẩu phải có ít nhất 6 ký tự');
            isValid = false;
        }

        // Validate Xác nhận Password
        if (confirmPassword !== password) {
            setConfirmError('Mật khẩu xác nhận không khớp');
            isValid = false;
        }

        if (isValid) {
            try {
                setIsLoading(true);

                const payload = {
                    username: username.trim(),
                    password: password,
                    fullName: fullName.trim(),
                    email: email.trim()
                };

                console.log("Đang gửi lên BE:", payload);

                await userService.register(payload);

                alert('Đăng ký thành công! Vui lòng đăng nhập.');
                router.replace('/(auth)/login');

            } catch (error: any) {
                console.error("Lỗi từ BE dội về:", error);
                alert(error.message || 'Đăng ký thất bại! Vui lòng kiểm tra lại thông tin.');
            } finally {
                setIsLoading(false);
            }
        }
    };

    return (
        <SafeAreaView style={styles.container}>
            <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={{ flex: 1 }}>
                <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>

                    <View style={styles.header}>
                        <View style={styles.iconCircle}>
                            <MaterialIcons name="person-add" size={48} color="#1a237e" />
                        </View>
                        <Text style={styles.title}>Tạo tài khoản</Text>
                        <Text style={styles.subtitle}>Bắt đầu hành trình quản lý tài chính</Text>
                    </View>

                    <View style={styles.form}>

                        <View style={styles.inputWrapper}>
                            <View style={[styles.inputGroup, fullNameError ? styles.inputError : null]}>
                                <MaterialIcons name="badge" size={20} color={fullNameError ? '#ff635f' : '#767683'} style={styles.inputIcon} />
                                <TextInput
                                    style={styles.input}
                                    placeholder="Họ và tên (Ví dụ: Nguyễn Văn A)"
                                    placeholderTextColor="#c6c5d4"
                                    value={fullName}
                                    onChangeText={(text) => { setFullName(text); setFullNameError(''); }}
                                />
                            </View>
                            {fullNameError ? <Text style={styles.errorText}>{fullNameError}</Text> : null}
                        </View>

                        <View style={styles.inputWrapper}>
                            <View style={[styles.inputGroup, usernameError ? styles.inputError : null]}>
                                <MaterialIcons name="person" size={20} color={usernameError ? '#ff635f' : '#767683'} style={styles.inputIcon} />
                                <TextInput
                                    style={styles.input}
                                    placeholder="Tên đăng nhập (viết liền không dấu)"
                                    placeholderTextColor="#c6c5d4"
                                    value={username}
                                    onChangeText={(text) => { setUsername(text); setUsernameError(''); }}
                                    autoCapitalize="none"
                                />
                            </View>
                            {usernameError ? <Text style={styles.errorText}>{usernameError}</Text> : null}
                        </View>

                        {/* --- Ô NHẬP EMAIL --- */}
                        <View style={styles.inputWrapper}>
                            <View style={[styles.inputGroup, emailError ? styles.inputError : null]}>
                                <MaterialIcons name="email" size={20} color={emailError ? '#ff635f' : '#767683'} style={styles.inputIcon} />
                                <TextInput
                                    style={styles.input}
                                    placeholder="Email của bạn"
                                    placeholderTextColor="#c6c5d4"
                                    value={email}
                                    onChangeText={(text) => { setEmail(text); setEmailError(''); }}
                                    keyboardType="email-address"
                                    autoCapitalize="none"
                                />
                            </View>
                            {emailError ? <Text style={styles.errorText}>{emailError}</Text> : null}
                        </View>

                        {/* --- Ô NHẬP PASSWORD --- */}
                        <View style={styles.inputWrapper}>
                            <View style={[styles.inputGroup, passwordError ? styles.inputError : null]}>
                                <MaterialIcons name="lock" size={20} color={passwordError ? '#ff635f' : '#767683'} style={styles.inputIcon} />
                                <TextInput
                                    style={styles.input}
                                    placeholder="Mật khẩu (ít nhất 6 ký tự)"
                                    placeholderTextColor="#c6c5d4"
                                    value={password}
                                    onChangeText={(text) => { setPassword(text); setPasswordError(''); }}
                                    secureTextEntry
                                />
                            </View>
                            {passwordError ? <Text style={styles.errorText}>{passwordError}</Text> : null}
                        </View>

                        {/* --- Ô XÁC NHẬN PASSWORD --- */}
                        <View style={styles.inputWrapper}>
                            <View style={[styles.inputGroup, confirmError ? styles.inputError : null]}>
                                <MaterialIcons name="verified-user" size={20} color={confirmError ? '#ff635f' : '#767683'} style={styles.inputIcon} />
                                <TextInput
                                    style={styles.input}
                                    placeholder="Xác nhận lại mật khẩu"
                                    placeholderTextColor="#c6c5d4"
                                    value={confirmPassword}
                                    onChangeText={(text) => { setConfirmPassword(text); setConfirmError(''); }}
                                    secureTextEntry
                                />
                            </View>
                            {confirmError ? <Text style={styles.errorText}>{confirmError}</Text> : null}
                        </View>

                        {/* NÚT ĐĂNG KÝ CÓ LOADING */}
                        <TouchableOpacity
                            style={[styles.registerBtn, isLoading && { opacity: 0.7 }]}
                            onPress={handleRegister}
                            disabled={isLoading}
                        >
                            {isLoading ? (
                                <ActivityIndicator color="#ffffff" />
                            ) : (
                                <Text style={styles.registerBtnText}>Đăng ký ngay</Text>
                            )}
                        </TouchableOpacity>

                        {/* CHUYỂN SANG ĐĂNG NHẬP */}
                        <TouchableOpacity style={styles.linkBtn} onPress={() => router.replace('/(auth)/login')}>
                            <Text style={styles.linkText}>Đã có tài khoản? <Text style={styles.linkTextBold}>Đăng nhập</Text></Text>
                        </TouchableOpacity>

                    </View>

                </ScrollView>
            </KeyboardAvoidingView>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#FBF8FF' },
    scrollContent: { flexGrow: 1, justifyContent: 'center', paddingHorizontal: 24, paddingVertical: 40 },
    header: { alignItems: 'center', marginBottom: 40 },
    iconCircle: { width: 80, height: 80, borderRadius: 40, backgroundColor: '#e0e0ff', alignItems: 'center', justifyContent: 'center', marginBottom: 16 },
    title: { fontSize: 28, fontWeight: 'bold', color: '#1a237e', marginBottom: 8 },
    subtitle: { fontSize: 16, color: '#767683' },

    form: { gap: 16 },
    inputWrapper: { gap: 4 },
    inputGroup: { flexDirection: 'row', alignItems: 'center', backgroundColor: '#ffffff', borderRadius: 16, paddingHorizontal: 16, height: 56, borderWidth: 1, borderColor: '#e4e1ea' },
    inputError: { borderColor: '#ff635f', backgroundColor: '#fff5f5' },
    inputIcon: { marginRight: 12 },
    input: { flex: 1, fontSize: 16, color: '#1b1b21' },
    errorText: { color: '#ff635f', fontSize: 12, marginLeft: 16 },

    registerBtn: { backgroundColor: '#1a237e', height: 56, borderRadius: 16, alignItems: 'center', justifyContent: 'center', marginTop: 8, shadowColor: '#1a237e', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.3, shadowRadius: 8, elevation: 4 },
    registerBtnText: { color: '#ffffff', fontSize: 16, fontWeight: 'bold' },

    linkBtn: { alignItems: 'center', marginTop: 16 },
    linkText: { fontSize: 15, color: '#767683' },
    linkTextBold: { fontWeight: 'bold', color: '#1a237e' }
});