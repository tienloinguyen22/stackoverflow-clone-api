package com.neoflies.mystackoverflowapi.domains;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {
  @Id
  @Column(columnDefinition = "uuid", updatable = false)
  private UUID token;

  @ManyToOne
  private User user;

  public RefreshToken(UUID token, User user) {
    this.token = token;
    this.user = user;
  }
}
