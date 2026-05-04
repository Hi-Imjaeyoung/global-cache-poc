package org.example.common;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

public class RandomDateGenerator {
    LocalDate start, end;
    public RandomDateGenerator(LocalDate start, LocalDate end){
        this.start = start;
        this.end = end;
    }
    public LocalDate getLocalDate() {
        // 1. 시작 날짜와 종료 날짜를 epoch day(long)로 변환
        long startEpochDay = start.toEpochDay();
        long endEpochDay = end.toEpochDay();
        // 2. 두 epoch day 사이의 랜덤한 long 값을 생성
        long randomDay = ThreadLocalRandom
                .current()
                .nextLong(startEpochDay, endEpochDay);
        // 3. 랜덤하게 생성된 epoch day를 다시 LocalDate로 변환하여 반환
        return LocalDate.ofEpochDay(randomDay);
    }
}
