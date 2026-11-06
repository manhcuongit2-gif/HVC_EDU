package com.educenter.repository;

import com.educenter.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByName(String name);
    boolean existsByName(String name);
    List<Room> findByActiveTrue();
    List<Room> findAllByOrderByIdAsc();
}
