package com.flexchain.idm;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompileRequest {

    @NotBlank(message = "Le code source .flexnet est obligatoire")
    private String source;
}
