package org.example.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.LazySegmentTreeService;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class L1CacheEvictSubscriber implements MessageListener {

    private final LazySegmentTreeService lazySegmentTreeService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            log.info("[Redis Pub/Sub 수신] L1 캐시 무효화 명령 도착: {}", body);
            String[] parts = body.split(":");
            if (parts.length == 2) {
                String email = parts[0];
                int year = Integer.parseInt(parts[1]);
                lazySegmentTreeService.removeTreeDataByEmailAndYear(email, year);
                log.debug("[L1 무효화 완료] 내 서버 메모리에서 {} 유저의 {}년도 트리 삭제!", email, year);
            }
        } catch (Exception e) {
            log.error("[Redis Pub/Sub 수신 에러] L1 무효화 처리 중 오류 발생", e);
        }
    }
}