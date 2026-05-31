package ru.practicum.ewm.analyzer.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Table(name = "user_actions")
@Entity
public class UserAction {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    private Long eventId;

    private Long userId;

    private Double weight;

    private Instant timestamp;
}
