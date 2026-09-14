package com.wandile.skillswap.repository;

import com.wandile.skillswap.model.Post;
import com.wandile.skillswap.model.PostType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByType(PostType type);
    List<Post> findAllByOrderByCreatedAtDesc();
}