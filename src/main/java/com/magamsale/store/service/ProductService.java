package com.magamsale.store.service;

import com.magamsale.store.dto.ProductCreateDTO;
import com.magamsale.store.dto.ProductDetailDTO;
import com.magamsale.store.entity.Product;
import com.magamsale.store.entity.ProductStatus;
import com.magamsale.store.entity.Seller;
import com.magamsale.store.repository.ProductRepository;
import com.magamsale.store.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository; // 제품 DB 작업용
    private final SellerRepository sellerRepository;   // 판매자 DB 작업용

    // ✅ [추가됨] S3에 파일을 올리기 위한 업로더 (이제 로컬 대신 얘가 일합니다)
    private final S3Uploader s3Uploader;

    /* -------------------------------------------------------
       ✅ 1. 이미지 저장 (S3 버전으로 교체 완료)
       - 복잡했던 로컬 폴더 생성/저장 로직 삭제
       - S3Uploader에게 파일만 넘기면 URL을 받아옵니다.
    ------------------------------------------------------- */
    public String saveImage(MultipartFile image) throws IOException {
        // 이미지가 없으면 null 리턴 (DB에도 null 저장)
        if (image == null || image.isEmpty()) {
            return null;
        }

        // S3에 업로드하고, 결과로 받은 인터넷 주소(https://...)를 바로 리턴
        return s3Uploader.upload(image);
    }

    /* -------------------------------------------------------
       2. 상품 생성 (기존 로직 유지)
    ------------------------------------------------------- */
    public void createProduct(ProductCreateDTO dto) {

        Seller seller = sellerRepository.findByUid(dto.getSellerUid());
        if (seller == null) {
            throw new IllegalArgumentException("해당 UID의 사장님이 없습니다. uid=" + dto.getSellerUid());
        }

        // 판매자의 가게 위치(위도/경도)를 상품 정보에 자동으로 입력
        dto.setStoreLat(seller.getStoreLat());
        dto.setStoreLng(seller.getStoreLng());

        // 마감 시간 문자열(String)을 날짜 객체(LocalDateTime)로 변환
        if (dto.getDeadlineTimeStr() != null) {
            try {
                dto.setDeadlineTime(LocalDateTime.parse(dto.getDeadlineTimeStr()));
            } catch (Exception e) {
                try {
                    // "2026-01-10 21:00" 처럼 초가 없으면 ":00" 붙여서 재시도
                    dto.setDeadlineTime(LocalDateTime.parse(dto.getDeadlineTimeStr() + ":00"));
                } catch (Exception e2) {
                    throw new IllegalArgumentException("deadlineTime 형식이 올바르지 않습니다: " + dto.getDeadlineTimeStr());
                }
            }
        } else {
            dto.setDeadlineTime(null);
        }

        // 수량 체크
        if (dto.getSaleQuantity() == 0) {
            throw new IllegalArgumentException("saleQuantity must be >= 1");
        }

        // DB에 저장 (여기서 dto.imageUrl에는 이미 S3 주소가 들어있음)
        productRepository.createProduct(dto);
    }

    // ✅ 상태 변경 (ACTIVE/HIDDEN 등)
    public void changeStatus(int productId, ProductStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status는 필수입니다.");
        }

        // DELETED는 상태변경이 아니라 별도 로직으로 처리
        if (status == ProductStatus.DELETED) {
            softDelete(productId);
            return;
        }

        int updated = productRepository.updateProductStatusForManage(productId, status);
        if (updated == 0) {
            throw new IllegalArgumentException("해당 상품이 없습니다. productId=" + productId);
        }
    }

    // ✅ 소프트 삭제
    public void softDelete(int productId) {
        productRepository.softDeleteProduct(productId);
    }

    // ✅ 상품관리: 내 상품 리스트 조회
    public List<Product> getProductsBySellerForManage(int sellerUid) {
        return productRepository.findProductsBySellerForManage(sellerUid);
    }

    /* -------------------------------------------------------
       3. 전체 상품 조회
    ------------------------------------------------------- */
    public List<Product> getAllProducts() {
        return productRepository.findAllProducts();
    }

    /* -------------------------------------------------------
       4. 사장님별 상품 조회
    ------------------------------------------------------- */
    public List<Product> getProductsBySeller(int sellerUid) {
        return productRepository.findProductsBySeller(sellerUid);
    }

    /* -------------------------------------------------------
       5. 상품 상세 조회
    ------------------------------------------------------- */
    public ProductDetailDTO getProductDetail(int productId) {
        return productRepository.findProductById(productId);
    }

    /* -------------------------------------------------------
       6. 상품 수정
    ------------------------------------------------------- */
    public void updateProduct(Product product) {
        productRepository.updateProduct(product);
    }

    /* -------------------------------------------------------
       7. 상품 삭제
    ------------------------------------------------------- */
    public void deleteProduct(int productId) {
        productRepository.deleteProduct(productId);
    }
}