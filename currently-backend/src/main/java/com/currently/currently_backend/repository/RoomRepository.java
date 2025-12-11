package com.currently.currently_backend.repository;

import com.currently.currently_backend.model.Room;
import com.currently.currently_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByUserOrderByFloorLabelAscNameAsc(User user);
}
