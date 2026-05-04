package org.example;

import org.example.common.CoupagRandomData;
import org.example.common.GenericBulkRepository;
import org.example.common.RandomDateGenerator;
import org.example.common.RandomKeywordGenerator;
import org.example.config.QueryDslConfig;
import org.example.domain.Campaign;
import org.example.domain.CoupangExcelData;
import org.example.domain.Keyword;
import org.example.domain.Member;
import org.example.dto.CampaignDto;
import org.example.dto.KeywordDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@DataJpaTest // DB 관련 빈(DataSource, JdbcTemplate 등)만 로드해! (유레카, 카프카 안 띄움)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 내장 DB 말고 우리가 설정한 실제 로컬/도커 DB를 써라!
@Import({GenericBulkRepository.class, QueryDslConfig.class}) // 우리가 만든 Repository
public class LargeDataSetUpTest {

    private GenericBulkRepository bulkRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // 빈 등록 없이 직접 생성해서 사용
        bulkRepository = new GenericBulkRepository(jdbcTemplate);
    }

    @CsvSource("500")
    @ParameterizedTest
    @Rollback(false)
    void 더미_멤버_x건_적재_시뮬레이션(int numberOfSize){
        String sql = "INSERT INTO member (name, email, password) VALUES (?, ?, ?)";
        List<Member> dummyList = generateMemberDummyData(numberOfSize);

        bulkRepository.bulkInsert(
                sql,
                dummyList,
                10000,
                (ps, data) -> {
                    try {
                        ps.setString(1, data.getName());
                        ps.setString(2, data.getEmail());
                        ps.setString(3, data.getPassword());
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }
    @CsvSource("500,10")
    @ParameterizedTest
    @Rollback(value = false)
    void 더미_캠패인_x건_적재_시뮬레이션(int memberSize,int numberOfCampaign){
        String sql = "INSERT INTO campaign (cam_campaign_name, cam_ad_type, member_id) VALUES (?, ?, ?)";
        List<CampaignDto> dummyList = generateCampaignDummyData(memberSize,numberOfCampaign);

        bulkRepository.bulkInsert(
                sql,
                dummyList,
                10000,
                (ps, data) -> {
                    try {
                        ps.setString(1, data.getName());
                        ps.setString(2, data.getAdType());
                        ps.setLong(3, data.getMemberId());
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }
    @CsvSource("476,500,20000,2026-01-01,2026-06-30")
    @ParameterizedTest
    @Rollback(value = false)
    void 더미_키워드_x건_적재_시뮬레이션(int startMember, int endMember, int numberOfKeyword, LocalDate start, LocalDate end){
        String sql = "INSERT INTO keyword (" +
                "key_keyword, key_search_type, campaign_id, " +
                "impressions, clicks, click_rate, total_sales, " +
                "cvr, cpc, ad_cost, ad_sales, roas, date" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        RandomDateGenerator randomDateGenerator = new RandomDateGenerator(start,end);
        List<KeywordDto> dummyList = generateKeywordDummyData(startMember,endMember,numberOfKeyword,randomDateGenerator);

        bulkRepository.bulkInsert(
                sql,
                dummyList,
                10000,
                (ps, data) -> {
                    try {
                        // 1~3: 기본 정보 및 FK
                        ps.setString(1, data.getKeyword());       // key_keyword
                        ps.setString(2, data.getAdType());        // key_search_type
                        ps.setLong(3, data.getCampaignId());      // campaign_id

                        // 4~12: 쿠팡 엑셀 통계 데이터 (Long, Double)
                        ps.setLong(4, data.getImpressions());     // impressions
                        ps.setLong(5, data.getClicks());          // clicks
                        ps.setDouble(6, data.getClickRate());     // click_rate
                        ps.setLong(7, data.getTotalSales());      // total_sales
                        ps.setDouble(8, data.getCvr());           // cvr
                        ps.setDouble(9, data.getCpc());           // cpc
                        ps.setDouble(10, data.getAdCost());       // ad_cost
                        ps.setDouble(11, data.getAdSales());      // ad_sales
                        ps.setDouble(12, data.getRoas());         // roas

                        // 13: 날짜 (LocalDate -> java.sql.Date)
                        ps.setDate(13, java.sql.Date.valueOf(data.getDate()));

                    } catch (SQLException e) {
                        throw new RuntimeException("매핑 도중 에러 발생! 데이터 확인해봐: " + data.getKeyword(), e);
                    }
                }
        );
    }

    public List<Member> generateMemberDummyData(int numberOfSize){
        List<Member> members = new ArrayList<>();
        for(int i=1;i<=numberOfSize;i++){
            String name = "testUser" + i;
            String email = name + "@test.com";
            members.add(
                    Member.builder()
                            .name(name)
                            .email(email)
                            .password("1234")
                            .build()
            );
        }
        return members;
    }
    public List<CampaignDto> generateCampaignDummyData(int memberSize,int numberOfCampaign){
        List<CampaignDto> campaigns = new ArrayList<>();
        int cnt = 1;
        for(int j=1;j<=memberSize;j++){
            for(int i=1;i<=numberOfCampaign;i++){
                String name = "campaign" + cnt;
                cnt++;
                campaigns.add(
                        CampaignDto.builder()
                                .name(name)
                                .adType("Normal")
                                .memberId((long)j)
                                .build()
                );
            }
        }
        return campaigns;
    }
    public List<KeywordDto> generateKeywordDummyData(int startMember,
                                                     int endMember,
                                                     int numberOfKeyword,
                                                     RandomDateGenerator randomDateGenerator){
        List<KeywordDto> keywordDtos = new ArrayList<>();

        Random random = new Random();
        for(int j=startMember;j<=endMember;j++){
            for(int i=(10 * (j-1)+1);i<= (10*(j));i++){
                RandomKeywordGenerator randomKeywordGenerator = new RandomKeywordGenerator((long)i, randomDateGenerator);
                for(int k=0;k<numberOfKeyword;k++){
                    CoupangExcelData coupangExcelData = CoupagRandomData.createRandomData();
                    int randomInt = random.nextInt(1, 11);
                    KeywordDto keyword;
                    // 비검색 키워드 생성 20%
                    if (randomInt % 5 == 0) {
                        keyword = randomKeywordGenerator.makeNonSearchKeyword(coupangExcelData);
                    } else {
                        keyword = randomKeywordGenerator.makeSearchKeyword(coupangExcelData);
                    }
                    keywordDtos.add(keyword);
                }
            }
        }
        return keywordDtos;
    }

}
