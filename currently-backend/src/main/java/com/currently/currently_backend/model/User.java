package com.currently.currently_backend.model;

import com.currently.currently_backend.persistence.EncryptedStringConverter;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/*
 * File: User.java
 * Description: JPA entity and Spring Security UserDetails implementation for Currently accounts.
 * Project: Currently
 * Author: Liam Connell
 *
 */
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Register data: user-chosen handle, encrypted at rest and separate from Spring Security getUsername().
    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, unique = true)
    private String username;

    @Convert(converter = EncryptedStringConverter.class)
    private String name;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "username_hash", unique = true, length = 88)
    private String usernameHash;

    @Column(name = "email_hash", unique = true, length = 88)
    private String emailHash;

    // Security: accepts password in request JSON, never includes it in response JSON.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    // Bills Vault security: stores a BCrypt PIN hash, never the plaintext PIN.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "vault_pin_hash", length = 100)
    private String vaultPinHash;

    // Dashboard/Forecast settings: price per kWh used by Cost and Savings calculations.
    @Column(name = "price_per_kwh")
    private Double pricePerKwh;

    @Column(name = "provider_name")
    @Convert(converter = EncryptedStringConverter.class)
    private String providerName;

    public User() {}

    public User(String username, String name, String email, String password) {
        this.username = username;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    // Normal getter/setter for the user-chosen handle
    public String getUsernameField() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsernameHash() { return usernameHash; }
    public void setUsernameHash(String usernameHash) { this.usernameHash = usernameHash; }

    public String getEmailHash() { return emailHash; }
    public void setEmailHash(String emailHash) { this.emailHash = emailHash; }

    @Override
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getVaultPinHash() { return vaultPinHash; }
    public void setVaultPinHash(String vaultPinHash) { this.vaultPinHash = vaultPinHash; }

    public Double getPricePerKwh() { return pricePerKwh; }
    public void setPricePerKwh(Double pricePerKwh) { this.pricePerKwh = pricePerKwh; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    // Authentication: Spring Security uses email as the principal identifier for JWT Login.
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
