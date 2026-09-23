package com.sigo.chat.application.port.out;

import com.sigo.chat.domain.SigoToolRequest;
import com.sigo.chat.domain.SigoToolResult;

public interface SigoQueryPort {

    SigoToolResult ejecutar(
            SigoToolRequest request
    );
}
