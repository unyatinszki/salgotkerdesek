package unyat.salgot.question3.dao;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "LABEL")
@Getter
@Setter
@NoArgsConstructor
public class Label {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "labels")
    private Set<Item> items = new HashSet<>();

}
