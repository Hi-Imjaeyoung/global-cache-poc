package org.example.controller;

import lombok.AllArgsConstructor;
import org.example.service.MemberCommandService;
import org.example.dto.MemberRequestRecord;
import org.example.producer.TreeUpdateEventProducer;
import org.example.dto.AllCampaignTypeData;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RequestMapping("/api/command/member")
@RestController
@AllArgsConstructor
public class MemberController {

    private final MemberCommandService memberCommandService;
    private final TreeUpdateEventProducer treeUpdateEventProducer;

    @PostMapping("/save")
    public void saveMember(@RequestBody MemberRequestRecord memberRequestRecord){
        memberCommandService.save(memberRequestRecord);
    }

    @GetMapping("/test/event")
    public void eventPublishTest(@RequestParam("email") String email,
                                 @RequestParam("id") Long id){
        treeUpdateEventProducer.sendUpdateEvent(email,id,2026,Map.of(LocalDate.now(),new AllCampaignTypeData()));
    }
}
