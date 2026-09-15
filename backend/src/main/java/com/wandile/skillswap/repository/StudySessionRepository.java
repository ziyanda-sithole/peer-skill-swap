package com.wandile.skillswap.repository;

import com.wandile.skillswap.model.SessionVisibility;
import com.wandile.skillswap.model.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
    List<StudySession> findByVisibilityOrderByCreatedAtDesc(SessionVisibility visibility);
}