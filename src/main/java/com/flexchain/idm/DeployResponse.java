package com.flexchain.idm;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeployResponse {

    private String networkName;
    private int warehousesCreated;
    private int trucksCreated;
    private int ordersCreated;
    private String message;
}
