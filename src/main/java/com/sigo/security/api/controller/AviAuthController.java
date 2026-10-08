package com.sigo.security.api.controller;

import com.sigo.security.api.dto.AviLoginRequest;
import com.sigo.security.api.dto.AviLoginResponse;
import com.sigo.security.application.port.in.AutenticacionUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/avi/auth")
@RequiredArgsConstructor
public class AviAuthController {

    private final AutenticacionUseCase autenticacionUseCase;

    @PostMapping("/login")
    public AviLoginResponse login(
            @Valid @RequestBody AviLoginRequest request
    ) {
        AutenticacionUseCase.LoginResult result =
                autenticacionUseCase.loginAvi(
                        request.codigo()
                );

        return new AviLoginResponse(
                result.token(),
                result.tipo(),
                result.expiresIn(),
                new AviLoginResponse.Usuario(
                        result.usuario().trabajadorId(),
                        result.usuario().codigo(),
                        result.usuario().nombre(),
                        result.usuario().rol(),
                        result.usuario().plazaId(),
                        result.usuario().plaza()
                )
        );
    }
}
