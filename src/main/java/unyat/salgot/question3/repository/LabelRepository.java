package unyat.salgot.question3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import unyat.salgot.question3.dao.Label;

public interface LabelRepository extends JpaRepository<Label, Long> {
}