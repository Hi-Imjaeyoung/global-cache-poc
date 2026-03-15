package org.example.controller;

import lombok.AllArgsConstructor;
import org.example.MemberCommandService;
import org.example.MemberRequestRecord;
import org.example.TreeUpdateEventProducer;
import org.example.dto.AllCampaignTypeData;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
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
    public void eventPublishTest(@RequestParam("email") String email){
        treeUpdateEventProducer.sendUpdateEvent(email,2026, Map.of(LocalDate.now(),new AllCampaignTypeData()));
    }
}
