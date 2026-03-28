package com.currently.currently_backend;

import com.currently.currently_backend.model.Room;
import com.currently.currently_backend.model.User;
import com.currently.currently_backend.model.UserAppliance;
import com.currently.currently_backend.repository.RoomRepository;
import com.currently.currently_backend.repository.UserApplianceRepository;
import com.currently.currently_backend.repository.UserRepository;
import com.currently.currently_backend.util.DataProtectionUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class PersistenceCrudTests {

    private static final String TEST_ENCRYPTION_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String TEST_HASH_KEY = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXowMTIzNDU2Nzg5MDE=";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserApplianceRepository userApplianceRepository;

    @BeforeAll
    static void configureDataProtection() {
        DataProtectionUtil.configure(TEST_ENCRYPTION_KEY, TEST_HASH_KEY);
    }

    // Verifies a room can be created, queried, and deleted through the JPA repositories.
    @Test
    void createReadAndDeleteRoomForUser() {
        User user = createUser("owner-room");
        userRepository.saveAndFlush(user);

        Room room = new Room(user, "Kitchen", "Ground", "Kitchen");
        Room savedRoom = roomRepository.saveAndFlush(room);

        List<Room> rooms = roomRepository.findByUser(user);
        assertThat(rooms).hasSize(1);
        assertThat(rooms.get(0).getId()).isEqualTo(savedRoom.getId());

        roomRepository.delete(savedRoom);
        roomRepository.flush();

        assertThat(roomRepository.findByUser(user)).isEmpty();
    }

    // Verifies the database rejects duplicate users that reuse the same email hash value.
    @Test
    void userEmailUniquenessConstraintIsEnforced() {
        User first = createUser("dup");
        first.setEmailHash("same-hash");
        userRepository.saveAndFlush(first);

        User duplicate = createUser("dup");
        duplicate.setEmailHash("same-hash");
        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Verifies an appliance row cannot be persisted without the required owning user.
    @Test
    void cannotPersistApplianceWithoutOwner() {
        UserAppliance orphan = new UserAppliance();
        orphan.setApplianceName("Fridge");
        orphan.setUsageType("continuous");
        orphan.setHoursPerDay(8.0);
        orphan.setRoom(null);
        orphan.setUser(null);

        assertThatThrownBy(() -> userApplianceRepository.saveAndFlush(orphan))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // Verifies relational integrity prevents deleting a room that is still referenced.
    @Test
    void cannotDeleteRoomWithExistingApplianceReference() {
        User user = createUser("owner-delete");
        userRepository.saveAndFlush(user);

        Room room = roomRepository.saveAndFlush(new Room(user, "Utility", "Ground", "Utility"));

        UserAppliance appliance = new UserAppliance();
        appliance.setUser(user);
        appliance.setRoom(room);
        appliance.setApplianceName("Fridge");
        appliance.setUsageType("continuous");
        appliance.setHoursPerDay(12.0);
        userApplianceRepository.saveAndFlush(appliance);

        roomRepository.delete(room);
        assertThatThrownBy(() -> roomRepository.flush())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User createUser(String prefix) {
        User user = new User();
        user.setUsername(prefix + "-" + System.nanoTime());
        user.setName("Integration " + prefix);
        user.setEmail(prefix + "@example.com");
        user.setPassword("Password123!");
        return user;
    }
}
