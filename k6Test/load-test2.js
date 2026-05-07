import http from 'k6/http';
import { check, sleep } from 'k6';

// 1. 테스트 옵션 설정
export const options = {
    // 갑자기 500명이 들어오면 로컬 PC 네트워크 포트가 죽을 수 있으니, 단계별로(Ramp-up) 올리는 걸 추천해.
    stages: [
        { duration: '10s', target: 100 }, // 10초 동안 100명으로 증가
        { duration: '30s', target: 500 }, // 30초 동안 500명까지 증가 (최대 부하)
        { duration: '20s', target: 500 }, // 20초 동안 500명 유지하며 융단폭격
        { duration: '10s', target: 0 },   // 10초 동안 0명으로 쿨다운
    ],
};

// 테스트 환경에 맞게 포트와 엔드포인트 URL을 수정해줘!
const BASE_URL = 'http://localhost:8083';

export default function () {
    // 2. 동적 데이터 생성 (VU 500명이 각각 다른 이메일로 요청)
    // __VU: 현재 가상 유저 번호 (1~500)
    // __ITER: 현재 유저가 반복한 횟수
    const email = `testUser${__VU}@test.com`;

    // 1월 1일부터 6월 30일까지 (약 180일 구간)
    const startDate = '2026-01-01';
    const endDate = '2026-06-30';

    // 3. API 요청 URL 세팅
    // 쿼리 파라미터나 Path Variable 등 네 API 스펙에 맞게 조립해.
    const url = `${BASE_URL}/api/query/campaign/totalAnalysisData/Prefix?email=${email}&start=${startDate}&end=${endDate}`;

    // 4. GET 요청 발송
    const res = http.get(url);

    // 5. 검증 (Http Status 200이 잘 떨어지는지)
    check(res, {
        'status is 200': (r) => r.status === 200,
    });

    // 6. 무한 루프 방지 및 현실적인 요청 딜레이 (0.1초)
    // 딜레이가 없으면 로컬 CPU가 요청을 보내는 데 다 쓰여서 서버가 제대로 된 성능을 못 내.
    sleep(0.1);
}