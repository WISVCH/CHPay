package ch.wisv.chpay.admin.service;

import ch.wisv.chpay.core.model.ConfettiShape;
import ch.wisv.chpay.core.model.ConfettiShapeType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ConfettiFormParser {

  public ConfettiFormResult parse(
      String name,
      String colorsInput,
      String scalarInput,
      String minimumTransactionsInput,
      String groupInput,
      boolean groupStartsWith,
      boolean hidden,
      boolean isDefault,
      List<String> shapeTypes,
      List<String> shapeValues) {

    String trimmedName = name == null ? "" : name.trim();
    if (trimmedName.isEmpty()) {
      return ConfettiFormResult.error("Name is required");
    }

    List<String> colors = parseColors(colorsInput);
    if (colors.isEmpty() || colors.stream().anyMatch(color -> !isValidColor(color))) {
      return ConfettiFormResult.error("Please provide at least one valid hex color (e.g. #FF0000)");
    }

    ScalarParseResult scalarResult = parseScalar(scalarInput);
    if (!scalarResult.isValid()) {
      return ConfettiFormResult.error("Scalar must be a number greater than 0");
    }

    MinimumTransactionsResult minimumTransactionsResult =
        parseMinimumTransactions(minimumTransactionsInput);
    if (!minimumTransactionsResult.isValid()) {
      return ConfettiFormResult.error("Minimum transactions must be 0 or a positive whole number");
    }

    ShapeParseResult shapeParseResult = buildShapes(shapeTypes, shapeValues);
    if (shapeParseResult.hasMissingValues()) {
      return ConfettiFormResult.error(
          "Add at least one shape and provide values for Path/Text shapes");
    }

    List<ConfettiShape> shapes = shapeParseResult.getShapes();
    if (shapes.isEmpty()) {
      return ConfettiFormResult.error("Add at least one shape");
    }

    return ConfettiFormResult.success(
        trimmedName,
        colors,
        shapes,
        scalarResult.scalar,
        minimumTransactionsResult.minimumTransactions,
        normalizeGroup(groupInput),
        normalizeGroupStartsWith(groupStartsWith, groupInput),
        hidden,
        isDefault);
  }

  private List<String> parseColors(String colorsInput) {
    if (colorsInput == null) {
      return List.of();
    }
    return Arrays.stream(colorsInput.split(","))
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .collect(Collectors.toList());
  }

  private boolean isValidColor(String color) {
    return color.matches("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");
  }

  private ScalarParseResult parseScalar(String scalarInput) {
    if (scalarInput == null || scalarInput.trim().isEmpty()) {
      return ScalarParseResult.invalid();
    }
    try {
      double scalar = Double.parseDouble(scalarInput.trim());
      if (Double.isFinite(scalar) && scalar > 0) {
        return ScalarParseResult.valid(scalar);
      }
    } catch (NumberFormatException ignored) {
      // handled below
    }
    return ScalarParseResult.invalid();
  }

  private MinimumTransactionsResult parseMinimumTransactions(String minimumTransactionsInput) {
    if (minimumTransactionsInput == null || minimumTransactionsInput.trim().isEmpty()) {
      return MinimumTransactionsResult.invalid();
    }
    try {
      int minimumTransactions = Integer.parseInt(minimumTransactionsInput.trim());
      if (minimumTransactions >= 0) {
        return MinimumTransactionsResult.valid(minimumTransactions);
      }
    } catch (NumberFormatException ignored) {
      // handled below
    }
    return MinimumTransactionsResult.invalid();
  }

  private String normalizeGroup(String groupInput) {
    if (groupInput == null) {
      return null;
    }
    String trimmed = groupInput.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private boolean normalizeGroupStartsWith(boolean groupStartsWith, String groupInput) {
    String normalized = normalizeGroup(groupInput);
    return normalized != null && groupStartsWith;
  }

  private ShapeParseResult buildShapes(List<String> shapeTypes, List<String> shapeValues) {
    List<ConfettiShape> shapes = new ArrayList<>();
    boolean hasMissingValues = false;

    List<String> sanitizedTypes = sanitizeList(shapeTypes);
    List<String> sanitizedValues = sanitizeList(shapeValues);

    for (int index = 0; index < sanitizedTypes.size(); index++) {
      String type = sanitizedTypes.get(index);
      if (type == null || type.trim().isEmpty()) {
        continue;
      }
      try {
        ConfettiShapeType shapeType =
            ConfettiShapeType.valueOf(type.trim().toUpperCase(Locale.ROOT));
        String value =
            index < sanitizedValues.size() && sanitizedValues.get(index) != null
                ? sanitizedValues.get(index).trim()
                : "";
        if (shapeType == ConfettiShapeType.PATH || shapeType == ConfettiShapeType.TEXT) {
          if (value.isEmpty()) {
            hasMissingValues = true;
            continue;
          }
          shapes.add(new ConfettiShape(shapeType, value));
        } else {
          shapes.add(new ConfettiShape(shapeType, null));
        }
      } catch (IllegalArgumentException ignored) {
        // Ignore unknown shape types
      }
    }

    return new ShapeParseResult(shapes, hasMissingValues);
  }

  private List<String> sanitizeList(List<String> values) {
    if (values == null) {
      return List.of();
    }
    return values.stream().filter(Objects::nonNull).collect(Collectors.toList());
  }

  private static final class ShapeParseResult {
    private final List<ConfettiShape> shapes;
    private final boolean missingValues;

    private ShapeParseResult(List<ConfettiShape> shapes, boolean missingValues) {
      this.shapes = shapes;
      this.missingValues = missingValues;
    }

    private List<ConfettiShape> getShapes() {
      return shapes;
    }

    private boolean hasMissingValues() {
      return missingValues;
    }
  }

  public static final class ConfettiFormResult {
    private final String name;
    private final List<String> colors;
    private final List<ConfettiShape> shapes;
    private final double scalar;
    private final int minimumTransactions;
    private final String group;
    private final boolean groupStartsWith;
    private final boolean hidden;
    private final boolean isDefault;
    private final String errorMessage;

    private ConfettiFormResult(
        String name,
        List<String> colors,
        List<ConfettiShape> shapes,
        double scalar,
        int minimumTransactions,
        String group,
        boolean groupStartsWith,
        boolean hidden,
        boolean isDefault,
        String errorMessage) {
      this.name = name;
      this.colors = colors;
      this.shapes = shapes;
      this.scalar = scalar;
      this.minimumTransactions = minimumTransactions;
      this.group = group;
      this.groupStartsWith = groupStartsWith;
      this.hidden = hidden;
      this.isDefault = isDefault;
      this.errorMessage = errorMessage;
    }

    public static ConfettiFormResult success(
        String name,
        List<String> colors,
        List<ConfettiShape> shapes,
        double scalar,
        int minimumTransactions,
        String group,
        boolean groupStartsWith,
        boolean hidden,
        boolean isDefault) {
      return new ConfettiFormResult(
          name,
          colors,
          shapes,
          scalar,
          minimumTransactions,
          group,
          groupStartsWith,
          hidden,
          isDefault,
          null);
    }

    public static ConfettiFormResult error(String message) {
      return new ConfettiFormResult(
          null, List.of(), List.of(), 0.0, 0, null, false, false, false, message);
    }

    public boolean isValid() {
      return errorMessage == null;
    }

    public String getName() {
      return name;
    }

    public List<String> getColors() {
      return colors;
    }

    public List<ConfettiShape> getShapes() {
      return shapes;
    }

    public double getScalar() {
      return scalar;
    }

    public int getMinTransactions() {
      return minimumTransactions;
    }

    public String getGroup() {
      return group;
    }

    public boolean isGroupStartsWith() {
      return groupStartsWith;
    }

    public boolean isHidden() {
      return hidden;
    }

    public boolean isDefault() {
      return isDefault;
    }

    public String getErrorMessage() {
      return errorMessage;
    }
  }

  private static final class ScalarParseResult {
    private final double scalar;
    private final boolean valid;

    private ScalarParseResult(double scalar, boolean valid) {
      this.scalar = scalar;
      this.valid = valid;
    }

    private static ScalarParseResult valid(double scalar) {
      return new ScalarParseResult(scalar, true);
    }

    private static ScalarParseResult invalid() {
      return new ScalarParseResult(0.0, false);
    }

    private boolean isValid() {
      return valid;
    }
  }

  private static final class MinimumTransactionsResult {
    private final int minimumTransactions;
    private final boolean valid;

    private MinimumTransactionsResult(int minimumTransactions, boolean valid) {
      this.minimumTransactions = minimumTransactions;
      this.valid = valid;
    }

    private static MinimumTransactionsResult valid(int minimumTransactions) {
      return new MinimumTransactionsResult(minimumTransactions, true);
    }

    private static MinimumTransactionsResult invalid() {
      return new MinimumTransactionsResult(0, false);
    }

    private boolean isValid() {
      return valid;
    }
  }
}
