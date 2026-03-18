package org.example.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.CommonResponse;
import org.example.dto.CampaignTotalDataRequestDto;
import org.example.dto.TotalCampaignsData;
import org.example.facade.CampaignTotalDataFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RequestMapping("/api/query/campaign")
@RestController
@AllArgsConstructor
public class CampaignController {

    private final CampaignTotalDataFacade campaignTotalDataFacade;

    @GetMapping("/totalAnalysisData")
    public ResponseEntity<CommonResponse<TotalCampaignsData>> campaignTotalAnalysis4(@ModelAttribute CampaignTotalDataRequestDto campaignTotalDataRequestDto){
        TotalCampaignsData totalCampaignsData =
                campaignTotalDataFacade.getCampaignTotalDataByLazyLoadingTree(campaignTotalDataRequestDto.getEmail(),campaignTotalDataRequestDto.getStart(),campaignTotalDataRequestDto.getEnd());
        return new ResponseEntity<>(CommonResponse.<TotalCampaignsData>builder("success get")
                .data(totalCampaignsData)
                .build(),HttpStatus.OK);
    }
}
