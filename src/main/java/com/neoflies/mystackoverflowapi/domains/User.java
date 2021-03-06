package com.neoflies.mystackoverflowapi.domains;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.neoflies.mystackoverflowapi.enums.Gender;
import com.neoflies.mystackoverflowapi.enums.LoginProvider;
import com.neoflies.mystackoverflowapi.utils.Auditable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends Auditable<UUID> {
  @Id
  @Column(columnDefinition = "uuid", updatable = false)
  private UUID id;

  private String email;

  @JsonIgnore
  private String password;

  private String firstName;

  private String lastName;

  private String countryCode;

  private String phoneNumber;

  private Gender gender;

  private Date dob;

  private String bio;

  private String avatarUrl;

  private LoginProvider loginProvider;

  private String providerId;

  private Boolean emailConfirmed = false;

  private Boolean active = true;

  @ManyToMany(targetEntity = Authority.class, fetch = FetchType.EAGER)
  private List<Authority> authorities = new LinkedList<>();
  
  @ManyToMany(targetEntity = Tag.class)
  private List<Tag> watchTags = new LinkedList<>();
  
  @ManyToMany(targetEntity = Tag.class)
  private List<Tag> ignoreTags = new LinkedList<>();
}
