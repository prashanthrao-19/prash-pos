package com.zosh.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatHelpResponse {
    private String reply;
    private String model;
    private boolean success;
    private String error;
    private String errorCode;
    private boolean configured;
    private List<String> suggestedActions;
}
