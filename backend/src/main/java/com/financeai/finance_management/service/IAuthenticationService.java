package com.financeai.finance_management.service;

import com.financeai.finance_management.dto.request.*;
import com.financeai.finance_management.dto.response.AuthenticationResponse;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.dto.response.IntrospectResponse;
import com.financeai.finance_management.entity.User;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;

import java.text.ParseException;

public interface IAuthenticationService {
    IntrospectResponse introspectResponse(IntrospectRequest request) throws ParseException, JOSEException;

    BaseResponse<AuthenticationResponse> authenticate(AuthenticationRequest request);

    AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException;

    SignedJWT verifyToken(String token, boolean isRefresh) throws ParseException, JOSEException;

    String generateToken(User user);

    AuthenticationResponse register(RegisterRequest request);
    void logout(LogoutRequest request) throws java.text.ParseException, com.nimbusds.jose.JOSEException;
}
