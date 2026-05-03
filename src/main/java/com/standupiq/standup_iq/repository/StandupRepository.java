package com.standupiq.standup_iq.repository;

import com.standupiq.standup_iq.entity.Standup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StandupRepository extends JpaRepository<Standup, Long> {
    List<Standup> findByUsernameOrderByGeneratedAtDesc(String username);
}
