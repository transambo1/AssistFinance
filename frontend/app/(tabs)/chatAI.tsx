import React, { useState } from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet, SafeAreaView, TextInput, KeyboardAvoidingView, Platform } from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';

export default function AIChatScreen() {
    const [inputText, setInputText] = useState('');

    return (
        <SafeAreaView style={styles.container}>
            {/* Header giữ cố định ở trên cùng */}
            <View style={styles.header}>
                <Text style={styles.headerTitle}>Trợ lý AI</Text>
                <TouchableOpacity style={styles.headerBtn}>
                    <MaterialIcons name="more-horiz" size={24} color="#000666" />
                </TouchableOpacity>
            </View>

            {/* KeyboardAvoidingView bọc toàn bộ nội dung chat và thanh nhập liệu */}
            <KeyboardAvoidingView
                style={styles.keyboardAvoid}
                behavior={Platform.OS === 'ios' ? 'padding' : undefined}
                keyboardVerticalOffset={Platform.OS === 'ios' ? 10 : 0} // Thêm offset để tránh bị che một nửa trên iOS
            >
                {/* Phần danh sách tin nhắn dùng flex: 1 để tự động đẩy thanh chat xuống đáy */}
                <ScrollView
                    style={styles.chatScrollArea}
                    contentContainerStyle={styles.chatContainer}
                    showsVerticalScrollIndicator={false}
                >
                    {/* AI Message - Welcome */}
                    <View style={styles.messageRowAI}>
                        <View style={styles.avatarAI}>
                            <MaterialIcons name="smart-toy" size={20} color="#ffffff" />
                        </View>
                        <View style={styles.bubbleAI}>
                            <Text style={styles.textAI}>Chào Thành! Hôm nay bạn có chi tiêu khoản nào không? Cứ nói với tôi bằng ngôn ngữ tự nhiên nhé.</Text>
                        </View>
                    </View>

                    {/* Quick Suggestions */}
                    <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.suggestionContainer}>
                        <TouchableOpacity style={styles.suggestionChip}>
                            <Text style={styles.suggestionText}>"Sáng nay ăn phở 45k"</Text>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.suggestionChip}>
                            <Text style={styles.suggestionText}>"Đổ xăng hết 60k nha"</Text>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.suggestionChip}>
                            <Text style={styles.suggestionText}>"Vừa nhận lương 20 củ"</Text>
                        </TouchableOpacity>
                    </ScrollView>

                    {/* User Message - Example */}
                    <View style={styles.messageRowUser}>
                        <View style={styles.bubbleUser}>
                            <Text style={styles.textUser}>Hôm qua mình đi siêu thị mua đồ ăn hết 350k</Text>
                        </View>
                    </View>

                    {/* AI Message - Response & Extraction */}
                    <View style={styles.messageRowAI}>
                        <View style={styles.avatarAI}>
                            <MaterialIcons name="smart-toy" size={20} color="#ffffff" />
                        </View>
                        <View style={styles.bubbleAI}>
                            <Text style={styles.textAI}>Đã ghi nhận! Tôi vừa tạo một giao dịch mới cho bạn.</Text>

                            {/* Transaction Preview Card */}
                            <View style={styles.transactionPreview}>
                                <View style={styles.previewHeader}>
                                    <View style={[styles.iconBox, { backgroundColor: '#ffe5e5' }]} >
                                        <MaterialIcons name="shopping-cart" size={20} color="#ff635f" />
                                    </View>
                                    <View>
                                        <Text style={styles.previewName}>Siêu thị (Đồ ăn)</Text>
                                        <Text style={styles.previewDate}>Hôm qua</Text>
                                    </View>
                                </View>
                                <Text style={styles.previewAmount}>-350.000 đ</Text>

                                <View style={styles.previewActions}>
                                    <TouchableOpacity style={styles.btnEdit}>
                                        <Text style={styles.btnTextEdit}>Chỉnh sửa</Text>
                                    </TouchableOpacity>
                                    <TouchableOpacity style={styles.btnConfirm}>
                                        <Text style={styles.btnTextConfirm}>Xác nhận lưu</Text>
                                    </TouchableOpacity>
                                </View>
                            </View>
                        </View>
                    </View>
                </ScrollView>

                {/* Input Area (Đã bỏ position: absolute để chạy theo Keyboard) */}
                <View style={styles.inputArea}>
                    <TouchableOpacity style={styles.attachBtn}>
                        <MaterialIcons name="image" size={24} color="#767683" />
                    </TouchableOpacity>

                    <View style={styles.inputWrapper}>
                        <TextInput
                            style={styles.input}
                            placeholder="Nhập chi tiêu..."
                            placeholderTextColor="#767683"
                            value={inputText}
                            onChangeText={setInputText}
                            multiline
                        />
                        {inputText.length > 0 ? (
                            <TouchableOpacity style={styles.sendBtnActive}>
                                <MaterialIcons name="send" size={20} color="#ffffff" />
                            </TouchableOpacity>
                        ) : (
                            <TouchableOpacity style={styles.micBtn}>
                                <MaterialIcons name="mic" size={24} color="#1a237e" />
                            </TouchableOpacity>
                        )}
                    </View>
                </View>
            </KeyboardAvoidingView>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#FBF8FF' },
    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 24, paddingTop: 16, paddingBottom: 16 },
    headerTitle: { fontSize: 24, fontWeight: 'bold', color: '#000666' },
    headerBtn: { padding: 8 },

    keyboardAvoid: { flex: 1 },
    chatScrollArea: { flex: 1 }, // Chiếm phần còn lại của màn hình
    chatContainer: { paddingHorizontal: 24, paddingBottom: 10, paddingTop: 16 }, // Padding vừa đủ để cuộn đẹp

    messageRowAI: { flexDirection: 'row', marginBottom: 24, alignItems: 'flex-start', maxWidth: '85%' },
    avatarAI: { width: 36, height: 36, borderRadius: 18, backgroundColor: '#1a237e', alignItems: 'center', justifyContent: 'center', marginRight: 12 },
    bubbleAI: { backgroundColor: '#ffffff', padding: 16, borderRadius: 20, borderTopLeftRadius: 4, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 1 },
    textAI: { fontSize: 15, color: '#1b1b21', lineHeight: 22 },

    messageRowUser: { flexDirection: 'row', marginBottom: 24, justifyContent: 'flex-end' },
    bubbleUser: { backgroundColor: '#e0e0ff', padding: 16, borderRadius: 20, borderTopRightRadius: 4, maxWidth: '80%' },
    textUser: { fontSize: 15, color: '#000666', lineHeight: 22 },

    suggestionContainer: { paddingLeft: 48, marginBottom: 24, gap: 8 },
    suggestionChip: { paddingHorizontal: 16, paddingVertical: 8, backgroundColor: '#f5f2fb', borderRadius: 16, borderWidth: 1, borderColor: '#e4e1ea', marginRight: 8 },
    suggestionText: { fontSize: 13, color: '#4c56af', fontWeight: '500' },

    transactionPreview: { marginTop: 16, backgroundColor: '#f5f2fb', padding: 16, borderRadius: 16, borderWidth: 1, borderColor: '#e4e1ea' },
    previewHeader: { flexDirection: 'row', alignItems: 'center', gap: 12, marginBottom: 12 },
    iconBox: { width: 40, height: 40, borderRadius: 20, alignItems: 'center', justifyContent: 'center' },
    previewName: { fontSize: 14, fontWeight: 'bold', color: '#1b1b21' },
    previewDate: { fontSize: 12, color: '#767683' },
    previewAmount: { fontSize: 18, fontWeight: 'bold', color: '#ff635f', marginBottom: 16 },
    previewActions: { flexDirection: 'row', gap: 12 },
    btnEdit: { flex: 1, paddingVertical: 10, borderRadius: 12, backgroundColor: '#ffffff', alignItems: 'center', borderWidth: 1, borderColor: '#e4e1ea' },
    btnTextEdit: { fontSize: 13, fontWeight: '600', color: '#454652' },
    btnConfirm: { flex: 1, paddingVertical: 10, borderRadius: 12, backgroundColor: '#1a237e', alignItems: 'center' },
    btnTextConfirm: { fontSize: 13, fontWeight: '600', color: '#ffffff' },

    inputArea: {
        flexDirection: 'row',
        borderWidth: 0.5,
        borderBottomWidth: 0,
        borderTopLeftRadius: 25,
        borderTopRightRadius: 25,
        borderColor: '#858585',
        alignItems: 'flex-end',
        paddingHorizontal: 24,
        paddingVertical: 12,
        backgroundColor: '#FBF8FF',
        paddingBottom: Platform.OS === 'ios' ? 70 : 70
    },
    attachBtn: { padding: 12, marginRight: 8, borderRadius: 25, backgroundColor: '#e6dafdd1', alignItems: 'center', justifyContent: 'center' },
    inputWrapper: { flex: 1, flexDirection: 'row', alignItems: 'center', backgroundColor: '#ffffff', borderRadius: 24, paddingLeft: 16, paddingRight: 8, paddingVertical: 4, shadowColor: '#000', shadowOffset: { width: 0, height: -2 }, shadowOpacity: 0.05, shadowRadius: 8, elevation: 4, },
    input: { flex: 1, minHeight: 40, maxHeight: 100, fontSize: 15, color: '#1b1b21', paddingRight: 8 },
    sendBtnActive: { width: 40, height: 40, borderRadius: 20, backgroundColor: '#1a237e', alignItems: 'center', justifyContent: 'center', marginLeft: 8 },
    micBtn: { width: 40, height: 40, borderRadius: 20, backgroundColor: '#f5f2fb', alignItems: 'center', justifyContent: 'center', marginLeft: 8 },
});