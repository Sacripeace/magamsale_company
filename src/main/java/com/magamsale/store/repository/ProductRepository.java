package com.magamsale.store.repository;

import com.magamsale.store.dto.ProductCreateDTO;
import com.magamsale.store.dto.ProductDetailDTO;
import com.magamsale.store.dto.ProductStatsResponse;
import com.magamsale.store.entity.Product;
import com.magamsale.store.entity.ProductStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductRepository {

    // 등록
    void createProduct(ProductCreateDTO dto);

    // 전체 조회
    List<Product> findAllProducts();

    // 사장님별 조회
    List<Product> findProductsBySeller(@Param("sellerUid") int sellerUid);

    // 상세조회
    ProductDetailDTO findProductById(@Param("productId") int productId);

    // 수정
    void updateProduct(Product product);

    // ✅ 상품관리 전용(추가)
    List<Product> findProductsBySellerForManage(@Param("sellerUid") int sellerUid);

    // 상태 변경
//    void updateProductStatus(@Param("productId") int productId,
//                             @Param("status") String status);

    // 삭제(하드삭제)
    void deleteProduct(@Param("productId") int productId);

    // 통계 (✅ sellerUid 바인딩 안전하게)
    ProductStatsResponse selectSellerProductStats(@Param("sellerUid") int sellerUid);

    // 소프트 삭제
    int softDeleteProduct(@Param("productId") int productId);

    // 9) 상품 상태 변경 (관리용)
    int updateProductStatusForManage(@Param("productId") int productId,
                                     @Param("status") ProductStatus status);

    // 10) 소프트 삭제 (관리용)
    int softDeleteProductForManage(@Param("productId") int productId);


    int softDeleteExpiredProducts();

}