package com.ticketflow.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "membership_history")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "membership_history_id")
    private Long membershipHistoryId;

    @Column(name = "action_type", length = 50, nullable = false)
    private String actionType;

    @Column(name = "previous_status", length = 20, nullable = false)
    private String previousStatus;

    @Column(name = "new_status", length = 20, nullable = false)
    private String newStatus;

    @Column(name = "history_note", columnDefinition = "TEXT")
    private String historyNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_id", nullable = false)
    private Membership membership;
}
