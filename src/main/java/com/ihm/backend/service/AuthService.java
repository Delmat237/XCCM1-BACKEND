// AuthService.java
package com.ihm.backend.service;

import com.ihm.backend.dto.request.AuthenticationRequest;
import com.ihm.backend.dto.request.PasswordResetRequest;
import com.ihm.backend.dto.request.PasswordUpdateRequest;
import com.ihm.backend.dto.request.RegisterRequest;
import com.ihm.backend.dto.response.ApiResponse;
import com.ihm.backend.dto.response.AuthenticationResponse;

public interface AuthService {

    ApiResponse<AuthenticationResponse> authenticate(AuthenticationRequest request);

    ApiResponse<AuthenticationResponse> register(RegisterRequest request);  // ← plus ?

    ApiResponse<String> requestPasswordReset(PasswordResetRequest request); // ← String ou Void

    ApiResponse<String> resetPassword(PasswordUpdateRequest request);       // ← String
}