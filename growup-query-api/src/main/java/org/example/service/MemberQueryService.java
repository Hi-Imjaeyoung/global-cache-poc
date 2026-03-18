package org.example.service;

import lombok.AllArgsConstructor;
import org.example.domain.Member;
import org.example.repo.MemberRepository;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MemberQueryService {
    private final MemberRepository memberRepository;

    public Member getMemberById(Long id){
        return memberRepository.findById(id).orElseThrow();
    }
}
