import { Tabs } from 'expo-router';
import { View, Platform, StyleSheet } from 'react-native';
import { BlurView } from 'expo-blur';
import Ionicons from '@expo/vector-icons/build/Ionicons';

export default function TabLayout() {
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarShowLabel: false, // Ẩn chữ hoàn toàn để tối giản
        tabBarStyle: styles.tabBar,
        tabBarActiveTintColor: '#1A237E', // Màu xanh chủ đạo khi focus
        tabBarInactiveTintColor: '#9CA3AF', // Màu xám nhạt khi không focus
        // Ép các item nằm chính giữa thanh tab
        tabBarItemStyle: {
          justifyContent: 'center',
          alignItems: 'center',
          paddingTop: Platform.OS === 'ios' ? 15 : 0, // Sửa lỗi lệch tâm trên iOS
        },
        // Nền kính mờ bo góc
        tabBarBackground: () => (
          <View style={styles.blurContainer}>
            <BlurView intensity={40} style={StyleSheet.absoluteFill} tint="light" />
          </View>
        ),
      }}>
      
      {/* 1. Trang chủ */}
      <Tabs.Screen
        name="index"
        options={{
          tabBarIcon: ({ focused, color }) => (
            <View style={focused ? styles.activeIconWrapper : styles.iconWrapper}>
              <Ionicons name={focused ? "home" : "home-outline"} size={26} color={color} />
            </View>
          ),
        }}
      />

      {/* 2. Lịch sử giao dịch */}
      <Tabs.Screen
        name="history"
        options={{
          tabBarIcon: ({ focused, color }) => (
            <View style={focused ? styles.activeIconWrapper : styles.iconWrapper}>
              <Ionicons name={focused ? "time" : "time-outline"} size={26} color={color} />
            </View>
          ),
        }}
      />

      {/* 3. Chat AI (Không lồi nữa, ngang hàng) */}
      <Tabs.Screen
        name="chatAI"
        options={{
          tabBarIcon: ({ focused, color }) => (
            <View style={focused ? styles.activeIconWrapper : styles.iconWrapper}>
              <Ionicons name={focused ? "chatbubbles" : "chatbubbles-outline"} size={26} color={color} />
            </View>
          ),
        }}
      />

      {/* 4. Ngân sách */}
      <Tabs.Screen
        name="budget"
        options={{
          tabBarIcon: ({ focused, color }) => (
            <View style={focused ? styles.activeIconWrapper : styles.iconWrapper}>
              <Ionicons name={focused ? "wallet" : "wallet-outline"} size={26} color={color} />
            </View>
          ),
        }}
      />

      {/* 5. Tài khoản */}
      <Tabs.Screen
        name="profile"
        options={{
          tabBarIcon: ({ focused, color }) => (
            <View style={focused ? styles.activeIconWrapper : styles.iconWrapper}>
              <Ionicons name={focused ? "person" : "person-outline"} size={26} color={color} />
            </View>
          ),
        }}
      />
    </Tabs>
  );
}

const styles = StyleSheet.create({
  tabBar: {
    position: 'absolute',
    // Căn lề 2 bên tự động thay vì dùng Dimensions để tránh lỗi trên các màn hình kích thước dị
    left: 20,
    right: 20,
    bottom: Platform.OS === 'ios' ? 30 : 20,
    height: 65, // Chiều cao lý tưởng cho tab bar lơ lửng
    borderRadius: 35,
    backgroundColor: 'rgba(255, 255, 255, 0.55)', // Màu trắng hơi trong suốt
    borderTopWidth: 0,
    borderTopColor: 'transparent',
    elevation: 0, // Chỉnh shadow bằng shadowColor cho cả 2 nền tảng
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.1,
    shadowRadius: 15,
  },
  blurContainer: {
    flex: 1,
    borderRadius: 35,
    overflow: 'hidden',
  },
  iconWrapper: {
    justifyContent: 'center',
    alignItems: 'center',
    width: 50,
    height: 50,
  },
  // Hiệu ứng bao quanh Icon khi được chọn (rất phổ biến trên iOS hiện nay)
  activeIconWrapper: {
    justifyContent: 'center',
    alignItems: 'center',
    width: 46,
    height: 46,
    borderRadius: 23,
    backgroundColor: 'rgba(26, 35, 126, 0.1)', // Một lớp nền xanh siêu nhạt ôm lấy Icon
  }
});