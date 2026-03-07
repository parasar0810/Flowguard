package com.flowguard.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer requests;

    @Column(name = "time_window_seconds", nullable = false)
    private Integer timeWindowSeconds;
}
