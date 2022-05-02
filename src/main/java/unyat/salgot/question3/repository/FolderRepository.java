package unyat.salgot.question3.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import unyat.salgot.question3.dao.Folder;

import java.util.Collection;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    @Query("SELECT DISTINCT f FROM Folder f JOIN FETCH f.items i JOIN FETCH i.properties")
    Collection<Folder> loadAllFolders();

}