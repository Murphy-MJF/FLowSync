package hgc.flowsyncapi.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiTaskSuggestionRequest {
    private String projectName;
    private String title;
    private String description;
}
