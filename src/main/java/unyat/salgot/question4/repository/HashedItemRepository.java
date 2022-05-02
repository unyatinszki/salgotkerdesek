package unyat.salgot.question4.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import unyat.salgot.question4.dao.HashedItem;

public interface HashedItemRepository extends JpaRepository<HashedItem, String> {
}