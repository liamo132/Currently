package com.currently.currently_backend.repository;

import com.currently.currently_backend.model.Room;
import com.currently.currently_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/*
 * Repository: RoomRepository
 * Purpose: Database access for Map My House rooms owned by a user.
 */
public interface RoomRepository extends JpaRepository<Room, Long> {

    // Backend Query: returns all Room entities belonging to the authenticated user.
    List<Room> findByUser(User user);
}
