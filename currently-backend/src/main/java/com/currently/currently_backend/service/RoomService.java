package com.currently.currently_backend.service;

import com.currently.currently_backend.dto.RoomRequest;
import com.currently.currently_backend.dto.RoomResponse;
import com.currently.currently_backend.model.Room;
import com.currently.currently_backend.model.User;
import com.currently.currently_backend.repository.RoomRepository;
import com.currently.currently_backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final UserLookupHashService userLookupHashService;

    public RoomService(
            RoomRepository roomRepository,
            UserRepository userRepository,
            UserLookupHashService userLookupHashService
    ) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.userLookupHashService = userLookupHashService;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailOrUsername = auth.getName();
        return userRepository.findByEmailHash(userLookupHashService.emailHash(emailOrUsername))
                
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    public List<RoomResponse> getRoomsForCurrentUser() {
        User user = getCurrentUser();
        List<Room> rooms = roomRepository.findByUser(user);
        return rooms.stream()
                .sorted(Comparator.comparing(Room::getFloorLabel, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Room::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public RoomResponse createRoom(RoomRequest request) {
        User user = getCurrentUser();
        if (request == null) {
            throw new IllegalArgumentException("Room request is required.");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Room name is required.");
        }
        if (request.getFloorLabel() == null || request.getFloorLabel().trim().isEmpty()) {
            throw new IllegalArgumentException("Floor label is required.");
        }

        Room room = new Room(
                user,
                request.getName().trim(),
                request.getFloorLabel().trim(),
                request.getType() != null && !request.getType().trim().isEmpty() ? request.getType().trim() : null
        );

        Room saved = roomRepository.save(room);
        return toResponse(saved);
    }

    public RoomResponse updateRoom(Long id, RoomRequest request) {
        User user = getCurrentUser();
        if (request == null) {
            throw new IllegalArgumentException("Room request is required.");
        }

        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (!room.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("You are not allowed to modify this room.");
        }

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Room name cannot be blank.");
            }
            room.setName(name);
        }
        if (request.getFloorLabel() != null) {
            String floorLabel = request.getFloorLabel().trim();
            if (floorLabel.isEmpty()) {
                throw new IllegalArgumentException("Floor label cannot be blank.");
            }
            room.setFloorLabel(floorLabel);
        }
        if (request.getType() != null) {
            String type = request.getType().trim();
            room.setType(type.isEmpty() ? null : type);
        }

        Room updated = roomRepository.save(room);
        return toResponse(updated);
    }

    public void deleteRoom(Long id) {
        User user = getCurrentUser();

        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (!room.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("You are not allowed to delete this room.");
        }

        roomRepository.delete(room);
    }

    private RoomResponse toResponse(Room room) {
        RoomResponse res = new RoomResponse();
        res.setId(room.getId());
        res.setName(room.getName());
        res.setFloorLabel(room.getFloorLabel());
        res.setType(room.getType());
        return res;
    }
}

