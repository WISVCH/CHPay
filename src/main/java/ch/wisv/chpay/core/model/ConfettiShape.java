package ch.wisv.chpay.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfettiShape {

  @Enumerated(EnumType.STRING)
  @Column(name = "shape_type", nullable = false, length = 16)
  private ConfettiShapeType type;

  @Column(name = "shape_value", columnDefinition = "TEXT")
  private String value;
}
