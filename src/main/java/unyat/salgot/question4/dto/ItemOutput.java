package unyat.salgot.question4.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Getter
@Setter
@RequiredArgsConstructor
@ToString
public class ItemOutput {

    @JsonIgnore
    @NonNull
    private int order;

    @NonNull
    private String id;

    @NonNull
    private String name;

    @NonNull
    private String passwordHash;

}
