import React, { createContext, useState, useContext } from 'react';

// 1. TẠO 2 BẢNG MÀU CỐ ĐỊNH Ở ĐÂY (Chỉ định nghĩa 1 lần duy nhất)
export const lightColors = {
    background: '#FBF8FF',
    surface: '#ffffff',
    text: '#1b1b21',
    textDim: '#767683',
    primary: '#1a237e',
    iconBg: '#e0e0ff',
    divider: '#f5f2fb',
};

export const darkColors = {
    background: '#121212',
    surface: '#1E1E1E',
    text: '#ffffff',
    textDim: '#A0A0A0',
    primary: '#8c9eff',
    iconBg: '#2C2C3E',
    divider: '#2A2A2A',
};

// Định nghĩa kiểu dữ liệu cho bảng màu
export type ColorsType = typeof lightColors;

interface ThemeContextProps {
    isDark: boolean;
    toggleTheme: () => void;
    colors: ColorsType; // <--- Thêm cái này để truyền bảng màu đi
}

const ThemeContext = createContext<ThemeContextProps | undefined>(undefined);

export const ThemeProvider = ({ children }: { children: React.ReactNode }) => {
    const [isDark, setIsDark] = useState(false);

    const toggleTheme = () => setIsDark(prev => !prev);

    // 2. TỰ ĐỘNG CHỌN BẢNG MÀU
    const colors = isDark ? darkColors : lightColors;

    return (

        <ThemeContext.Provider value={{ isDark, toggleTheme, colors }}>
            {children}
        </ThemeContext.Provider>
    );
};

export const useTheme = () => {
    const context = useContext(ThemeContext);
    if (!context) throw new Error("useTheme must be used within a ThemeProvider");
    return context;
};