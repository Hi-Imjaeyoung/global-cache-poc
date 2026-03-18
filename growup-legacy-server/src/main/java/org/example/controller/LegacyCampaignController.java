package org.example.controller;

import lombok.AllArgsConstructor;
import org.example.common.CommonResponse;
import org.example.dto.CampaignDeleteDto;
import org.example.dto.CampaignTotalDataRequestDto;
import org.example.dto.TotalCampaignsData;
import org.example.facade.LegacyCampaignDeleteFacade;
import org.example.facade.LegacyCampaignTotalDataFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/legacy/campaign")
@RestController
@AllArgsConstructor
public class LegacyCampaignController {

    private LegacyCampaignTotalDataFacade campaignTotalDataFacade;
    private LegacyCampaignDeleteFacade campaignDeleteFacade;
    @GetMapping("/get/totalData")
    public ResponseEntity<CommonResponse<TotalCampaignsData>> campaignTotalAnalysis4(@ModelAttribute CampaignTotalDataRequestDto campaignTotalDataRequestDto){
        TotalCampaignsData totalCampaignsData =
                campaignTotalDataFacade.getCampaignTotalDataByLazyLoadingTree(campaignTotalDataRequestDto.getEmail(), campaignTotalDataRequestDto.getStart(),campaignTotalDataRequestDto.getEnd());
        return new ResponseEntity<>(CommonResponse.<TotalCampaignsData>builder("success get")
                .data(totalCampaignsData)
                .build(), HttpStatus.OK);
    }
    @DeleteMapping("/delete/data")
    public ResponseEntity<CommonResponse<?>> deleteCampaignData(@RequestBody CampaignDeleteDto campaignDeleteDto){
        campaignDeleteFacade.deleteCampaignDataByPeriod(campaignDeleteDto);
        return new ResponseEntity<>(CommonResponse.builder("delete success")
                .data("delete success")
                .build(), HttpStatus.OK);
    }
}
