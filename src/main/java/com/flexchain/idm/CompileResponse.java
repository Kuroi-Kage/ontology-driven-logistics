package com.flexchain.idm;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CompileResponse {

    private String networkName;
    private int warehouseCount;
    private int truckCount;
    private int orderCount;
    private String generatedClassName;
    private String generatedJavaSource;
}
