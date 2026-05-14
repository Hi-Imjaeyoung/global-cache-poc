package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CampaignUpdateEvent {
    String email;
    int year;
    int updateNumber;
    Map<LocalDate, AllCampaignTypeData> data;
}
