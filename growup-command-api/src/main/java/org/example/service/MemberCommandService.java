package org.example.service;

import lombok.AllArgsConstructor;
import org.example.dto.MemberRequestRecord;
import org.example.domain.Member;

import org.example.repo.MemberRepository;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MemberCommandService {

    private final MemberRepository memberRepository;

    public void save(MemberRequestRecord memberRequestRecord){
        Member member = Member.builder()
                .email(memberRequestRecord.email())
                .name(memberRequestRecord.name())
                .build();
        memberRepository.save(member);
    }
}
