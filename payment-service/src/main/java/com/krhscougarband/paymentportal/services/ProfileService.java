package com.krhscougarband.paymentportal.services;

import com.krhscougarband.paymentportal.entities.UserProfile;
import com.krhscougarband.paymentportal.repositories.UserProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {
    private final UserProfileRepository userProfileRepository;

    public ProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public UserProfile getByEmail(String email) {
        return userProfileRepository.findByUserEmail(email).orElse(null);
    }

    public UserProfile upsertProfile(String userId, String userEmail, String instrument) {
        UserProfile profile = userProfileRepository.findByUserEmail(userEmail)
            .orElseGet(UserProfile::new);
        profile.setUserId(userId);
        profile.setUserEmail(userEmail);
        profile.setInstrument(instrument);
        return userProfileRepository.save(profile);
    }
}
