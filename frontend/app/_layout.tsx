import { ThemeProvider as NavThemeProvider, DefaultTheme } from '@react-navigation/native';
import { PaperProvider } from 'react-native-paper';
import { Stack } from 'expo-router';
// 1. IMPORT THÊM CÁI NÀY
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider as MyCustomThemeProvider } from '../src/context/ThemeContext';

// 2. KHỞI TẠO QueryClient (để ngoài component nhé)
const queryClient = new QueryClient();

export default function RootLayout() {
  return (
    // 3. BỌC LỚP NGOÀI CÙNG LÀ QueryClientProvider
    <QueryClientProvider client={queryClient}>
      <MyCustomThemeProvider>
        <PaperProvider>
          <NavThemeProvider value={DefaultTheme}>
            <Stack>
              <Stack.Screen name="index" options={{ headerShown: false }} />
              <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
              <Stack.Screen name="(auth)/login" options={{ headerShown: false }} />
              <Stack.Screen name="(auth)/register" options={{ headerShown: false }} />
              <Stack.Screen name="add-transaction" options={{ headerShown: false }} />
              <Stack.Screen name="profile" options={{ headerShown: false }} />
              <Stack.Screen name="DashboardScreen" options={{ headerShown: false }} />
              <Stack.Screen name="create-transaction" options={{ headerShown: false }} />
              <Stack.Screen name="create-budget" options={{ headerShown: false }} />
              <Stack.Screen name="personal-salary" options={{ headerShown: false }} />
              <Stack.Screen name="transaction-detail" options={{ headerShown: false }} />
              <Stack.Screen name="manage-categories" options={{ headerShown: false }} />
                 <Stack.Screen name="create-category" options={{ headerShown: false }} />
            </Stack>
          </NavThemeProvider>
        </PaperProvider>
      </MyCustomThemeProvider>

    </QueryClientProvider>
  );
}