package com.financeai.finance_management.service;

import com.financeai.finance_management.dto.request.AuthenticationRequest;
import com.financeai.finance_management.dto.request.IntrospectRequest;
import com.financeai.finance_management.dto.request.RefreshRequest;
import com.financeai.finance_management.dto.response.AuthenticationResponse;
import com.financeai.finance_management.dto.response.IntrospectResponse;
import com.financeai.finance_management.entity.User;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;

import java.text.ParseException;

public interface IAuthenticationService {
    IntrospectResponse introspectResponse(IntrospectRequest request) throws ParseException, JOSEException;

    AuthenticationResponse authenticate(AuthenticationRequest request);

    AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException;

    SignedJWT verifyToken(String token, boolean isRefresh) throws ParseException, JOSEException;

    String generateToken(User user);
}
