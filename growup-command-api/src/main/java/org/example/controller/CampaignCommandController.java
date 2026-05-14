package org.example.controller;

import lombok.AllArgsConstructor;
import org.example.facade.CampaignDeleteFacade;
import org.example.common.CommonResponse;
import org.example.dto.CampaignDeleteDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/command/campaign")
@RestController
@AllArgsConstructor
public class CampaignCommandController {

    private final CampaignDeleteFacade campaignDeleteFacade;

    @DeleteMapping("/delete/data")
    public ResponseEntity<CommonResponse<?>> deleteCampaignData(@RequestBody CampaignDeleteDto campaignDeleteDto){
        campaignDeleteFacade.deleteCampaignDataByPeriod(campaignDeleteDto);
        return new ResponseEntity<>(CommonResponse.builder("delete success")
                .data("delete success")
                .build(), HttpStatus.OK);
    }

    @DeleteMapping("/delete/data/pub")
    public ResponseEntity<CommonResponse<?>> deleteCampaignDataByPeriodPublishRedis(@RequestBody CampaignDeleteDto campaignDeleteDto){
        campaignDeleteFacade.deleteCampaignDataByPeriodPublishRedis(campaignDeleteDto);
        return new ResponseEntity<>(CommonResponse.builder("delete success")
                .data("delete success")
                .build(), HttpStatus.OK);
    }
}
