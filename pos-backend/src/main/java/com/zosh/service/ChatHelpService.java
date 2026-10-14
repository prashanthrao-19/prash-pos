package com.zosh.service;

import com.zosh.modal.User;
import com.zosh.payload.request.ChatHelpRequest;
import com.zosh.payload.response.ChatHelpResponse;
import com.zosh.payload.response.ChatStatusResponse;

public interface ChatHelpService {

    ChatHelpResponse getAiHelp(ChatHelpRequest request, User authenticatedUser);

    ChatStatusResponse getChatStatus();
}
