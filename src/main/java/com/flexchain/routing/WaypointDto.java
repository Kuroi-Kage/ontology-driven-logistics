package com.flexchain.routing;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaypointDto {
    private double latitude;
    private double longitude;
    private String label;
}
