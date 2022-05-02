package unyat.salgot.question4.dao;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "HASHED_ITEM")
@Getter
@Setter
@NoArgsConstructor
public class HashedItem {

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "PASSWORD_HASH")
    private String passwordHash;

}
