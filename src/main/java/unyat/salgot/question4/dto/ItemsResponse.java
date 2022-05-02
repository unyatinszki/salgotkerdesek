package unyat.salgot.question4.dto;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
public class ItemsResponse {

    @NonNull
    private List<ItemOutput> items;

}
