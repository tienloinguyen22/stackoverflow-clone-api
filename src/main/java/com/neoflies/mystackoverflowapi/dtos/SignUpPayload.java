package com.neoflies.mystackoverflowapi.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
public class SignUpPayload {
  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email address")
  private String email;

  @NotBlank(message = "Password is required")
  private String password;

  @NotBlank(message = "First Name is required")
  private String firstName;

  @NotBlank(message = "Last Name is required")
  private String lastName;
}
