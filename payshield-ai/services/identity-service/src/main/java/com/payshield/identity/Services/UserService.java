package com.payshield.identity.Services;

import org.springframework.stereotype.Service;

import com.payshield.identity.Repository.UserRepository;

@Service 
public class UserService {

    private UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    



    
}
