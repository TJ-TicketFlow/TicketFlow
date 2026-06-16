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
// SQL에서 직접 1번을 넣기 위해 @GeneratedValue를 제거하거나
// 직접 할당이 가능하도록 설정해야 합니다.
    @Column(name = "hall_id")
    private Long hallId;

    @Column(name = "hall_name", length = 1000, nullable = false)
    private String hallName;

    // SQL에 있는 hall_address 필드 추가
    @Column(name = "hall_address", length = 1000)
    private String hallAddress;

    @OneToMany(mappedBy = "hall", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Concert> concerts = new ArrayList<>();
}