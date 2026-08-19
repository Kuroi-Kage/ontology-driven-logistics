package com.flexchain.dto.dashboard;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MissionResponseDto {
    private String agentName;
    private String message;
    private String timestamp;
}