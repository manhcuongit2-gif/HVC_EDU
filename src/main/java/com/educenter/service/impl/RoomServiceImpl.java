package com.educenter.service.impl;

import com.educenter.entity.Room;
import com.educenter.repository.RoomRepository;
import com.educenter.service.RoomService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;

    public RoomServiceImpl(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    public List<Room> findAll() {
        return roomRepository.findAllByOrderByIdAsc();
    }

    @Override
    public List<Room> findAllActive() {
        return roomRepository.findByActiveTrue();
    }

    @Override
    public Optional<Room> findById(Long id) {
        return roomRepository.findById(id);
    }

    @Override
    public Room save(Room room) {
        return roomRepository.save(room);
    }

    @Override
    public void toggleActive(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng học với ID: " + id));
        room.setActive(!room.getActive());
        roomRepository.save(room);
    }

    @Override
    public boolean existsByName(String name) {
        return roomRepository.existsByName(name);
    }
}
