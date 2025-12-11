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

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public RoomService(RoomRepository roomRepository, UserRepository userRepository) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String emailOrUsername = auth.getName();
        return userRepository.findByEmail(emailOrUsername)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
    }

    public List<RoomResponse> getRoomsForCurrentUser() {
        User user = getCurrentUser();
        List<Room> rooms = roomRepository.findByUserOrderByFloorLabelAscNameAsc(user);
        return rooms.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public RoomResponse createRoom(RoomRequest request) {
        User user = getCurrentUser();

        Room room = new Room(
                user,
                request.getName(),
                request.getFloorLabel(),
                request.getType()
        );

        Room saved = roomRepository.save(room);
        return toResponse(saved);
    }

    public RoomResponse updateRoom(Long id, RoomRequest request) {
        User user = getCurrentUser();

        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (!room.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("You are not allowed to modify this room.");
        }

        if (request.getName() != null) {
            room.setName(request.getName());
        }
        if (request.getFloorLabel() != null) {
            room.setFloorLabel(request.getFloorLabel());
        }
        if (request.getType() != null) {
            room.setType(request.getType());
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
