package com.magamsale.store.scheduler;

import com.magamsale.store.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductExpireScheduler {

    private final ProductRepository productRepository;

    // ✅ 1분마다 마감 지난 상품을 DELETED 처리
    @Scheduled(fixedDelay = 60_000)
    public void softDeleteExpiredProducts() {
        int updated = productRepository.softDeleteExpiredProducts();
        if (updated > 0) {
            log.info("마감 지난 상품 소프트삭제 처리: {}건", updated);
        }
    }
}