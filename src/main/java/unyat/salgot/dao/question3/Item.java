package unyat.salgot.dao.question3;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ITEM")
@Getter
@Setter
@NoArgsConstructor
public class Item {

    public Item(Folder folder){
        this.folder = folder;
        folder.getItems().add(this);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Version
    @Column(name = "VERSION_ID")
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FOLDER_ID", nullable = false)
    private Folder folder;

    @OneToMany(mappedBy = "item", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Fetch(FetchMode.JOIN)
    private Set<Property> properties = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "ITEM_LABEL",
            joinColumns = @JoinColumn(name = "ITEM_ID"),
            inverseJoinColumns = @JoinColumn(name = "LABEL_ID"))
    private Set<Label> labels = new HashSet<>();

    public void addLabel(Label label){
        labels.add(label);
        label.getItems().add(this);
    }

    public void removeLabel(Label label){
        labels.remove(label);
        label.getItems().remove(label);
    }

}
