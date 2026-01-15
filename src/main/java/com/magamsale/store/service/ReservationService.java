package com.magamsale.store.service;

import com.magamsale.store.dto.CreateReservationRequest;
import com.magamsale.store.dto.ReservationResponse;
import com.magamsale.store.dto.SellerDashboardDto;
import com.magamsale.store.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;

    public ReservationService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public int createReservation(CreateReservationRequest req,
                                 String buyerType, int buyerUid,
                                 String buyerName, String buyerPhone) {

        // 1. 유효성 검사
        if (req.getProductId() <= 0) throw new IllegalArgumentException("상품 정보가 올바르지 않습니다.");
        if (req.getQuantity() <= 0) throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");

        // 2. 상품 정보 조회 (가격, 판매자ID 확인)
        Map<String, Object> snap = reservationRepository.selectProductSnapshot(req.getProductId());
        if (snap == null || snap.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 상품입니다.");
        }

        // 3. 데이터 추출 (Null Safe & Type Safe)
        // DB 드라이버에 따라 Integer 혹은 Long으로 올 수 있으므로 Number로 받아 처리
        int sellerUid = ((Number) snap.get("sellerUid")).intValue();
        int salePrice = ((Number) snap.get("salePrice")).intValue();

        // 4. 재고 감소 시도
        int updated = reservationRepository.decreaseProductQuantity(req.getProductId(), req.getQuantity());
        if (updated == 0) {
            throw new IllegalStateException("재고가 부족합니다 (품절).");
        }

        // 5. 가격 계산 및 만료 시간 설정
        int totalPrice = salePrice * req.getQuantity();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
        String expiresAtStr = expiresAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // [핵심 수정] DB NOT NULL 제약조건 해결
        // 헤더에서 이름/폰번호가 안 넘어왔을 경우(NULL) DB 에러가 나지 않도록 기본값 설정
        String safeBuyerName = (buyerName != null && !buyerName.isEmpty()) ? buyerName : "구매자(정보없음)";
        String safeBuyerPhone = (buyerPhone != null && !buyerPhone.isEmpty()) ? buyerPhone : "010-0000-0000";

        // 6. 저장할 데이터 맵핑
        Map<String, Object> param = new HashMap<>();
        param.put("sellerUid", sellerUid);
        param.put("productId", req.getProductId());
        param.put("buyerType", buyerType);
        param.put("buyerUid", buyerUid);

        // 안전하게 처리된 이름과 전화번호 입력
        param.put("buyerName", safeBuyerName);
        param.put("buyerPhone", safeBuyerPhone);

        param.put("quantity", req.getQuantity());
        param.put("salePrice", salePrice);
        param.put("totalPrice", totalPrice);
        param.put("reservationTime", req.getReservationTime());
        param.put("requestMessage", req.getRequestMessage());
        param.put("expiresAt", expiresAtStr);

        // 7. DB 저장
        reservationRepository.insertReservation(param);

        // 8. 생성된 ID 반환
        Object rid = param.get("reservationId");
        return rid == null ? 0 : Integer.parseInt(String.valueOf(rid));
    }

    // --- 상태 변경 (기존 로직 유지 + 안전한 형변환 적용) ---
    @Transactional
    public void updateStatus(int reservationId, String nextStatus, String actorType, int actorUid) {
        Map<String, Object> auth = reservationRepository.selectReservationAuth(reservationId);
        if (auth == null || auth.isEmpty()) throw new IllegalArgumentException("예약 정보가 없습니다.");

        int sellerUid = ((Number) auth.get("sellerUid")).intValue();
        int productId = ((Number) auth.get("productId")).intValue();
        String buyerType = String.valueOf(auth.get("buyerType"));
        int buyerUid = ((Number) auth.get("buyerUid")).intValue();
        int qty = ((Number) auth.get("quantity")).intValue();

        // 권한 체크
        if ("COMPLETED".equals(nextStatus)) {
            if (!"SELLER".equals(actorType) || actorUid != sellerUid) {
                throw new SecurityException("구매 확정(완료) 처리는 판매자만 가능합니다.");
            }
        }
        if ("CANCELLED".equals(nextStatus)) {
            boolean isSeller = "SELLER".equals(actorType) && actorUid == sellerUid;
            boolean isBuyer = actorType.equals(buyerType) && actorUid == buyerUid;
            if (!isSeller && !isBuyer) throw new SecurityException("예약 취소 권한이 없습니다.");
        }

        reservationRepository.updateReservationStatus(reservationId, nextStatus);

        // 취소 시 재고 복구
        if ("CANCELLED".equals(nextStatus) || "EXPIRED".equals(nextStatus)) {
            reservationRepository.increaseProductQuantity(productId, qty);
        }
    }

    // --- 조회 메서드들 (기존 유지) ---

    public List<ReservationResponse> listByBuyer(String buyerType, int buyerUid) {
        return reservationRepository.selectByBuyer(buyerType, buyerUid);
    }

    public List<ReservationResponse> getSellerReservations(int sellerUid) {
        return reservationRepository.selectBySeller(sellerUid);
    }

    public ReservationResponse getOne(int reservationId) {
        return reservationRepository.selectOne(reservationId);
    }

    public SellerDashboardDto getSellerDashboardStats(int sellerUid) {
        return reservationRepository.selectSellerDashboardStats(sellerUid);
    }
}