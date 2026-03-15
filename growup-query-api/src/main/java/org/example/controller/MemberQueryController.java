package org.example.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.MemberQueryService;
import org.example.domain.Member;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api/query/member")
@RestController
@AllArgsConstructor
@Slf4j
public class MemberQueryController {

    private final MemberQueryService memberCommandService;

    @GetMapping("/get")
    public ResponseEntity<?> saveMember(@RequestParam Long id){
        Member member = memberCommandService.getMemberById(id);
        log.info(member.getEmail());
        return ResponseEntity.status(HttpStatus.OK).body(member);
    }
}
