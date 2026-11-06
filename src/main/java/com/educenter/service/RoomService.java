package com.educenter.service;

import com.educenter.entity.Room;

import java.util.List;
import java.util.Optional;

public interface RoomService {
    List<Room> findAll();
    List<Room> findAllActive();
    Optional<Room> findById(Long id);
    Room save(Room room);
    void toggleActive(Long id);
    boolean existsByName(String name);
}
