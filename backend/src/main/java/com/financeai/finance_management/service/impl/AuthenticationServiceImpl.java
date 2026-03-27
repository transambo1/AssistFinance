package com.financeai.finance_management.service.impl;

import com.financeai.finance_management.dto.request.AuthenticationRequest;
import com.financeai.finance_management.dto.request.IntrospectRequest;
import com.financeai.finance_management.dto.request.RefreshRequest;
import com.financeai.finance_management.dto.response.AuthenticationResponse;
import com.financeai.finance_management.dto.response.IntrospectResponse;
import com.financeai.finance_management.entity.InvalidatedToken;
import com.financeai.finance_management.entity.User;
import com.financeai.finance_management.exception.exception.AppException;
import com.financeai.finance_management.exception.exception.ErrorCode;
import com.financeai.finance_management.repository.InvalidatedTokenRepository;
import com.financeai.finance_management.repository.UserRepository;
import com.financeai.finance_management.service.IAuthenticationService;
import com.financeai.finance_management.service.ICategoryService;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.github.perplexhub.rsql.RSQLJPASupport;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationServiceImpl implements IAuthenticationService {
    private static final Logger log = LoggerFactory.getLogger(IAuthenticationService.class);
    UserRepository userRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;
    private static final List<String> SEARCH_FIELDS = List.of("fullname");
    private final ICategoryService categoryService;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY ;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION ;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION ;

    @Override
    public IntrospectResponse introspectResponse(IntrospectRequest request) throws ParseException, JOSEException {
        var token = request.getToken();
        boolean isValid = true;
        try {
            verifyToken(token, false);
        } catch (AppException e) {
            isValid = false;
        }
        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        boolean authenticated =  passwordEncoder.matches(request.getPassword(), user.getPassword());
        if(!authenticated) throw new AppException(ErrorCode.UNAUTHENTICATED);
        var token = generateToken(user);
        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    @Override
    public AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        var signToken = verifyToken(request.getToken(), true);

        String jit = signToken.getJWTClaimsSet().getJWTID();
        Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();
        String username = signToken.getJWTClaimsSet().getSubject();
        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jit)
                .expiryDate(expiryTime)
                .build();
        invalidatedTokenRepository.save(invalidatedToken);

        var user = userRepository.findByUsername(username).orElseThrow(
                () -> new AppException(ErrorCode.USER_NOT_EXISTED)
        );

        var token = generateToken(user);
        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

<<<<<<< Updated upstream
=======
        // 2. Mã hóa mật khẩu
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 3. Tạo user mới (Phải có ID và đúng tên field fullName)
        User newUser = User.builder()
                .id(String.valueOf(IdGenerator.generateRandomId())) // BẮT BUỘC phải có dòng này vì Entity không tự sinh ID
                .username(request.getUsername())
                .password(encodedPassword)
                .fullName(request.getFullName()) // Khớp với biến fullName trong Entity
                .email(request.getEmail())
                .currentBalance(java.math.BigDecimal.ZERO) // Set giá trị mặc định cho an toàn
                .build();

        User savedUser = userRepository.save(newUser);
        categoryService.createDefaultCategories(savedUser.getId());


        // 4. Tự động đăng nhập
        String token = generateToken(savedUser);

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }
>>>>>>> Stashed changes
    @Override
    public SignedJWT verifyToken(String token, boolean isRefresh) throws ParseException, JOSEException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);
        Date expiryTime = (isRefresh)
                ? new Date(signedJWT.getJWTClaimsSet().getIssueTime()
                .toInstant().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS)
                .toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(verifier);

        if(!(verified && expiryTime.after(new Date()))) throw new AppException(ErrorCode.UNAUTHENTICATED);

        if(invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        return signedJWT;
    }

    @Override
    public String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(String.valueOf(user.getId()))
                .issuer("fast-food.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()
                ))
                .jwtID(UUID.randomUUID().toString())
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot sign JWT object", e);
            throw new RuntimeException(e);
        }
    }
}
