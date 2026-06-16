package com.ticketflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hall")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hall_id")
    private Long hallId;

    @Column(name = "hall_name", length = 1000, nullable = false)
    private String hallName;

    @OneToMany(mappedBy = "hall", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Concert> concerts = new ArrayList<>();
}
