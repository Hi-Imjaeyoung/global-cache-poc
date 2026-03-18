package org.example.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignDeleteDto {
    LocalDate start;
    LocalDate end;
    String email;
    List<Long> campaignIds;

    public boolean checkThreshold(){
        if(start.getYear() == end.getYear()){
            long daysBetween = ChronoUnit.DAYS.between(start, end);
            return daysBetween <= 40;
        }
        return false;
    }
}
