package unyat.salgot.question4.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@ToString
public class ItemInput {

    @JsonIgnore
    private int order;

    @NonNull
    private String name;

    @NonNull
    private String password;

}
