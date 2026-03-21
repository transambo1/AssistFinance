package com.financeai.finance_management.service;

import com.financeai.finance_management.model.User;
import com.financeai.finance_management.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class UserService implements UserDetailsService { // Thêm implements ở đây

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User saveUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    // THÊM HÀM NÀY ĐỂ WEBSECURITYCONFIG KHÔNG BÁO LỖI
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Bây giờ findByUsername trả về Optional nên dùng orElseThrow là chuẩn bài!
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                new ArrayList<>()
        );
    }
}