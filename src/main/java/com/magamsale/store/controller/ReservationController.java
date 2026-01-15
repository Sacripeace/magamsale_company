package com.magamsale.store.controller;

import com.magamsale.store.dto.CreateReservationRequest;
import com.magamsale.store.dto.ReservationResponse;
import com.magamsale.store.dto.SellerDashboardDto;
import com.magamsale.store.dto.UpdateReservationStatusRequest;
import com.magamsale.store.service.ReservationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // 1. 예약 생성
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> create(
            @RequestBody CreateReservationRequest req,
            @RequestHeader(value = "X-ACTOR-TYPE", defaultValue = "USER") String actorType,
            @RequestHeader(value = "X-ACTOR-UID", defaultValue = "0") int actorUid,
            @RequestHeader(value = "X-ACTOR-NAME", required = false) String actorName,
            @RequestHeader(value = "X-ACTOR-PHONE", required = false) String actorPhone
    ) {
        int reservationId = reservationService.createReservation(req, actorType, actorUid, actorName, actorPhone);
        return Map.of("reservationId", reservationId, "message", "예약 성공");
    }

    // 2. [판매자용] 리스트 조회
    @GetMapping(value = "/seller/{sellerUid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ReservationResponse> getSellerReservations(@PathVariable int sellerUid) {
        return reservationService.getSellerReservations(sellerUid);
    }

    // 3. 상태 변경
    @PatchMapping(value = "/{reservationId}/status", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> updateStatus(
            @PathVariable int reservationId,
            @RequestBody UpdateReservationStatusRequest req,
            @RequestHeader(value = "X-ACTOR-TYPE", defaultValue = "SELLER") String actorType,
            @RequestHeader(value = "X-ACTOR-UID", defaultValue = "0") int actorUid
    ) {
        reservationService.updateStatus(reservationId, req.getStatus(), actorType, actorUid);
        return Map.of("ok", true, "status", req.getStatus());
    }

    // 4. [구매자용] 리스트 조회
    @GetMapping(value = "/my", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ReservationResponse> my(
            @RequestHeader(value = "X-ACTOR-TYPE", defaultValue = "USER") String actorType,
            @RequestHeader(value = "X-ACTOR-UID", defaultValue = "0") int actorUid
    ) {
        return reservationService.listByBuyer(actorType, actorUid);
    }

    // 5. 단건 조회
    @GetMapping(value = "/{reservationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ReservationResponse one(@PathVariable int reservationId) {
        return reservationService.getOne(reservationId);
    }

    // ✅ [추가] 판매자 대시보드 통계 API
    // GET /api/reservations/dashboard/stats
    @GetMapping("/dashboard/stats")
    public ResponseEntity<SellerDashboardDto> getDashboardStats(
            @RequestHeader(value = "X-ACTOR-UID", defaultValue = "0") int actorUid
    ) {
        if (actorUid == 0) {
            // 로그인 안된 경우 빈 객체 혹은 에러 리턴
            return ResponseEntity.ok(new SellerDashboardDto());
        }

        SellerDashboardDto stats = reservationService.getSellerDashboardStats(actorUid);
        return ResponseEntity.ok(stats);
    }

}