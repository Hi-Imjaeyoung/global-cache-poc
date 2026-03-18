package org.example.dto;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CampaignTotalDataRequestDto {
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    LocalDate start;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    LocalDate end;
    String email;
}
