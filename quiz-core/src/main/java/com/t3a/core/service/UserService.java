package com.t3a.core.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.t3a.core.domain.entity.User;
import com.t3a.core.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 用户服务
 */
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserMapper userMapper;
    private final ApplicationContext applicationContext;  // Inject ApplicationContext to break circular dependency

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .eq(User::getDeleted, 0)
        );

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        if (user.getStatus() == 0) {
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }

    /**
     * 根据用户名查询用户
     */
    public User findByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .eq(User::getDeleted, 0)
        );
    }

    /**
     * 创建用户
     */
    public User createUser(User user) {
        // Get PasswordEncoder from ApplicationContext to break circular dependency
        PasswordEncoder passwordEncoder = applicationContext.getBean(PasswordEncoder.class);
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setDeleted(0);
        user.setStatus(1);
        user.setRole("USER");
        userMapper.insert(user);
        return user;
    }

    /**
     * 检查用户名是否存在
     */
    public boolean existsByUsername(String username) {
        return userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .eq(User::getDeleted, 0)
        ) > 0;
    }

    /**
     * 检查邮箱是否存在
     */
    public boolean existsByEmail(String email) {
        return userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, email)
                        .eq(User::getDeleted, 0)
        ) > 0;
    }
}
