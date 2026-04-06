import { Tabs } from 'expo-router';
import { View, Text, Platform, Dimensions, StyleSheet } from 'react-native';
import { BlurView } from 'expo-blur';
import Ionicons from '@expo/vector-icons/build/Ionicons';
// Hàm hỗ trợ vẽ Icon (Tạm thời dùng chữ, sau này bạn sẽ thay bằng thư viện Icon thật)
const TabIcon = ({ name, color }: { name: string, color: string }) => (
  <View style={{ alignItems: 'center', justifyContent: 'center' }}>
    <Text style={{ color: color, fontSize: 20 }}>{name}</Text>
  </View>
);

export default function TabLayout() {
  return (
    <Tabs
      screenOptions={{
        headerShown: false, // Ẩn thanh tiêu đề mặc định phía trên
        tabBarShowLabel: true,
        tabBarStyle: styles.tabBar,
        tabBarActiveTintColor: '#1A237E',
        tabBarInactiveTintColor: '#767683',
        tabBarBackground: () => (
          <BlurView intensity={5} style={[
            StyleSheet.absoluteFill,
            { borderRadius: 40, overflow: 'hidden' }
          ]} tint="light" />
        ),
      }}>
      <Tabs.Screen
        name="index"
        options={{
          tabBarIcon: ({ focused }) => (
            <View style={focused ? styles.activeTab : null}>
              <Ionicons name={focused ? "map" : "map-outline"} size={24} color={focused ? '#ffffff' : 'gray'} />
            </View>
          ),
        }}
      />
      <Tabs.Screen
        name="history"
        options={{
          title: 'History',
          tabBarIcon: ({ focused }) =>
            <View style={focused ? styles.activeTab : null}>
              <Ionicons name={focused ? "map" : "map-outline"} size={24} color={focused ? '#ffffff' : 'gray'} />
            </View>
        }}
      />
      <Tabs.Screen
        name="chatAI"
        options={{
          title: 'Chat AI',
          tabBarIcon: ({ color }) => <TabIcon name="🤖" color={color} />,
        }}
      />
      <Tabs.Screen
        name="budget"
        options={{
          title: 'Budget',
          tabBarIcon: ({ focused }) => (
            <View style={focused ? styles.activeTab : null}>
              <Ionicons name={focused ? "map" : "map-outline"} size={24} color={focused ? '#ffffff' : 'gray'} />
            </View>
          ),
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          title: 'Profile',
          tabBarIcon: ({ color }) => <TabIcon name="👤" color={color} />,
        }}
      />
    </Tabs>
  );
}
const styles = StyleSheet.create({
  tabBar: {
    position: 'absolute',
    width: Dimensions.get('window').width * 0.88,
    marginLeft: Dimensions.get('window').width * 0.06,
    marginRight: Dimensions.get('window').width * 0.06,
    bottom: 30,
    height: 50,
    borderRadius: 40,
    backgroundColor: 'rgba(207, 204, 204, 0.7)', // Nền trắng trong suốt nhẹ
    borderTopWidth: 0,
    borderBottomWidth: 0, // Xóa đường kẻ ngang
    marginBottom: 0,
    // Đổ bóng kiểu iOS
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 40 },
    shadowOpacity: 0.1,
    shadowRadius: 20,
    elevation: 10, // Đổ bóng kiểu Android
    paddingBottom: Platform.OS === 'ios' ? 0 : 0, // Tùy chỉnh cho Android/iOS
  },
  activeTab: {
    // Nền xanh nhạt khi focus
    position: 'relative',
    paddingVertical: 8,
    borderRadius: 25,
    marginTop: 12,
    height: 50,
    width: 70,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(138, 138, 138, 0.8)',
    //backgroundColor: 'rgba(59, 191, 178, 0.8)',
  }
}
);
