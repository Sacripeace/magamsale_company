package com.magamsale.store.controller;

import com.magamsale.store.dto.ProductCreateDTO;
import com.magamsale.store.dto.ProductDetailDTO;
import com.magamsale.store.dto.ProductStatusRequest;
import com.magamsale.store.entity.Product;
import com.magamsale.store.entity.ProductStatus;
import com.magamsale.store.entity.Seller;
import com.magamsale.store.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ✅ 프론트가 호출하는 "내 상품 리스트"
    // GET /api/product/seller/{sellerUid}
//    @GetMapping("/seller/{sellerUid}")
//    public List<Product> getSellerProductsForFront(@PathVariable int sellerUid) {
//        return productService.getProductsBySellerForManage(sellerUid);
//    }

    // ✅ 상품관리 전용(추가)
    @GetMapping("/manage/seller/{sellerUid}")
    public ResponseEntity<List<Product>> getMyProductsForManage(@PathVariable int sellerUid) {
        return ResponseEntity.ok(productService.getProductsBySellerForManage(sellerUid));
    }

    // ✅ 상태 변경
    // PATCH /api/product/{productId}/status  body: { "status": "ACTIVE" | "HIDDEN" }
    @PatchMapping("/{productId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable int productId,
                                          @RequestBody ProductStatusRequest req) {

        ProductStatus status = req.getStatus();
        if (status == null) {
            return ResponseEntity.badRequest().body("status는 필수입니다.");
        }

        // ✅ DELETED는 삭제 API 로직으로 처리(500 방지)
        if (status == ProductStatus.DELETED) {
            productService.softDelete(productId);
            return ResponseEntity.ok().build();
        }

        productService.changeStatus(productId, status);
        return ResponseEntity.ok().build();
    }

    // ✅ 소프트삭제
    // DELETE /api/product/{productId}
    @DeleteMapping("/{productId}")
    public ResponseEntity<?> softDelete(@PathVariable int productId) {
        productService.softDelete(productId);
        return ResponseEntity.ok().build();
    }

    /**
     * ✅ 상품 등록 (multipart)
     */
    @PostMapping(value = "/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProduct(
            @RequestParam("name") String name,
            @RequestParam("originalPrice") Integer originalPrice,
            @RequestParam("salePrice") Integer salePrice,
            @RequestParam("deadlineTimeStr") String deadlineTimeStr,
            @RequestParam(value = "deadlineTime", required = false) String deadlineTime,
            @RequestParam(value = "notice", required = false) String notice,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "storeLat", required = false) Double storeLat,
            @RequestParam(value = "storeLng", required = false) Double storeLng,
            @RequestParam(value = "address", required = false) String address,
            @RequestParam("saleQuantity") int saleQuantity,
            Authentication authentication
    ) {
        // ✅ 인증 없으면 401 (NPE 방지)
        if (authentication == null || !(authentication.getPrincipal() instanceof Seller)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {
            Seller seller = (Seller) authentication.getPrincipal();
            Integer sellerUid = seller.getUid();

            String effectiveAddress = (address != null && !address.isBlank())
                    ? address
                    : seller.getAddress();

            String imageUrl = productService.saveImage(image);

            ProductCreateDTO dto = new ProductCreateDTO();
            dto.setSellerUid(sellerUid);
            dto.setName(name);
            dto.setOriginalPrice(originalPrice);
            dto.setSalePrice(salePrice);
            dto.setNotice(notice);
            dto.setImageUrl(imageUrl);
            dto.setStoreLat(storeLat);
            dto.setStoreLng(storeLng);
            dto.setAddress(effectiveAddress);

            String effectiveDeadline = (deadlineTimeStr != null && !deadlineTimeStr.isBlank())
                    ? deadlineTimeStr
                    : deadlineTime;
            dto.setDeadlineTimeStr(effectiveDeadline);

            dto.setSaleQuantity(saleQuantity);

            productService.createProduct(dto);

            return ResponseEntity.ok("상품 등록 완료!");
        } catch (Exception e) {
            log.error("상품 등록 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("상품 등록 중 서버 오류");
        }
    }

    /** ✅ 전체 상품 조회(메인) */
    @GetMapping("/list")
    public List<Product> getProductList() {
        return productService.getAllProducts();
    }

    /** ✅ 특정 사장님 상품 조회 */
    @GetMapping("/list/{sellerUid}")
    public List<Product> getSellerProducts(@PathVariable int sellerUid) {
        return productService.getProductsBySeller(sellerUid);
    }

    /** ✅ 상품 상세 조회 */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailDTO> getProduct(@PathVariable int productId) {
        ProductDetailDTO dto = productService.getProductDetail(productId);
        if (dto == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(dto);
    }

    /**
     * ✅ 상품 수정 (multipart: 이미지 포함)
     * PUT /api/product/update/{productId}
     */
// ✅ 상품 수정 (이미지 포함) - multipart PUT
    @PutMapping(value = "/update/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProductMultipart(
            @PathVariable int productId,
            @RequestParam("name") String name,
            @RequestParam("originalPrice") Integer originalPrice,
            @RequestParam("salePrice") Integer salePrice,
            @RequestParam(value = "deadlineTimeStr", required = false) String deadlineTimeStr,
            @RequestParam(value = "notice", required = false) String notice,
            @RequestParam(value = "saleQuantity", required = false) Integer saleQuantity,
            @RequestParam(value = "image", required = false) MultipartFile image,
            Authentication authentication
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Seller)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {
            String imageUrl = (image != null && !image.isEmpty())
                    ? productService.saveImage(image)
                    : null;

            Product p = new Product();
            p.setProductId(productId);

            // ✅ 여기: 엔티티에 맞게 세터 사용
            p.setProductName(name);               // (setName X)
            p.setOriginalPrice(originalPrice);
            p.setSalePrice(salePrice);
            p.setNotice(notice);
            if (saleQuantity != null) p.setSaleQuantity(saleQuantity);

            // ✅ deadlineTime 타입이 String인 프로젝트면 그대로 넣으면 됨
            // (LocalDateTime이면 서비스에서 파싱해서 넣는 구조인데, 너는 String이라 했었지)
            if (deadlineTimeStr != null && !deadlineTimeStr.isBlank()) {
                p.setDeadlineTime(deadlineTimeStr);
            }

            // ✅ 이미지 변경이 있을 때만 업데이트
            if (imageUrl != null) {
                p.setImageUrl(imageUrl);
            }

            // ✅ 핵심: 없는 메서드 호출 제거 → 기존 updateProduct 사용
            productService.updateProduct(p);

            return ResponseEntity.ok("상품 수정 완료!");
        } catch (Exception e) {
            log.error("상품 수정 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("상품 수정 실패");
        }
    }

    /**
     * ✅ 상품 수정 (JSON) - 혹시 쓰는 곳 있을 수 있으니 남기되,
     * consumes를 명확히 해서 multipart PUT 과 충돌 방지
     */
    @PutMapping(value = "/update/{productId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public String updateProduct(@PathVariable int productId, @RequestBody Product product) {
        product.setProductId(productId);
        productService.updateProduct(product);
        return "상품 수정 완료!";
    }

    /** ✅ 상품 삭제(하드삭제) */
    @DeleteMapping("/delete/{productId}")
    public String deleteProduct(@PathVariable int productId) {
        productService.deleteProduct(productId);
        return "상품 삭제 완료!";
    }
}