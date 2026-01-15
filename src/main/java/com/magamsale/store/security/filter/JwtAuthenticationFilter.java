package com.magamsale.store.security.filter;

import com.magamsale.store.entity.Seller;
import com.magamsale.store.security.JwtUtil;
import com.magamsale.store.service.SellerService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * ✅ JwtAuthenticationFilter (OncePerRequestFilter)
 *
 * [역할]
 * - 매 요청마다 Authorization 헤더의 Bearer 토큰을 확인
 * - 토큰이 유효하면 SecurityContext에 인증(Authentication) 객체를 세팅
 *
 * [왜 필요한가?]
 * - 컨트롤러에서 @AuthenticationPrincipal 또는 SecurityContext 기반 인증/인가 가능
 * - Spring Security가 "로그인된 사용자"로 인식하게 만드는 핵심 필터
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SellerService sellerService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, SellerService sellerService) {
        this.jwtUtil = jwtUtil;
        this.sellerService = sellerService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1) Authorization 헤더 읽기
        String header = request.getHeader("Authorization");

        // 토큰이 없거나 Bearer 형식이 아니면 → 인증 없이 통과
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2) "Bearer " 제거해서 순수 토큰만 추출  ✅ (중요)
        String token = header.substring("Bearer ".length());

        // 3) 이미 인증이 세팅되어 있으면 중복 세팅하지 않음 (불필요한 덮어쓰기 방지)
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // 4) 만료/유효성 검사
            //    만료면 인증 세팅 없이 통과 (Refresh 로직에서 처리 가능)
            if (jwtUtil.isAccessTokenExpired(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 5) 토큰에서 판매자 UID 추출
            int sellerUid = jwtUtil.getUserUid(token);

            // 6) DB에서 판매자 조회 (토큰은 신원 확인용, 실제 유저 존재/상태는 DB로 보강)
            Seller seller = sellerService.getSellerByUid(sellerUid);
            if (seller == null) {
                filterChain.doFilter(request, response);
                return;
            }

            // 7) 권한(ROLE) 구성
            //    - 토큰의 role 클레임을 사용하면 가장 명확함
            //    - 없으면 seller 엔티티의 roles 기반으로 구성
            String role = null;
            try {
                role = jwtUtil.getRole(token); // 예: "SELLER"
            } catch (Exception ignore) {
                // role 클레임이 없거나 파싱 실패해도 fallback 로직으로 진행
            }

            List<SimpleGrantedAuthority> authorities;

            if (role != null && !role.isBlank()) {
                // ✅ role이 "SELLER"면 "ROLE_SELLER"로 맞춰서 Spring Security 규칙에 맞춤
                String normalized = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                authorities = List.of(new SimpleGrantedAuthority(normalized));
            } else if (seller.getRoles() != null && !seller.getRoles().isEmpty()) {
                // seller.getRoles()가 ["ROLE_SELLER"] 같은 형태라면 그대로 권한 변환 가능
                authorities = seller.getRoles().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();
            } else {
                // 최후 기본값
                authorities = List.of(new SimpleGrantedAuthority("ROLE_SELLER"));
            }

            // 8) 스프링 시큐리티 인증 객체 생성
            //    principal: seller (컨트롤러에서 꺼내 쓰기 쉬움)
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(seller, null, authorities);

            // 9) 요청 정보 세팅 (IP, 세션 등 부가정보)
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            // 10) SecurityContext에 인증 저장 (이 시점부터 "로그인 상태"로 인식됨)
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            // ✅ 토큰 파싱 에러, 예상치 못한 오류가 나도 서버를 죽이지 않고 "비인증"으로 통과
            // (원하면 여기서 response.sendError(401)로 막을 수도 있음)
        }

        // 11) 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    /* -------------------- (추가로 넣으면 좋은 것들) --------------------
     *
     * ✅ [옵션1] 로그인/회원가입/리프레시 같은 엔드포인트는 필터를 아예 안 타게 만들기
     * protected boolean shouldNotFilter(HttpServletRequest request) {
     *     String path = request.getRequestURI();
     *     return path.startsWith("/api/seller/login")
     *         || path.startsWith("/api/seller/signup")
     *         || path.startsWith("/api/auth/refresh")
     *         || path.startsWith("/swagger")
     *         || path.startsWith("/v3/api-docs");
     * }
     *
     * ✅ [옵션2] 만료/오류면 그냥 통과가 아니라 401을 주고 싶으면:
     * response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
     * return;
     *
     * ------------------------------------------------------------------ */
}