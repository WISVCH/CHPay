package ch.wisv.chpay.core.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "confetti")
@Getter
@NoArgsConstructor
public class Confetti {

  public static final double DEFAULT_SCALAR = 1.0;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Setter
  @Column(nullable = false)
  private String name;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "confetti_colors", joinColumns = @JoinColumn(name = "confetti_id"))
  @Column(name = "color", nullable = false, length = 32)
  private List<String> colors = new ArrayList<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "confetti_shapes", joinColumns = @JoinColumn(name = "confetti_id"))
  private List<ConfettiShape> shapes = new ArrayList<>();

  @Column(nullable = false)
  private double scalar = DEFAULT_SCALAR;

  @Column(name = "minimum_transactions", nullable = false)
  private int minimumTransactions;

  @Setter
  @Column(name = "group_name")
  private String group;

  @Column(nullable = false)
  private boolean hidden;

  @Setter
  @Column(name = "is_default", nullable = false)
  private boolean defaultConfetti;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  public Confetti(
      String name,
      List<ConfettiShape> shapes,
      List<String> colors,
      double scalar,
      int minimumTransactions,
      String group,
      boolean hidden,
      boolean defaultConfetti) {
    this.name = name;
    this.colors.addAll(colors);
    this.shapes.addAll(shapes);
    this.scalar = scalar;
    this.minimumTransactions = minimumTransactions;
    this.group = normalizeGroup(group);
    this.hidden = hidden;
    this.defaultConfetti = defaultConfetti;
    this.createdAt = LocalDateTime.now();
  }

  public void updateDefinition(
      String name,
      List<ConfettiShape> shapes,
      List<String> colors,
      double scalar,
      int minimumTransactions,
      String group,
      boolean hidden,
      boolean defaultConfetti) {
    this.name = name;
    this.shapes.clear();
    this.shapes.addAll(shapes);
    this.colors.clear();
    this.colors.addAll(colors);
    this.scalar = scalar;
    this.minimumTransactions = minimumTransactions;
    this.group = normalizeGroup(group);
    this.hidden = hidden;
    this.defaultConfetti = defaultConfetti;
  }

  public boolean hasShapeType(ConfettiShapeType type) {
    return shapes.stream().anyMatch(shape -> shape.getType() == type);
  }

  private String normalizeGroup(String group) {
    if (group == null) {
      return null;
    }
    String trimmed = group.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
