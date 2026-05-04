package org.example.common;

import jakarta.transaction.Transactional;
import org.apache.logging.log4j.util.BiConsumer;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Component
public class GenericBulkRepository {

    private final JdbcTemplate jdbcTemplate;

    public GenericBulkRepository(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }
    /**
     * @param sql 실행할 INSERT 쿼리 (예: INSERT INTO table (col1, col2) VALUES (?, ?))
     * @param dataList 넣을 데이터 리스트
     * @param batchSize 한 번에 묶어서 보낼 단위 (보통 1,000 ~ 10,000)
     * @param setter PreparedStatement에 데이터를 매핑할 로직
     */
    @Transactional
    public <T> void bulkInsert(String sql, List<T> dataList, int batchSize, BiConsumer<PreparedStatement, T> setter) {

        int totalSize = dataList.size();

        for (int i = 0; i < totalSize; i += batchSize) {
            int end = Math.min(i + batchSize, totalSize);
            List<T> subList = dataList.subList(i, end);

            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int rowNum) throws SQLException {
                    T item = subList.get(rowNum);
                    setter.accept(ps, item); // 이 부분이 핵심! 외부에서 정의한 매핑 로직 실행
                }

                @Override
                public int getBatchSize() {
                    return subList.size();
                }
            });
        }
    }
}