package org.example;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/command/member")
@RestController
@AllArgsConstructor
public class MemberController {

    private final MemberCommandService memberCommandService;

    @PostMapping("/save")
    public void saveMember(@RequestBody MemberRequestRecord memberRequestRecord){
        memberCommandService.save(memberRequestRecord);
    }
}
