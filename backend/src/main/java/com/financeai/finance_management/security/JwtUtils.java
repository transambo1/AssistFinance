package com.financeai.finance_management.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {
  private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

  @Value("${jwt.signerKey}")
  private String signerKey;

  @Value("${jwt.valid-duration}")
  private int validDuration;

  private Key key;

  @PostConstruct
  public void init() {
    this.key = Keys.hmacShaKeyFor(signerKey.getBytes());
  }

  public String generateToken(String userId) {
    return Jwts.builder()
        .setSubject(userId)
        .setIssuedAt(new Date())
        .setExpiration(new Date((new Date()).getTime() + (validDuration * 1000L)))
        .signWith(key, SignatureAlgorithm.HS512)
        .compact();
  }

  public String getUserIdFromJwtToken(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody()
        .getSubject();
  }

  public boolean validateJwtToken(String authToken) {
    try {
      Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
      return true;
    } catch (MalformedJwtException e) {
      logger.error("Token không hợp lệ: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      logger.error("Token đã hết hạn: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      logger.error("Token không được hỗ trợ: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      logger.error("Chuỗi Claims trống: {}", e.getMessage());
    } catch (Exception e) {
      logger.error("Lỗi xác thực Token: {}", e.getMessage());
    }
    return false;
  }
}
