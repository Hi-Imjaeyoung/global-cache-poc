import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics'; // 추가!

const redisTrend = new Trend('duration_redis');
const healthTrend = new Trend('duration_health');

export const options = {
    scenarios: {
        // 1. Redis API 부하 (이놈이 스레드를 다 잡아먹을 예정)
        redis_scenario: {
            executor: 'constant-arrival-rate',
            rate: 300,
            timeUnit: '1s',
            duration: '30s',
            preAllocatedVUs: 100,
            maxVUs: 500,
            exec: 'callRedisApi',
        },
        // 2. Health Check 모니터링 (격벽이 없을 때 같이 죽는지 확인)
        health_scenario: {
            executor: 'constant-arrival-rate',
            rate: 10, // 얘는 1초에 10번만 찔러봐도 충분해
            timeUnit: '1s',
            duration: '30s',
            preAllocatedVUs: 10,
            exec: 'callHealthApi',
        },
    },
};

export function callRedisApi() {
    let res = http.get('http://localhost:8083/api/query/campaign/totalAnalysisData?start=2026-01-01&end=2026-06-01&email=test33@gmail.com'); // 실제 네 Redis API 경로로 수정해줘!
    redisTrend.add(res.timings.duration); // Redis 시간만 기록!
}

export function callHealthApi() {
    let res = http.get('http://localhost:8083/api/query/campaign/health-check');
    healthTrend.add(res.timings.duration); // Health 시간만 기록!
    check(res, { 'health is 200': (r) => r.status === 200 });
}