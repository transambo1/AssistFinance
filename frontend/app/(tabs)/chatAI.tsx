import React, { useState, useRef } from 'react';
import {
    View, Text, ScrollView, Modal, TouchableOpacity, StyleSheet,
    SafeAreaView, TextInput, KeyboardAvoidingView, Platform,
    ActivityIndicator, Alert, Image
} from 'react-native';
import { MaterialIcons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';
import { aiService } from '../../src/api/aiService';
import { transactionService } from '../../src/api/transactionService'; 
import { categoryService } from '../../src/api/categoryService'; // KHÔI PHỤC: Import Category API
import { useQuery } from '@tanstack/react-query'; // KHÔI PHỤC: Import useQuery
import { Message } from '../../src/types';

export default function AIChatScreen() {
    const [inputText, setInputText] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const scrollViewRef = useRef<ScrollView>(null);

    // --- KHÔI PHỤC: LẤY DANH SÁCH DANH MỤC TỪ BACKEND ---
    const { data: categoryData } = useQuery<any>({
        queryKey: ['categories'],
        queryFn: () => categoryService.getAll(),
    });
    const categories = Array.isArray(categoryData?.data) ? categoryData.data : (categoryData?.data?.data || []);

    // --- STATE CHO MODAL CHỈNH SỬA ---
    const [editingTransaction, setEditingTransaction] = useState<any>(null);
    const [editAmount, setEditAmount] = useState('');
    const [editNote, setEditNote] = useState('');
    const [isSavingEdit, setIsSavingEdit] = useState(false);

    // KHÔI PHỤC: Các state phục vụ chọn Loại và Danh mục
    const [editType, setEditType] = useState<'INCOME' | 'EXPENSE'>('EXPENSE');
    const [editCategoryId, setEditCategoryId] = useState<string>('');
    const [showCategoryPicker, setShowCategoryPicker] = useState(false);

    // Khởi tạo State chứa lịch sử chat
    const [messages, setMessages] = useState<Message[]>([
        {
            id: 'welcome_1',
            sender: 'ai',
            text: 'Chào bạn! Hôm nay bạn có chi tiêu khoản nào không? Cứ nói với tôi bằng ngôn ngữ tự nhiên nhé.',
        }
    ]);
    // --- HÀM XỬ LÝ CHỌN ẢNH VÀ GỌI OCR ---
    const handlePickImage = async () => {
    const { granted } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (!granted) {
        alert("Bạn cần cấp quyền truy cập ảnh.");
        return;
    }

    const result = await ImagePicker.launchImageLibraryAsync({
        mediaTypes: ['images'],
        quality: 1,
    });

    if (!result.canceled) {
        const selectedUri = result.assets[0].uri;
        const tempId = Date.now().toString();

        // 1. Hiện ảnh user gửi lên luôn cho đẹp
        setMessages(prev => [...prev, { id: tempId, sender: 'user', imageUri: selectedUri }]);
        setIsLoading(true);

        try {
    const response = await aiService.uploadOcrImage(selectedUri);

let finalData: any = response;

// Nếu response là string JSON thì parse
if (typeof finalData === 'string') {
    finalData = JSON.parse(finalData);
}

// Nếu response.data tồn tại
if (finalData?.data) {
    finalData = finalData.data;
}

// Nếu response.data.data tồn tại
if (finalData?.data) {
    finalData = finalData.data;
}

// parse string lần nữa nếu nested string
if (typeof finalData === 'string') {
    finalData = JSON.parse(finalData);
}

console.log("FINAL OCR DATA:", finalData);
console.log("AMOUNT:", finalData.amount);

   
    // Kiểm tra xem đã tìm thấy 'amount' chưa
    if (!finalData || finalData.amount === undefined) {
        throw new Error("Không tìm thấy số tiền trong dữ liệu");
    }

    // --- LOGIC HIỂN THỊ (Giữ nguyên phần đẹp đẽ cũ) ---
    const getEmoji = (icon: string) => {
        const map: any = { 'restaurant': '🍔', 'car': '🚗', 'salary': '💰', 'gift': '🎁' };
        return map[icon] || '🧾';
    };

    const emoji = getEmoji(finalData.categoryIcon);
    const typeText = finalData.type === 'INCOME' ? 'Thu nhập 💰' : 'Chi phí 💸';
    const formattedAmount = Number(finalData.amount).toLocaleString('vi-VN');

    const aiReport = `${emoji} Mình đã bóc tách xong hóa đơn:
• Loại: ${typeText}
• Ngày: ${finalData.transactionDate || 'Hôm nay'}
• Số tiền: ₫${formattedAmount}
• Danh mục: ${finalData.categoryName}`;

    setMessages(prev => [...prev, {
        id: `ai_ocr_${Date.now()}`,
        sender: 'ai',
        text: aiReport,
        actionType: 'PARSE_TRANSACTION',
        transactionData: finalData
    }]);

} catch (error: any) {
    console.error("LỖI THỰC TẾ:", error);
    setMessages(prev => [...prev, { 
        id: `err_${Date.now()}`, 
        sender: 'ai', 
        text: `Hic, lỗi xử lý: ${error.message}. Thành check lại cấu trúc data nhé!` 
    }]);
}
    }
}; // Kết thúc hàm handlePickImage
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
        // KHÔI PHỤC: Điền lại Loại và Danh mục cũ
        setEditType(txData.type === 'INCOME' ? 'INCOME' : 'EXPENSE');
        setEditCategoryId(txData.categoryId || '');
        setShowCategoryPicker(false);
    };

    // --- HÀM LƯU GIAO DỊCH SAU KHI SỬA ---
    const handleSaveEdit = async () => {
        if (!editingTransaction?.id) return;
        if (!editCategoryId) {
            Alert.alert("Lỗi", "Vui lòng chọn danh mục.");
            return;
        }

        setIsSavingEdit(true);
        try {
            const updatedAmount = Number(editAmount.replace(/[^0-9]/g, ''));
            const selectedCategory = categories.find((c: any) => c.id === editCategoryId);

            // 1. Gọi API Update lên Server
            await transactionService.update(editingTransaction.id, {
                amount: updatedAmount,
                note: editNote,
                categoryId: editCategoryId, // Lấy ID mới nếu user đổi
                type: editType             // Lấy Type mới nếu user đổi
            });

            // 2. Cập nhật lại UI trong màn hình chat
            setMessages(prev => prev.map(msg => {
                if (msg.actionType === 'PARSE_TRANSACTION' && msg.transactionData?.id === editingTransaction.id) {
                    return {
                        ...msg,
                        transactionData: {
                            ...msg.transactionData,
                            amount: updatedAmount,
                            note: editNote,
                            type: editType,
                            categoryId: editCategoryId,
                            categoryName: selectedCategory ? selectedCategory.name : msg.transactionData.categoryName
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

    // KHÔI PHỤC: Lọc danh mục theo Loại (Thu/Chi) để hiện vào Dropdown
    const availableCategories = categories.filter((c: any) => c.type === editType);
    const selectedCategoryName = availableCategories.find((c: any) => c.id === editCategoryId)?.name || 'Chọn danh mục';

    const renderMessageItem = (msg: Message, index: number) => {
    if (msg.sender === 'user') {
        return (
            <View key={msg.id} style={styles.messageRowUser}>
                {/* --- SỬA LOGIC Ở ĐÂY --- */}
                {msg.imageUri ? (
                    // Nếu có imageUri, hiện hình ảnh
                    <Image 
                        source={{ uri: msg.imageUri }} 
                        style={styles.sentImageUser} 
                        resizeMode="cover"
                    />
                ) : (
                    // Nếu không có, hiện Text bubble cũ
                    <View style={styles.bubbleUser}>
                        <Text style={styles.textUser}>{msg.text}</Text>
                    </View>
                )}
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
                                    <View style={[styles.previewIconWrapper, { backgroundColor: msg.transactionData.type === 'INCOME' ? '#e0f7fa' : '#ffebee' }]}>
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
                            <Text style={styles.suggestionText}>&quot;Sáng nay ăn phở 45k&quot;</Text>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.suggestionChip} onPress={() => setInputText("Đổ xăng hết 60k nha")}>
                            <Text style={styles.suggestionText}>&quot;Đổ xăng hết 60k nha&quot;</Text>
                        </TouchableOpacity>
                        <TouchableOpacity style={styles.suggestionChip} onPress={() => setInputText("Vừa nhận lương 20 củ")}>
                            <Text style={styles.suggestionText}>&quot;Vừa nhận lương 20 củ&quot;</Text>
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
                    <TouchableOpacity style={styles.attachBtn} onPress={handlePickImage}>
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

                        {/* --- KHÔI PHỤC: NÚT CHỌN THU NHẬP / CHI PHÍ --- */}
                        <Text style={styles.modalLabel}>Loại giao dịch</Text>
                        <View style={styles.typeSelectorRow}>
                            <TouchableOpacity 
                                style={[styles.typeBtn, editType === 'EXPENSE' && styles.typeBtnActiveExpense]}
                                onPress={() => { setEditType('EXPENSE'); setEditCategoryId(''); setShowCategoryPicker(false); }}
                            >
                                <Text style={[styles.typeBtnText, editType === 'EXPENSE' && {color: '#fff'}]}>Chi phí</Text>
                            </TouchableOpacity>
                            <TouchableOpacity 
                                style={[styles.typeBtn, editType === 'INCOME' && styles.typeBtnActiveIncome]}
                                onPress={() => { setEditType('INCOME'); setEditCategoryId(''); setShowCategoryPicker(false); }}
                            >
                                <Text style={[styles.typeBtnText, editType === 'INCOME' && {color: '#fff'}]}>Thu nhập</Text>
                            </TouchableOpacity>
                        </View>

                        {/* --- KHÔI PHỤC: DROPDOWN CHỌN DANH MỤC --- */}
                        <Text style={styles.modalLabel}>Danh mục</Text>
                        <TouchableOpacity 
                            style={[styles.modalInput, styles.dropdownSelector]} 
                            onPress={() => setShowCategoryPicker(!showCategoryPicker)}
                        >
                            <Text style={{color: editCategoryId ? '#1b1b21' : '#999', fontSize: 15}}>{selectedCategoryName}</Text>
                            <MaterialIcons name={showCategoryPicker ? "keyboard-arrow-up" : "keyboard-arrow-down"} size={24} color="#767683" />
                        </TouchableOpacity>

                        {showCategoryPicker && (
                            <View style={styles.dropdownList}>
                                <ScrollView nestedScrollEnabled style={{maxHeight: 150}}>
                                    {availableCategories.map((cat: any) => (
                                        <TouchableOpacity 
                                            key={cat.id} 
                                            style={styles.dropdownItem}
                                            onPress={() => {
                                                setEditCategoryId(cat.id);
                                                setShowCategoryPicker(false);
                                            }}
                                        >
                                            <MaterialIcons name={(cat.icon as any) || 'category'} size={20} color={cat.color || '#1a237e'} style={{marginRight: 10}}/>
                                            <Text style={{fontSize: 15, color: '#1b1b21'}}>{cat.name}</Text>
                                        </TouchableOpacity>
                                    ))}
                                    {availableCategories.length === 0 && (
                                        <Text style={{padding: 10, textAlign: 'center', color: '#999'}}>Không có danh mục nào.</Text>
                                    )}
                                </ScrollView>
                            </View>
                        )}

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
    sentImageUser: {
        width: 220,             // Chiều rộng ảnh
        height: 150,            // Chiều cao ảnh
        borderRadius: 12,       // Bo góc ảnh cho đẹp
        marginVertical: 5,      // Khoảng cách trên dưới
        borderWidth: 1,         // (Tùy chọn) Viền ảnh
        borderColor: '#ddd',    // (Tùy chọn) Màu viền
        alignSelf: 'flex-end',  // Đẩy ảnh sang bên phải (phía người dùng)
    },
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
    
    // --- KHÔI PHỤC: Style cho Nút Chọn Loại & Danh mục ---
    typeSelectorRow: { flexDirection: 'row', gap: 12, marginBottom: 16 },
    typeBtn: { flex: 1, paddingVertical: 10, borderRadius: 8, borderWidth: 1, borderColor: '#e8e8e8', alignItems: 'center', backgroundColor: '#f9f9f9' },
    typeBtnText: { fontSize: 14, fontWeight: '600', color: '#767683' },
    typeBtnActiveExpense: { backgroundColor: '#ff635f', borderColor: '#ff635f' },
    typeBtnActiveIncome: { backgroundColor: '#1b6d24', borderColor: '#1b6d24' },
    
    dropdownSelector: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 0 },
    dropdownList: { backgroundColor: '#fff', borderWidth: 1, borderColor: '#e8e8e8', borderRadius: 12, marginTop: 4, marginBottom: 16, elevation: 2 },
    dropdownItem: { flexDirection: 'row', alignItems: 'center', padding: 12, borderBottomWidth: 1, borderBottomColor: '#f0f0f0' },
    
    modalActions: { flexDirection: 'row', gap: 12, marginTop: 8 },
    cancelBtn: { flex: 1, paddingVertical: 14, borderRadius: 12, backgroundColor: '#f0f0f0', alignItems: 'center' },
    cancelBtnText: { fontSize: 15, fontWeight: '600', color: '#454652' },
    saveBtn: { flex: 1, paddingVertical: 14, borderRadius: 12, backgroundColor: '#1a237e', alignItems: 'center' },
    saveBtnText: { fontSize: 15, fontWeight: '600', color: '#ffffff' }
});