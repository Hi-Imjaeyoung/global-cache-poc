package org.example.repo;

import org.example.domain.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KeywordRepository extends JpaRepository<Keyword,Long>,KeywordRepositoryCustom {
}
