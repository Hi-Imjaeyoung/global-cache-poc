package org.example.dto;

import java.time.LocalDate;
import java.util.Map;

public record TreeUpdateEvent(
        String memberEmail,
        int year,
        Map<LocalDate, AllCampaignTypeData> deltaData
) {}
