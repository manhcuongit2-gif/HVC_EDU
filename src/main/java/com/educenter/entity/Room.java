package com.educenter.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 50, unique = true, nullable = false)
    @NotBlank(message = "Tên phòng học không được để trống")
    @Size(max = 50, message = "Tên phòng học tối đa 50 ký tự")
    private String name;

    @Column(name = "capacity", nullable = false)
    @NotNull(message = "Sức chứa phòng học không được để trống")
    @Min(value = 1, message = "Sức chứa tối thiểu phải là 1 học sinh")
    private Integer capacity;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public Room() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
