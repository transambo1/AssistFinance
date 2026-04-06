import React, { useState, useRef, useEffect } from 'react';
import {
    View, Text, ScrollView, Modal, TouchableOpacity, StyleSheet,
    SafeAreaView, TextInput, KeyboardAvoidingView, Platform,
    ActivityIndicator, Alert
} from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';
import { aiService } from '../../src/api/aiService';
import { transactionService } from '../../src/api/transactionService'; // IMPORT THÊM CÁI NÀY
import { Message } from '../../src/types';

export default function AIChatScreen() {
    const [inputText, setInputText] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const scrollViewRef = useRef<ScrollView>(null);

    // --- STATE CHO MODAL CHỈNH SỬA ---
    const [editingTransaction, setEditingTransaction] = useState<any>(null);
    const [editAmount, setEditAmount] = useState('');
    const [editNote, setEditNote] = useState('');
    const [isSavingEdit, setIsSavingEdit] = useState(false);

    // Khởi tạo State chứa lịch sử chat
    const [messages, setMessages] = useState<Message[]>([
        {
            id: 'welcome_1',
            sender: 'ai',
            text: 'Chào bạn! Hôm nay bạn có chi tiêu khoản nào không? Cứ nói với tôi bằng ngôn ngữ tự nhiên nhé.',
        }
    ]);

    // Hàm xử lý gửi tin nhắn
    const handleSend = async () => {
        if (!inputText.trim()) return;

        const userText = inputText.trim();
        const userMsg: Message = {
            id: `user_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
            sender: 'user',
            text: userText
        };

        setMessages(prev => [...prev, userMsg]);
        setInputText('');
        setIsLoading(true);

        try {
            const aiResponse = await aiService.chatWithAI(userText);

            const aiMsg: Message = {
                id: `ai_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
                sender: 'ai',
                text: aiResponse?.answer || 'Đã xử lý xong.',
                actionType: aiResponse?.actionType,
                transactionData: (aiResponse?.data && Array.isArray(aiResponse.data) && aiResponse.data.length > 0) ? aiResponse.data[0] : null
            };

            setMessages(prev => [...prev, aiMsg]);
        } catch (error) {
            const errorMsg: Message = {
                id: `error_${Date.now()}`,
                sender: 'ai',
                text: 'Xin lỗi, tôi đang gặp sự cố kết nối. Vui lòng thử lại sau!'
            };
            setMessages(prev => [...prev, errorMsg]);
        } finally {
            setIsLoading(false);
        }
    };

    // --- HÀM MỞ MODAL VÀ ĐIỀN DATA CŨ ---
    const handleOpenEdit = (txData: any) => {
        setEditingTransaction(txData);
        setEditAmount(txData.amount ? txData.amount.toString() : '0');
        setEditNote(txData.note || '');
    };

    // --- HÀM LƯU GIAO DỊCH SAU KHI SỬA ---
    const handleSaveEdit = async () => {
        if (!editingTransaction?.id) return;

        setIsSavingEdit(true);
        try {
            const updatedAmount = Number(editAmount.replace(/[^0-9]/g, '')); // Xóa ký tự lạ, chỉ giữ số

            // 1. Gọi API Update lên Server
            await transactionService.update(editingTransaction.id, {
                amount: updatedAmount,
                note: editNote,
                categoryId: editingTransaction.categoryId, // Giữ nguyên danh mục cũ
                type: editingTransaction.type            // Giữ nguyên loại cũ
            });

            // 2. Cập nhật lại UI trong màn hình chat
            setMessages(prev => prev.map(msg => {
                if (msg.actionType === 'PARSE_TRANSACTION' && msg.transactionData?.id === editingTransaction.id) {
                    return {
                        ...msg,
                        transactionData: {
                            ...msg.transactionData,
                            amount: updatedAmount,
                            note: editNote
                        }
                    };
                }
                return msg;
            }));

            // 3. Đóng Modal
            setEditingTransaction(null);
        } catch (error) {
            Alert.alert("Lỗi", "Không thể cập nhật giao dịch. Vui lòng thử lại.");
        } finally {
            setIsSavingEdit(false);
        }
    };

    const renderMessageItem = (msg: Message, index: number) => {
        if (msg.sender === 'user') {
            return (
                <View key={msg.id} style={styles.messageRowUser}>
                    <View style={styles.bubbleUser}>
                        <Text style={styles.textUser}>{msg.text}</Text>
                    </View>
                </View>
            );
        }

        return (
            <React.Fragment key={msg.id}>
                <View style={styles.messageRowAI}>
                    <View style={styles.avatarAI}>
                        <MaterialIcons name="smart-toy" size={20} color="#ffffff" />
                    </View>
                    <View style={styles.bubbleAI}>
                        <Text style={styles.textAI}>{msg.text}</Text>

                        {/* Thẻ Review Chi Tiêu */}
                        {msg.actionType === 'PARSE_TRANSACTION' && msg.transactionData && (
                            <TouchableOpacity
                                style={styles.transactionPreviewCard}
                                onPress={() => handleOpenEdit(msg.transactionData)}
                                activeOpacity={0.7}
                            >
                                <View style={styles.previewCardHeader}>
                                    <Text style={styles.previewStatusText}>
                                        {msg.transactionData.type === 'INCOME' ? 'Đã ghi nhận: Thu nhập' : 'Đã ghi nhận: Chi phí'}
                                    </Text>
                                    <Text style={styles.previewDateText}>
                                        {new Date().toLocaleDateString('vi-VN', { weekday: 'short', day: 'numeric', month: 'short' })}
                                    </Text>
                                </View>

                                <View style={styles.previewCardBody}>
                                    <View style={styles.previewIconWrapper}>
                                        <Text style={styles.previewEmoji}>
                                            {msg.transactionData.type === 'INCOME' ? '💰' : '🍔'}
                                        </Text>
                                    </View>
                                    <View style={styles.previewInfo}>
                                        <Text style={styles.previewCategoryName} numberOfLines={1}>
                                            {msg.transactionData.categoryName || 'Chi tiêu mới'}
                                        </Text>
                                        <Text style={styles.previewNoteText} numberOfLines={1}>
                                            {msg.transactionData.note || 'Không có ghi chú'}
                                        </Text>
                                    </View>
                                    <Text style={[styles.previewAmountText, { color: msg.transactionData.type === 'INCOME' ? '#1b6d24' : '#ff635f' }]}>
                                        {msg.transactionData.type === 'INCOME' ? '+' : '-'}₫{Number(msg.transactionData.amount || 0).toLocaleString('vi-VN')}
                                    </Text>
                                </View>

                                <TouchableOpacity style={styles.quickEditBtn} onPress={() => handleOpenEdit(msg.transactionData)}>
                                    <MaterialIcons name="edit" size={16} color="#1a237e" style={{ marginRight: 6 }} />
                                    <Text style={styles.quickEditText}>Chỉnh sửa giao dịch</Text>
                                </TouchableOpacity>
                            </TouchableOpacity>
                        )}
                    </View>
                </View>

                {index === 0 && (
                    <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.suggestionContainer}>
                        <TouchableOpacity style={styles.suggestionChip} onPress={() => setInputText("Sáng nay ăn phở 45k")}>
                            <Text style={styles.suggestionText}>"Sáng nay ăn phở 45k"</Text>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.suggestionChip} onPress={() => setInputText("Đổ xăng hết 60k nha")}>
                            <Text style={styles.suggestionText}>"Đổ xăng hết 60k nha"</Text>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.suggestionChip} onPress={() => setInputText("Vừa nhận lương 20 củ")}>
                            <Text style={styles.suggestionText}>"Vừa nhận lương 20 củ"</Text>
                        </TouchableOpacity>
                    </ScrollView>
                )}
            </React.Fragment>
        );
    };

    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <Text style={styles.headerTitle}>Trợ lý AI</Text>
                <TouchableOpacity style={styles.headerBtn}>
                    <MaterialIcons name="more-horiz" size={24} color="#000666" />
                </TouchableOpacity>
            </View>

            <KeyboardAvoidingView
                style={styles.keyboardAvoid}
                behavior={Platform.OS === 'ios' ? 'padding' : undefined}
                keyboardVerticalOffset={Platform.OS === 'ios' ? 10 : 0}
            >
                <ScrollView
                    ref={scrollViewRef}
                    style={styles.chatScrollArea}
                    contentContainerStyle={styles.chatContainer}
                    showsVerticalScrollIndicator={false}
                    onContentSizeChange={() => scrollViewRef.current?.scrollToEnd({ animated: true })}
                >
                    {messages.map((msg, index) => renderMessageItem(msg, index))}

                    {isLoading && (
                        <View style={styles.messageRowAI}>
                            <View style={styles.avatarAI}>
                                <MaterialIcons name="smart-toy" size={20} color="#ffffff" />
                            </View>
                            <View style={[styles.bubbleAI, { padding: 12 }]}>
                                <ActivityIndicator size="small" color="#1a237e" />
                            </View>
                        </View>
                    )}
                </ScrollView>

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
                            <TouchableOpacity style={styles.sendBtnActive} onPress={handleSend}>
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

            {/* --- MODAL CHỈNH SỬA GIAO DỊCH --- */}
            <Modal visible={!!editingTransaction} transparent animationType="fade">
                <KeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : "height"} style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        <View style={styles.modalHeader}>
                            <Text style={styles.modalTitle}>Chỉnh sửa giao dịch</Text>
                            <TouchableOpacity onPress={() => setEditingTransaction(null)}>
                                <MaterialIcons name="close" size={24} color="#767683" />
                            </TouchableOpacity>
                        </View>

                        <Text style={styles.modalLabel}>Danh mục</Text>
                        <TextInput style={[styles.modalInput, { backgroundColor: '#f5f5f5', color: '#999' }]} value={editingTransaction?.categoryName || 'Không xác định'} editable={true} />

                        <Text style={styles.modalLabel}>Số tiền (VND)</Text>
                        <TextInput
                            style={styles.modalInput}
                            keyboardType="numeric"
                            value={editAmount}
                            onChangeText={setEditAmount}
                        />

                        <Text style={styles.modalLabel}>Ghi chú</Text>
                        <TextInput
                            style={styles.modalInput}
                            value={editNote}
                            onChangeText={setEditNote}
                            placeholder="Thêm ghi chú..."
                        />

                        <View style={styles.modalActions}>
                            <TouchableOpacity style={styles.cancelBtn} onPress={() => setEditingTransaction(null)} disabled={isSavingEdit}>
                                <Text style={styles.cancelBtnText}>Hủy</Text>
                            </TouchableOpacity>
                            <TouchableOpacity style={styles.saveBtn} onPress={handleSaveEdit} disabled={isSavingEdit}>
                                {isSavingEdit ? (
                                    <ActivityIndicator size="small" color="#ffffff" />
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

// BỘ STYLE
const styles = StyleSheet.create({
    container: { flex: 1, backgroundColor: '#FBF8FF' },
    header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 24, paddingTop: 16, paddingBottom: 16 },
    headerTitle: { fontSize: 24, fontWeight: 'bold', color: '#000666' },
    headerBtn: { padding: 8 },

    keyboardAvoid: { flex: 1, marginBottom: 28 },
    chatScrollArea: { flex: 1 },
    chatContainer: { paddingHorizontal: 24, paddingBottom: 10, paddingTop: 16 },

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

    inputArea: {
        flexDirection: 'row', borderWidth: 0.5, borderBottomWidth: 0,
        borderTopLeftRadius: 25, borderTopRightRadius: 25, borderColor: '#858585',
        alignItems: 'flex-end', paddingHorizontal: 24, paddingVertical: 12,
        backgroundColor: '#FBF8FF', paddingBottom: Platform.OS === 'ios' ? 30 : 20
    },
    attachBtn: { padding: 12, marginRight: 8, borderRadius: 25, backgroundColor: '#e6dafdd1', alignItems: 'center', justifyContent: 'center' },
    inputWrapper: { flex: 1, flexDirection: 'row', alignItems: 'center', backgroundColor: '#ffffff', borderRadius: 24, paddingLeft: 16, paddingRight: 8, paddingVertical: 4, shadowColor: '#000', shadowOffset: { width: 0, height: -2 }, shadowOpacity: 0.05, shadowRadius: 8, elevation: 4, },
    input: { flex: 1, minHeight: 40, maxHeight: 100, fontSize: 15, color: '#1b1b21', paddingRight: 8 },
    sendBtnActive: { width: 40, height: 40, borderRadius: 20, backgroundColor: '#1a237e', alignItems: 'center', justifyContent: 'center', marginLeft: 8 },
    micBtn: { width: 40, height: 40, borderRadius: 20, backgroundColor: '#f5f2fb', alignItems: 'center', justifyContent: 'center', marginLeft: 8 },

    transactionPreviewCard: {
        marginTop: 20, backgroundColor: '#ffffff', borderRadius: 16, padding: 16,
        borderWidth: 1, borderColor: '#e8e8e8', width: '100%',
        shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2,
    },
    previewCardHeader: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 12 },
    previewStatusText: { fontSize: 13, color: '#454652', fontWeight: '500' },
    previewDateText: { fontSize: 13, color: '#767683' },
    previewCardBody: { flexDirection: 'row', alignItems: 'center' },
    previewIconWrapper: { width: 44, height: 44, borderRadius: 22, backgroundColor: '#e0f7fa', alignItems: 'center', justifyContent: 'center', marginRight: 12 },
    previewEmoji: { fontSize: 20 },
    previewInfo: { flex: 1 },
    previewCategoryName: { fontSize: 16, fontWeight: '600', color: '#1b1b21', marginBottom: 4 },
    previewNoteText: { fontSize: 13, color: '#767683' },
    previewAmountText: { fontSize: 16, fontWeight: 'bold' },
    quickEditBtn: {
        marginTop: 16, paddingVertical: 10, backgroundColor: '#f5f2fb',
        borderRadius: 8, flexDirection: 'row', alignItems: 'center', justifyContent: 'center'
    },
    quickEditText: { fontSize: 14, fontWeight: '600', color: '#1a237e' },

    // Modal Style
    modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.5)', justifyContent: 'center', alignItems: 'center' },
    modalContent: { width: '85%', backgroundColor: '#fff', borderRadius: 20, padding: 24, elevation: 5 },
    modalHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 },
    modalTitle: { fontSize: 18, fontWeight: 'bold', color: '#1b1b21' },
    modalLabel: { fontSize: 13, fontWeight: '600', color: '#454652', marginBottom: 8 },
    modalInput: { borderWidth: 1, borderColor: '#e8e8e8', borderRadius: 12, paddingHorizontal: 16, paddingVertical: 12, fontSize: 15, marginBottom: 16, color: '#1b1b21' },
    modalActions: { flexDirection: 'row', gap: 12, marginTop: 8 },
    cancelBtn: { flex: 1, paddingVertical: 14, borderRadius: 12, backgroundColor: '#f0f0f0', alignItems: 'center' },
    cancelBtnText: { fontSize: 15, fontWeight: '600', color: '#454652' },
    saveBtn: { flex: 1, paddingVertical: 14, borderRadius: 12, backgroundColor: '#1a237e', alignItems: 'center' },
    saveBtnText: { fontSize: 15, fontWeight: '600', color: '#ffffff' }
});