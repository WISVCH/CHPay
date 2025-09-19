package ch.wisv.chpay.core.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "system_settings")
public class SystemSettings {

  @Id private int id = 1;

  @Getter
  @Setter
  @Column(nullable = false)
  private boolean frozen;

  @Getter
  @Setter
  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal maxBalance = new BigDecimal(200);

  @Getter
  @Setter
  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal minTopUp = new BigDecimal(2);
}
