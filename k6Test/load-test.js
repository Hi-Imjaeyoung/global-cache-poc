import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '10s', target: 100 }, // 처음 10초 동안 100명까지 서서히 증가 (Tomcat 충격 완화)
        { duration: '20s', target: 500 }, // 다음 20초 동안 500명까지 치솟음! (본 게임)
        { duration: '20s', target: 500 }, // 500명 상태로 20초간 극딜 유지!
        { duration: '10s', target: 0 },   // 마지막 10초 동안 0명으로 서서히 종료
    ],
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500'],
    },
};

function getRandomSixMonthsRange() {
    const year = "2026";

    const startMonthInt = Math.random() < 0.5 ? 6 : 7;
    const endMonthInt = startMonthInt + 5;

    const startMonthStr = String(startMonthInt).padStart(2, '0');
    const endMonthStr = String(endMonthInt).padStart(2, '0');

    const d1 = Math.floor(Math.random() * 28) + 1;
    const d2 = Math.floor(Math.random() * 28) + 1;

    const day1Str = String(d1).padStart(2, '0');
    const day2Str = String(d2).padStart(2, '0');

    return {
        startDate: `${year}-${startMonthStr}-${day1Str}`,
        endDate: `${year}-${endMonthStr}-${day2Str}`
    };
}


export default function () {
    const randomUserId = Math.floor(Math.random() * 500) + 1;
    const email = `test${randomUserId}@gmail.com`;
    const year = 2026
    const isWriteRequest = Math.random() < 0.05;

    if (isWriteRequest) {
        const deleteUrl = `http://localhost:8081/api/legacy/campaign/delete/data`;
        // const deleteUrl = `http://localhost:8080/api/command/campaign/delete/data`
        const payload = JSON.stringify({
            email: email,
            start: `${year}-01-01`,
            end: `${year}-01-10`,
            campaignIds: [randomUserId] // 더미 캠페인 ID
        });
        const params = { headers: { 'Content-Type': 'application/json' } };

        const res = http.del(deleteUrl, payload, params); // 혹은 http.del()
        check(res, { 'Write status 200': (r) => r.status === 200 });

    } else {
        const dates = getRandomSixMonthsRange();
        // const readUrl = `http://localhost:8080/api/query/campaign/totalAnalysisData?start=${dates.startDate}&end=${dates.endDate}&email=${email}`;
        const readUrl = `http://localhost:8081/api/legacy/campaign/get/totalData?start=${dates.startDate}&end=${dates.endDate}&email=${email}`;
        const res = http.get(readUrl);
        check(res, { 'Read status 200': (r) => r.status === 200 });

    }

    sleep(1);
}