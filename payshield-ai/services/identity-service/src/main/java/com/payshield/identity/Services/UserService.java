package com.payshield.identity.Services;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.payshield.identity.Entity.User;
import com.payshield.identity.Repository.UserRepository;

@Service 
public class UserService {

    private UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> getUserById(UUID userId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUserById'");
    }

    



    
}
