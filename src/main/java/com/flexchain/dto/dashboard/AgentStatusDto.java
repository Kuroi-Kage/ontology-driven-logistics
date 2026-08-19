package com.flexchain.dto.dashboard;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentStatusDto {
    private String name;
    private String status;
    private int progress;
}