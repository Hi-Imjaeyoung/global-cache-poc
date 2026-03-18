package org.example;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.domain.Campaign;
import org.example.domain.Keyword;
import org.example.domain.Member;
import org.example.repo.CampaignRepository;
import org.example.repo.KeywordRepository;
import org.example.repo.MemberRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DummyDataGenerator implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final CampaignRepository campaignRepository;
    private final KeywordRepository keywordRepository;

    @Override
    public void run(String... args) throws Exception {
        if (memberRepository.count() > 0) {
            log.info("더미 데이터가 이미 존재합니다. 스킵!");
            return;
        }
        log.info("🚀 [DataGenerator] VUs 500명 대응! 182,500건 더미 데이터 생성 시작...");
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        for (int i = 1; i <= 500; i++) {
            Member member = Member.builder()
                    .email("test" + i + "@gmail.com")
                    .name("test"+i)
                    .build();
            memberRepository.save(member);
            Campaign campaign = Campaign.builder()
                    .member(member)
                    .camCampaignName(member.getName()+"campaign")
                    .camAdType("ALL")
                    .build();
            campaignRepository.save(campaign);
            List<Keyword> keywordsToSave = new ArrayList<>();
            for (int day = 0; day < 365; day++) {
                LocalDate currentDate = startDate.plusDays(day);
                double cost = 1000 + (Math.random() * 40);
                cost = (double) Math.round(cost);
                double sales = cost * (1 + (Math.random() * 2));
                sales =(double) Math.round(sales);
                keywordsToSave.add(Keyword.builder()
                        .campaign(campaign)
                        .keyKeyword("testKeyword"+Math.random())
                        .date(currentDate)
                        .adCost(cost)
                        .adSales(sales)
                        .build());
            }
            keywordRepository.saveAll(keywordsToSave);
            if (i % 50 == 0) {
                log.info("데이터 생성 진행 중... [ {} / 500 명 완료 ]", i);
            }
        }
        log.info("[DataGenerator] 500명 x 365일 = 총 182,500건 세팅 종료");
    }
}