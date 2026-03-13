package org.example.repo;

import org.example.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberReop extends JpaRepository<Member,Long> {
}
