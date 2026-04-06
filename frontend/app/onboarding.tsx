import React, { useState } from 'react';
import { View, Text, StyleSheet, SafeAreaView, TouchableOpacity, Dimensions } from 'react-native';
import { useRouter } from 'expo-router';
import { MaterialIcons } from '@expo/vector-icons';

const { width } = Dimensions.get('window');

const SLIDES = [
    { title: "Theo dõi thông minh", desc: "Quản lý mọi khoản thu chi trong lòng bàn tay với giao diện hiện đại.", icon: "account-balance-wallet" },
    { title: "Trợ lý ảo AI", desc: "Không còn phải nhập tay lằng nhằng. Chỉ cần nhắn hay gửi ảnh, AI sẽ làm nốt phần còn lại.", icon: "smart-toy" },
    { title: "Dự báo tương lai", desc: "Hệ thống tự động phân tích và đưa ra lời khuyên để bạn không bao giờ rỗng túi.", icon: "insights" }
];

export default function OnboardingScreen() {
    const [currentSlide, setCurrentSlide] = useState(0);
    const router = useRouter();

    const nextSlide = () => {
        if (currentSlide < SLIDES.length - 1) {
            setCurrentSlide(currentSlide + 1);
        } else {
            // router.replace giúp người dùng vào app và KHÔNG THỂ bấm nút quay lại Onboarding được nữa
            router.replace('/(auth)/login');
        }
    };

    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.content}>
                <View style={styles.iconCircle}>
                    <MaterialIcons name={SLIDES[currentSlide].icon as any} size={80} color="#1a237e" />
                </View>

                <Text style={styles.title}>{SLIDES[currentSlide].title}</Text>
                <Text style={styles.desc}>{SLIDES[currentSlide].desc}</Text>

                {/* Chỉ báo trang (Dots) */}
                <View style={styles.dotContainer}>
                    {SLIDES.map((_, i) => (
                        <View key={i} style={[styles.dot, currentSlide === i && styles.activeDot]} />
                    ))}
                </View>
            </View>

            <TouchableOpacity style={styles.btn} onPress={nextSlide}>
                <Text style={styles.btnText}>
                    {currentSlide === SLIDES.length - 1 ? "Bắt đầu ngay" : "Tiếp theo"}
                </Text>
            </TouchableOpacity>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#FBF8FF', justifyContent: 'space-between', paddingVertical: 50 },
    content: { alignItems: 'center', paddingHorizontal: 40, marginTop: 50 },
    iconCircle: { width: 160, height: 160, borderRadius: 80, backgroundColor: '#ffffff', alignItems: 'center', justifyContent: 'center', marginBottom: 40, elevation: 5, shadowColor: '#000', shadowOpacity: 0.1, shadowRadius: 10 },
    title: { fontSize: 28, fontWeight: 'bold', color: '#000666', textAlign: 'center', marginBottom: 16 },
    desc: { fontSize: 16, color: '#767683', textAlign: 'center', lineHeight: 24 },
    dotContainer: { flexDirection: 'row', gap: 8, marginTop: 40 },
    dot: { width: 8, height: 8, borderRadius: 4, backgroundColor: '#c6c5d4' },
    activeDot: { width: 24, backgroundColor: '#1a237e' },
    btn: { backgroundColor: '#1a237e', marginHorizontal: 40, paddingVertical: 18, borderRadius: 20, alignItems: 'center' },
    btnText: { color: '#ffffff', fontSize: 16, fontWeight: 'bold' }
});