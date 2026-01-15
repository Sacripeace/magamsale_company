package com.magamsale.store.repository;

import com.magamsale.store.dto.SellerInfoResponse;
import com.magamsale.store.dto.SellerSignupDTO;
import com.magamsale.store.dto.SellerUpdateRequest;
import com.magamsale.store.entity.Seller;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SellerRepository {

    // ✅ 회원가입
    void insertSeller(SellerSignupDTO dto);

    // ✅ [추가] 로그인 검증용: ID로만 사용자 정보를 조회
    Seller findById(@Param("id") String id);

    // ✅ [추가] 중복 가입 방지용
    boolean existsById(@Param("id") String id);

    // (기존 로그인용 - 이제 안 쓰지만 에러 방지 위해 유지)
    Seller findByLogin(@Param("id") String id, @Param("pw") String pw);

    // ✅ UID로 판매자 조회
    Seller findByUid(@Param("uid") int uid);

    // ✅ RefreshToken 저장
    void updateRefreshToken(@Param("uid") int uid,
                            @Param("refreshToken") String refreshToken);

    // ✅ 회원 삭제
    void deleteByUid(@Param("uid") int uid);

    // ✅ 내 상점 정보 조회
    SellerInfoResponse selectMyStoreInfo(@Param("sellerUid") int sellerUid);

    // ✅ 내 상점 정보 업데이트
    int updateMyStoreInfo(@Param("sellerUid") int sellerUid,
                          @Param("req") SellerUpdateRequest req);

    // ===============================
    // 비밀번호 찾기/재설정 기능
    // ===============================

    List<Integer> findSellerUidsForPwReset(@Param("businessUid") String businessUid,
                                           @Param("username") String username,
                                           @Param("phoneNumber") String phoneNumber);

    int updatePasswordByUid(@Param("uid") int uid,
                            @Param("newPassword") String newPassword);

    // ===============================
    // 아이디 찾기 기능
    // ===============================
    Seller findIdByInfo(@Param("businessUid") String businessUid,
                        @Param("phoneNumber") String phoneNumber);
}