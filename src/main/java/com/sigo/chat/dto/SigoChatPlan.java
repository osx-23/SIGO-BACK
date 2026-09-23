package com.sigo.chat.dto;

import java.util.List;

public record SigoChatPlan(List<SigoToolRequest> tools) {
    public List<SigoToolRequest> toolsSeguras() {
        return tools == null ? List.of() : tools;
    }
}
