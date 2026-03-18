package org.example.repo;

import org.example.domain.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LegacyKeywordRepository extends JpaRepository <Keyword,Long>, LegacyKeywordRepositoryCustom {
}
