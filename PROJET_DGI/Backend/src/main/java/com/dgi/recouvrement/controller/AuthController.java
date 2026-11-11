package com.dgi.recouvrement.controller;

import com.dgi.recouvrement.dto.*;
import com.dgi.recouvrement.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:8081", "http://localhost:8082"})
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/contribuable/register")
    public ResponseEntity<?> registerContribuable(@RequestBody RegisterRequest request) {
        try { return ResponseEntity.ok(authService.registerContribuable(request)); } 
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    @PostMapping("/contribuable/login")
    public ResponseEntity<?> loginContribuable(@RequestBody LoginRequest request) {
        try { 
            return ResponseEntity.ok(authService.loginContribuable(request)); 
        } catch (Exception e) { 
            return ResponseEntity.status(401).body(e.getMessage()); 
        }
    }

    @PostMapping("/dgi/login")
    public ResponseEntity<?> loginAgentDgi(@RequestBody LoginRequest request) {
        try { return ResponseEntity.ok(authService.loginAgentDgi(request)); } 
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }

    //creation d'agent DGI 
    @PostMapping("/dgi/register")
    public ResponseEntity<?> registerAgentDgi(@RequestBody RegisterAgentRequest request) {
        try { return ResponseEntity.ok(authService.registerAgentDgi(request)); } 
        catch (Exception e) { return ResponseEntity.badRequest().body(e.getMessage()); }
    }
}