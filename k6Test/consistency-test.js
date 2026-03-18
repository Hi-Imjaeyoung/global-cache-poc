import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 1,
    iterations: 1,
};

export default function () {
    const email = 'test1@gmail.com'; // 테스트할 타겟 유저
    const year = 2026;

    const commandUrl = `http://localhost:8081/api/command/campaign/delete/data`;
    const queryUrls = [
        `http://localhost:60761/api/query/campaign/totalAnalysisData?start=${year}-01-01&end=${year}-12-31&email=${email}`,
        `http://localhost:49844/api/query/campaign/totalAnalysisData?start=${year}-01-01&end=${year}-12-31&email=${email}`,
        `http://localhost:49837/api/query/campaign/totalAnalysisData?start=${year}-01-01&end=${year}-12-31&email=${email}`
    ];

    console.log(`\n==================================================`);
    console.log(`🔍 [MSA 정합성 검증 시작] Target User: ${email}`);
    console.log(`==================================================\n`);

    // ==========================================
    // 1️⃣ [Before] Query 서버에서 삭제 전 원본 데이터 확인 및 트리 빌드
    // ==========================================
    let beforeRes = http.get(queryUrls[0]);
    check(beforeRes, { 'Before Read Status 200': (r) => r.status === 200 });
    let beforeRes2 = http.get(queryUrls[1]);
    // check(beforeRes, { 'Before Read Status 200': (r) => r.status === 200 });
    let beforeRes3 = http.get(queryUrls[2]);
    // check(beforeRes, { 'Before Read Status 200': (r) => r.status === 200 });

    sleep(3);

    let beforeBody = JSON.parse(beforeRes.body);

    let beforeAdCost = beforeBody.data.sumOfAdSalesAndAdCostByCampaignType.ALL.adCost || 0;

    console.log(`📊 [Before] 삭제 전 2026년 총 광고비: ${beforeAdCost}`);

    const deletePayload = JSON.stringify({
        email: email,
        start: `${year}-01-01`,
        end: `${year}-01-31`, // 1월 한 달치 삭제 요청!
        campaignIds: [1]      // 더미 캠페인 ID
    });
    const params = { headers: { 'Content-Type': 'application/json' } };

    console.log(`[Action] Command 서버로 1월 데이터 삭제 요청 발송...`);
    let deleteRes = http.del(commandUrl, deletePayload, params);
    check(deleteRes, { 'Delete Status 200': (r) => r.status === 200 });

    // ==========================================
    // 3️⃣ [Wait] Kafka 이벤트 브로드캐스팅 및 L1 트리 업데이트 대기
    // ==========================================
    console.log(`Kafka 메시지 전파 및 3대 Query 서버 트리 업데이트 대기 (1.5초)...`);
    sleep(1.5);

    // ==========================================
    // 4️⃣ [Verify] 3대의 Query 서버 개별 타격 및 정합성 비교!
    // ==========================================
    console.log(`\n [Verify] 3대의 Query 서버가 모두 동일하게 감소된 데이터를 반환하는가?`);
    let isConsistent = true;
    let targetCost = -1;

    for (let i = 0; i < 3; i++) {
        let afterRes = http.get(queryUrls[i]);
        let afterBody = JSON.parse(afterRes.body);
        let afterAdCost = afterBody.data.sumOfAdSalesAndAdCostByCampaignType.ALL.adCost || 0;

        console.log(` Query Server ${i + 1} (Port 808${i + 1}) 응답값: ${afterAdCost}`);

        // 첫 번째 서버의 응답값을 기준으로 잡음
        if (i === 0) targetCost = afterAdCost;

        // 값이 기준값과 다르거나, 삭제 전보다 줄어들지 않았다면 정합성 실패!
        if (afterAdCost !== targetCost || afterAdCost >= beforeAdCost) {
            isConsistent = false;
        }
    }

    console.log(`\n==================================================`);
    if (isConsistent) {
        console.log(`✅ [SUCCESS] 완벽한 결과형 일관성(Eventual Consistency) 달성!`);
        console.log(`💡 DB 접근 없이, Kafka 이벤트만으로 3대의 서버가 정확히 동기화되었습니다.`);
    } else {
        console.log(`❌ [FAIL] 데이터 정합성 깨짐! 서버 간 캐시 상태가 다릅니다. (Kafka 수신 실패 의심)`);
    }
    console.log(`==================================================\n`);
}