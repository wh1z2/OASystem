package com.oasystem.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.oasystem.entity.User;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Spring Security UserDetails实现
 */
@Data
public class UserDetailsImpl implements UserDetails {

    private Long id;
    private String username;
    private String name;
    private String email;
    private String phone;
    private String avatar;
    private Long roleId;
    private String roleName;
    private String roleLabel;
    private Long deptId;
    private String deptName;
    private Integer status;

    @JsonIgnore
    private String password;

    private List<String> permissions;

    private Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(User user, String roleName, String deptName, List<String> permissions, Collection<? extends GrantedAuthority> authorities) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.name = user.getName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.avatar = user.getAvatar();
        this.roleId = user.getRoleId();
        this.roleName = roleName;
        this.deptId = user.getDeptId();
        this.deptName = deptName;
        this.status = user.getStatus();
        this.password = user.getPassword();
        this.permissions = permissions != null ? permissions : Collections.emptyList();
        this.authorities = authorities;
    }

    public static UserDetailsImpl build(User user, String roleName, String deptName, List<String> permissions) {
        List<SimpleGrantedAuthority> authorities = permissions != null ? permissions.stream()
                .filter(permission -> permission != null && !permission.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .toList() : Collections.emptyList();
        return new UserDetailsImpl(user, roleName, deptName, permissions, authorities);
    }

    public static UserDetailsImpl build(User user, String roleName, String deptName) {
        return build(user, roleName, deptName, Collections.emptyList());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status != null && status == 1;
    }
}
