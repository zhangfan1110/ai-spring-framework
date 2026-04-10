package com.example.aiframework.common.security;

import com.example.aiframework.system.entity.UserEntity;
import com.example.aiframework.system.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义用户详情服务
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userMapper.findByUsername(username);
        
        if (userEntity == null) {
            throw new UsernameNotFoundException("用户不存在：" + username);
        }

        if (userEntity.getStatus() != 1) {
            throw new UsernameNotFoundException("用户已被禁用：" + username);
        }

        // 加载用户角色
        List<String> roleCodes = userMapper.findRoleCodesByUserId(userEntity.getId());
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        
        for (String roleCode : roleCodes) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode));
        }

        // 加载用户权限
        List<String> permissionCodes = userMapper.findPermissionCodesByUserId(userEntity.getId());
        for (String permissionCode : permissionCodes) {
            authorities.add(new SimpleGrantedAuthority(permissionCode));
        }

        return new User(
                userEntity.getUsername(),
                userEntity.getPassword(),
                authorities
        );
    }
}
