package com.wandile.skillswap.repository;

import com.wandile.skillswap.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByPost_User_Id(Long userId);
    List<Match> findByResponder_Id(Long userId);
}