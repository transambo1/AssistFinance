import { Stack } from 'expo-router';

export default function RootLayout() {
  return (
    <Stack
      // initialRouteName giúp xác định màn hình nào hiện ra đầu tiên
      initialRouteName="onboarding"
    >
      {/* 1. Màn hình Chào mừng (Hiện đầu tiên) */}
      <Stack.Screen
        name="onboarding"
        options={{
          headerShown: false,
          animation: 'fade', // Hiệu ứng chuyển cảnh mờ dần cho đẹp
        }}
      />

      {/* 2. Cụm màn hình chính (Sau khi bấm Bắt đầu ở Onboarding) */}
      <Stack.Screen
        name="(tabs)"
        options={{
          headerShown: false,
          animation: 'slide_from_right',
        }}
      />

      {/* 3. Màn hình Thêm giao dịch (Dạng Modal trượt từ dưới lên) */}
      <Stack.Screen
        name="add-transaction"
        options={{
          presentation: 'modal',
          headerShown: false
        }}
      />
    </Stack>
  );
}