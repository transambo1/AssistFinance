// Lấy Key từ file .env an toàn
const GROQ_API_KEY = process.env.EXPO_PUBLIC_GROQ_API_KEY;

export const aiService = {
    generateFinancialVision: async (salary: string, day: string) => {
        // 1. Kiểm tra Key
        if (!GROQ_API_KEY) {
            console.error("❌ Lỗi: Chưa cấu hình EXPO_PUBLIC_GROQ_API_KEY trong file .env");
            return fallbackData();
        }

        try {
            const response = await fetch("https://api.groq.com/openai/v1/chat/completions", {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${GROQ_API_KEY}`,
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    // Sử dụng Model Llama 3.3 mới nhất và mạnh nhất của Groq
                    model: "llama-3.3-70b-versatile",
                    messages: [
                        {
                            role: "system",
                            content: `Bạn là chuyên gia tư vấn tài chính của ứng dụng Aeon Ledger. 
                            Nhiệm vụ: Phân tích tài chính ngắn gọn.
                            BẮT BUỘC: Trả về kết quả dưới định dạng json nguyên bản, không kèm văn bản thừa.`
                        },
                        {
                            role: "user",
                            content: `Thông tin: Lương ${salary} VND, nhận ngày ${day} hàng tháng. 
                            Hãy tạo một tầm nhìn tài chính cho năm 2026 dưới định dạng json với cấu trúc: 
                            {"title": "tên_tiêu_đề", "description": "lời_khuyên_chi_tiết"}`
                        }
                    ],
                    // Ép kiểu JSON Object yêu cầu từ khóa "json" phải xuất hiện trong messages ở trên
                    response_format: { type: "json_object" },
                    temperature: 0.8,
                })
            });

            // 2. Kiểm tra phản hồi HTTP
            if (!response.ok) {
                const errorData = await response.json();
                console.error("❌ Groq API Error Detail:", JSON.stringify(errorData));
                return fallbackData();
            }

            const data = await response.json();
            const content = data?.choices?.[0]?.message?.content;

            // 3. Kiểm tra dữ liệu thô
            if (!content) {
                throw new Error("Dữ liệu content bị rỗng");
            }

            // 4. Parse JSON an toàn
            return JSON.parse(content) as { title: string; description: string };

        } catch (error) {
            console.error("❌ Lỗi hệ thống AI Service:", error);
            return fallbackData();
        }
    },
};

// Dữ liệu dự phòng khi gặp sự cố API
const fallbackData = () => ({
    title: "Tầm nhìn Tài chính 2026",
    description: "Hệ thống AI đang bảo trì. Dựa trên dữ liệu cũ, bạn nên duy trì tỷ lệ tiết kiệm 20% thu nhập để chuẩn bị cho các mục tiêu dài hạn trong năm 2026."
});