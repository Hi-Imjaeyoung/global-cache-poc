package org.example.listener;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.dto.AllCampaignTypeData;

import java.time.LocalDate;
import java.util.Map;

@Getter
@AllArgsConstructor
public class TreeUpdateEvent {
    private final String email;
    private final Map<LocalDate, AllCampaignTypeData> preData;
}
