package com.magamsale.store.service;

import com.magamsale.store.dto.*;
import com.magamsale.store.entity.Seller;
import com.magamsale.store.exception.BadRequestException;
import com.magamsale.store.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SellerService {

    private final SellerRepository sellerRepository;
    private final PasswordEncoder passwordEncoder;

    // 1) 회원가입
    @Transactional
    public void createSeller(SellerSignupDTO dto) {
        // 중복 체크
        if (sellerRepository.existsById(dto.getId())) {
            throw new RuntimeException("이미 존재하는 아이디입니다.");
        }

        // null 방지 처리
        if (dto.getStoreLat() == null) dto.setStoreLat(0.0);
        if (dto.getStoreLng() == null) dto.setStoreLng(0.0);

        // ✅ [핵심] 비밀번호 암호화 후 저장
        String encodedPw = passwordEncoder.encode(dto.getPw());
        dto.setPw(encodedPw);

        sellerRepository.insertSeller(dto);
    }

    // 2) 로그인 (이 부분은 아주 잘 짜셨습니다!)
    public Seller login(String id, String rawPw) {
        // ✅ DB에서 비밀번호 검사 없이 아이디로만 일단 가져옴 (findById 사용)
        Seller seller = sellerRepository.findById(id);

        // ✅ 자바(Security)가 암호화된 비밀번호를 '해독'해서 비교함 (matches 사용)
        if (seller == null || !passwordEncoder.matches(rawPw, seller.getPw())) {
            throw new BadRequestException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        return seller;
    }

    // 3) 리프레시토큰 저장
    public void updateRefreshToken(int uid, String refreshToken) {
        sellerRepository.updateRefreshToken(uid, refreshToken);
    }

    // 4) UID 조회
    public Seller getSellerByUid(int uid) {
        return sellerRepository.findByUid(uid);
    }

    // 5) 내 상점 조회
    public SellerInfoResponse getMyStoreInfo(int sellerUid) {
        SellerInfoResponse info = sellerRepository.selectMyStoreInfo(sellerUid);
        if (info == null) {
            throw new IllegalArgumentException("상점 정보를 찾을 수 없습니다.");
        }
        return info;
    }

    // 판매자 상세 조회
    public SellerInfoResponse getSellerInfoByUid(int uid) {
        return getMyStoreInfo(uid);
    }

    // 6) 내 상점 수정
    @Transactional
    public void updateMyStoreInfo(int sellerUid, SellerUpdateRequest req) {
        int updated = sellerRepository.updateMyStoreInfo(sellerUid, req);
        if (updated == 0) {
            throw new IllegalStateException("상점 정보 수정 실패(대상 없음).");
        }
    }

    // 7) 회원 탈퇴
    public void deleteSeller(int uid) {
        sellerRepository.deleteByUid(uid);
    }

    // 8) 비밀번호 재설정
    @Transactional
    public void resetPassword(ResetPwRequest req) {
        List<Integer> uids = sellerRepository.findSellerUidsForPwReset(
                req.getBusinessUid(),
                req.getUsername(),
                req.getPhoneNumber()
        );

        if (uids.isEmpty()) throw new BadRequestException("일치하는 판매자가 없습니다.");
        if (uids.size() > 1) throw new BadRequestException("중복되는 계정이 있습니다.");

        int uid = uids.get(0);

        // ✅ 새 비밀번호도 반드시 암호화해서 저장!
        String encodedNewPw = passwordEncoder.encode(req.getNewPassword());
        sellerRepository.updatePasswordByUid(uid, encodedNewPw);
    }

    // 9) 아이디 찾기 (✅ 이 부분을 수정했습니다!)
    public String findId(String businessUid, String phoneNumber, String password) {
        // [수정 전] DB 쿼리로 비밀번호까지 비교 -> 암호화된 비번은 비교 불가능해서 실패함
        // [수정 후] 사업자번호랑 폰번호로만 찾고, 비밀번호는 자바에서 비교

        // 1. 사업자번호, 폰번호로 유저 조회 (findIdByInfo 쿼리 사용 - 아래 XML 확인)
        Seller seller = sellerRepository.findIdByInfo(businessUid, phoneNumber);

        if (seller == null) {
            throw new BadRequestException("일치하는 정보가 없습니다.");
        }

        // 2. 여기서 비밀번호 비교 (matches)
        if (!passwordEncoder.matches(password, seller.getPw())) {
            throw new BadRequestException("비밀번호가 일치하지 않습니다.");
        }

        return seller.getId();
    }


    public void verifySellerForPwReset(FindPwRequest req) {
        List<Integer> uids = sellerRepository.findSellerUidsForPwReset(
                req.getBusinessUid(),
                req.getUsername(),
                req.getPhoneNumber()
        );

        if (uids.isEmpty()) throw new BadRequestException("일치하는 판매자가 없습니다.");
        if (uids.size() > 1) throw new BadRequestException("중복되는 계정이 있습니다.");
    }
}