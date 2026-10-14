package com.zosh.payload.request;

import com.zosh.payload.dto.ChatMessageDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatHelpRequest {

    @NotBlank(message = "Question or message cannot be empty")
    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    private String message;

    @Size(max = 200, message = "Route path is too long")
    private String currentRoute;

    @Size(max = 50, message = "User role is too long")
    private String userRole;

    @Builder.Default
    private List<ChatMessageDTO> history = new ArrayList<>();
}
