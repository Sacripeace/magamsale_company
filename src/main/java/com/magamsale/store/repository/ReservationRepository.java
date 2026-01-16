package com.magamsale.store.repository;

import com.magamsale.store.dto.ReservationResponse;
import com.magamsale.store.dto.SellerDashboardDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ReservationRepository {

    Map<String, Object> selectProductSnapshot(@Param("productId") int productId);

    int decreaseProductQuantity(@Param("productId") int productId, @Param("qty") int qty);

    int increaseProductQuantity(@Param("productId") int productId, @Param("qty") int qty);

    int insertReservation(Map<String, Object> params);

    Map<String, Object> selectReservationAuth(@Param("reservationId") int reservationId);

    int updateReservationStatus(@Param("reservationId") int reservationId, @Param("status") String status);

    List<ReservationResponse> selectByBuyer(@Param("buyerType") String buyerType, @Param("buyerUid") int buyerUid);

    List<ReservationResponse> selectBySeller(@Param("sellerUid") int sellerUid);

    ReservationResponse selectOne(@Param("reservationId") int reservationId);

    SellerDashboardDto selectSellerDashboardStats(@Param("sellerUid") int sellerUid);

    ReservationResponse selectLatestReservationBySeller(@Param("sellerUid") int sellerUid);

    // ✅ 이거 하나만 남기세요! (XML의 #{uid}와 일치해야 함)
    String selectSellerPhone(@Param("uid") int uid);

    // ✅ 일반 유저 폰번호 조회
    String selectUserPhone(@Param("uid") int uid);
}