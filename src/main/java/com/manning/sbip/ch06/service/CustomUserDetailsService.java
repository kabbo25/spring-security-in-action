package com.manning.sbip.ch06.service;

import com.manning.sbip.ch06.entity.ApplicationUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        ApplicationUser applicationUser = userService.findByUsername(username);

        if (applicationUser == null) {
            throw new UsernameNotFoundException("User with username " + username + " does not exist");
        }

        UserDetails userDetails = User.withUsername(username)
                .password(applicationUser.getPassword())
                .roles("USER")
                .disabled(!applicationUser.isVerified())
                .build();

        return userDetails;
    }
}
