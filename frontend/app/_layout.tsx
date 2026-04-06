import { Stack } from 'expo-router';
import { ThemeProvider } from '../src/context/ThemeContext';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient();

export default function RootLayout() {
  return (
    // 3. Bọc QueryClientProvider ngoài cùng
    <QueryClientProvider client={queryClient}>
      <ThemeProvider>
        <Stack
          initialRouteName="onboarding"
          screenOptions={{ headerShown: false }}
        >
          <Stack.Screen name="onboarding" options={{ animation: 'fade' }} />
          <Stack.Screen name="(auth)" options={{ animation: 'fade' }} />
          <Stack.Screen name="(tabs)" options={{ animation: 'slide_from_right' }} />
          {/* <Stack.Screen name="add-transaction" options={{ presentation: 'modal' }} /> */}
        </Stack>
      </ThemeProvider>
    </QueryClientProvider>
  );
}