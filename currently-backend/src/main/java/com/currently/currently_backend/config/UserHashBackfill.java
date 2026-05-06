package com.currently.currently_backend.config;

import com.currently.currently_backend.model.User;
import com.currently.currently_backend.repository.UserRepository;
import com.currently.currently_backend.service.UserLookupHashService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class UserHashBackfill {

    private final UserRepository userRepository;
    private final UserLookupHashService userLookupHashService;

    public UserHashBackfill(UserRepository userRepository, UserLookupHashService userLookupHashService) {
        this.userRepository = userRepository;
        this.userLookupHashService = userLookupHashService;
    }

    /*
     * Data migration helper: User hash backfill
     * Purpose: On startup, fills missing email/username HMAC hashes so encrypted user fields remain searchable.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillMissingHashes() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            boolean changed = false;

            if (user.getEmailHash() == null || user.getEmailHash().isBlank()) {
                user.setEmailHash(userLookupHashService.emailHash(user.getEmail()));
                changed = true;
            }

            if (user.getUsernameHash() == null || user.getUsernameHash().isBlank()) {
                user.setUsernameHash(userLookupHashService.usernameHash(user.getUsernameField()));
                changed = true;
            }

            if (changed) {
                userRepository.save(user);
            }
        }
    }
}
