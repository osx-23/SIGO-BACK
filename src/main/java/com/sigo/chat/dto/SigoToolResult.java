package com.sigo.chat.dto;

import java.util.List;
import java.util.Map;

public record SigoToolResult(
        String titulo,
        List<Map<String, Object>> filas,
        String nota
) {}
