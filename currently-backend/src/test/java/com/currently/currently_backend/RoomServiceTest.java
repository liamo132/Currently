package com.currently.currently_backend;

import com.currently.currently_backend.dto.RoomRequest;
import com.currently.currently_backend.dto.RoomResponse;
import com.currently.currently_backend.model.Room;
import com.currently.currently_backend.model.User;
import com.currently.currently_backend.repository.RoomRepository;
import com.currently.currently_backend.repository.UserRepository;
import com.currently.currently_backend.service.RoomService;
import com.currently.currently_backend.service.UserLookupHashService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserLookupHashService userLookupHashService;

    private RoomService roomService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        roomService = new RoomService(roomRepository, userRepository, userLookupHashService);

        currentUser = new User();
        currentUser.setId(100L);
        currentUser.setEmail("owner@example.com");

        authenticateAs("owner@example.com");
        when(userLookupHashService.emailHash(anyString())).thenReturn("hash-owner");
        when(userRepository.findByEmailHash("hash-owner")).thenReturn(Optional.of(currentUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Verifies room results are sorted consistently for the current user.
    @Test
    void returnsRoomsInFloorThenNameOrderIgnoringCase() {
        Room roomA = new Room(currentUser, "bedroom", "Second", "Bedroom");
        setEntityId(roomA, 1L);
        Room roomB = new Room(currentUser, "attic", "first", "Storage");
        setEntityId(roomB, 2L);

        when(roomRepository.findByUser(currentUser)).thenReturn(List.of(roomA, roomB));

        List<RoomResponse> result = roomService.getRoomsForCurrentUser();

        assertThat(result)
                .extracting(RoomResponse::getFloorLabel)
                .containsExactly("first", "Second");
    }

    // Verifies create rejects payloads that omit the required room name.
    @Test
    void createRoomRejectsMissingName() {
        RoomRequest request = new RoomRequest();
        request.setFloorLabel("Ground");

        assertThatThrownBy(() -> roomService.createRoom(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Room name is required.");
    }

    // Verifies a user cannot update a room owned by someone else.
    @Test
    void updateRoomRejectsWrongOwner() {
        User otherUser = new User();
        otherUser.setId(999L);
        Room room = new Room(otherUser, "Guest", "Floor", null);
        setEntityId(room, 20L);

        RoomRequest request = new RoomRequest();
        request.setName("Updated");

        when(roomRepository.findById(20L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.updateRoom(20L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You are not allowed to modify this room.");
    }

    // Verifies update rejects names that are present but blank after trimming.
    @Test
    void updateRoomRejectsBlankName() {
        Room room = new Room(currentUser, "Current", "Floor", null);
        setEntityId(room, 30L);

        RoomRequest request = new RoomRequest();
        request.setName("   ");

        when(roomRepository.findById(30L)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.updateRoom(30L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Room name cannot be blank.");
    }

    private void authenticateAs(String principal) {
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    private void setEntityId(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }
}
