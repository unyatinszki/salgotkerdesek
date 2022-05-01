package unyat.salgot.repository.question3;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import unyat.salgot.dao.question3.Folder;
import unyat.salgot.dao.question3.Item;
import unyat.salgot.dao.question3.Label;
import unyat.salgot.dao.question3.Property;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
public class FolderRepositoryTest {

    @Autowired private FolderRepository folderRepository;

    @Autowired private LabelRepository labelRepository;

    private Folder createFolder(){
        Folder f1 = new Folder();

        Item i1 = new Item(f1);
        Item i2 = new Item(f1);

        Property p1 = new Property(i1);
        Property p2 = new Property(i1);
        Property p3 = new Property(i2);
        Property p4 = new Property(i2);

        return folderRepository.saveAndFlush(f1);
    }



    @Test
    public void testStoringFolderStructure(){
        Folder folder1 = createFolder();
        checkGeneratedFolderIdsAndInitialVersion(folder1);
        Folder folder2 = createFolder();
        checkGeneratedFolderIdsAndInitialVersion(folder2);
    }

    @Test
    public void testAddingLabels(){
        Folder folder = createFolder();
        Label l1 = new Label();
        Label l2 = new Label();
        labelRepository.saveAllAndFlush(Arrays.asList(l1, l2));
        // every adding of a Label should increase the version of the Item
        addLabelToEveryItem(folder, l1);
        folderRepository.saveAndFlush(folder);
        checkItemVersions(folder, 1);
        addLabelToEveryItem(folder, l2);
        folderRepository.saveAndFlush(folder);
        checkItemVersions(folder, 2);
        folder.getItems().forEach(i -> {
            i.removeLabel(l2);
        });
        folderRepository.saveAndFlush(folder);
        checkItemVersions(folder, 3);
        var folders = folderRepository.loadAllFolders();
    }

    @Test
    public void testDeletingFolder(){
        Folder folder = createFolder();
        Label l1 = new Label();
        Label l2 = new Label();
        labelRepository.saveAllAndFlush(Arrays.asList(l1, l2));
        // every adding of a Label should increase the version of the Item
        addLabelToEveryItem(folder, l1);
        folderRepository.saveAndFlush(folder);
        folderRepository.delete(folder);
        folderRepository.flush();
    }

    private void checkGeneratedFolderIdsAndInitialVersion(Folder folder){
        assertNotNull(folder.getId());
        assertEquals(0, folder.getVersion());
        assertEquals(2, folder.getItems().size());
        folder.getItems().forEach(i -> {
            assertNotNull(i.getId());
            assertEquals(0, i.getVersion());
            assertEquals(2, i.getProperties().size());
            i.getProperties().forEach(p -> {
                assertNotNull(p.getId());
                assertEquals(0, p.getVersion());
            });
        });
    }

    private void addLabelToEveryItem(Folder folder, Label l1) {
        folder.getItems().forEach(i -> {
            i.addLabel(l1);
        });
    }

    private void checkItemVersions(Folder folder, int expected) {
        folder.getItems().forEach(i -> {
            assertEquals(expected, i.getVersion());
        });
    }
}
