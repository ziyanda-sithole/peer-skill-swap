package com.wandile.skillswap.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "session_memberships", uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "user_id"}))
public class SessionMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id")
    private StudySession session;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected SessionMembership() {}

    public SessionMembership(StudySession session, User user, MembershipRole role, MembershipStatus status) {
        this.session = session;
        this.user = user;
        this.role = role;
        this.status = status;
    }

    public Long getId() { return id; }
    public StudySession getSession() { return session; }
    public User getUser() { return user; }
    public MembershipRole getRole() { return role; }
    public void setRole(MembershipRole role) { this.role = role; }
    public MembershipStatus getStatus() { return status; }
    public void setStatus(MembershipStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}