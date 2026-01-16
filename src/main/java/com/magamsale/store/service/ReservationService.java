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
        int sellerUid = ((Number) snap.get("sellerUid")).intValue();
        int salePrice = ((Number) snap.get("salePrice")).intValue();

        // 4. 재고 감소 시도
        int updated = reservationRepository.decreaseProductQuantity(req.getProductId(), req.getQuantity());
        if (updated == 0) {
            throw new IllegalStateException("재고가 부족합니다 (품절).");
        }

        // 5. [핵심] DB에서 진짜 전화번호 가져오기 (전화번호 누락 해결)
        String realBuyerPhone = "010-0000-0000";

        if ("SELLER".equals(buyerType)) {
            // ... (판매자 로직 유지) ...
            try {
                String dbPhone = reservationRepository.selectSellerPhone(buyerUid);
                if (dbPhone != null && !dbPhone.isEmpty()) realBuyerPhone = dbPhone;
            } catch (Exception e) {}

        } else if ("USER".equals(buyerType)) {
            // 🚨 [여기가 문제였음!]
            String dbPhone = null;
            if (buyerUid != 0) {
                // UID가 있으면 UID로 조회
                dbPhone = reservationRepository.selectUserPhone(buyerUid);
            } else {
                // 🚨 UID가 0이면(문자열 ID) 'buyerName'(=testuser01)으로 조회해야 함!
                dbPhone = reservationRepository.selectUserPhoneById(buyerName);
            }

            if (dbPhone != null && !dbPhone.isEmpty()) {
                realBuyerPhone = dbPhone; // DB 번호 발견!
            } else {
                // DB에도 없으면 어쩔 수 없이 프론트 값 사용
                if (buyerPhone != null && !buyerPhone.isEmpty()) realBuyerPhone = buyerPhone;
            }
        }

        // 6. 가격 계산 및 만료 시간 설정
        int totalPrice = salePrice * req.getQuantity();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);
        String expiresAtStr = expiresAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // DB NULL 방어용 이름 처리
        String safeBuyerName = (buyerName != null && !buyerName.isEmpty()) ? buyerName : "구매자(정보없음)";

        // 7. 저장할 데이터 맵핑
        Map<String, Object> param = new HashMap<>();
        param.put("sellerUid", sellerUid);
        param.put("productId", req.getProductId());
        param.put("buyerType", buyerType);
        param.put("buyerUid", buyerUid);

        param.put("buyerName", safeBuyerName);
        param.put("buyerPhone", realBuyerPhone); // 🚨 진짜 전화번호 저장

        param.put("quantity", req.getQuantity());
        param.put("salePrice", salePrice);
        param.put("totalPrice", totalPrice);
        param.put("reservationTime", req.getReservationTime());
        param.put("requestMessage", req.getRequestMessage());
        param.put("expiresAt", expiresAtStr);

        // 8. DB 저장
        reservationRepository.insertReservation(param);

        // 9. 생성된 ID 반환
        Object rid = param.get("reservationId");
        return rid == null ? 0 : Integer.parseInt(String.valueOf(rid));
    }

    // --- 상태 변경 ---
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

        // 취소/만료 시 재고 복구
        if ("CANCELLED".equals(nextStatus) || "EXPIRED".equals(nextStatus)) {
            reservationRepository.increaseProductQuantity(productId, qty);
        }
    }

    // --- 조회 메서드들 ---

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