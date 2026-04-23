package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.api.client.model.ApiClientRole;
import ch.wisv.chpay.api.client.service.ApiClientService;
import ch.wisv.chpay.core.service.NotificationService;
import java.util.EnumSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('SUPERADMIN')")
@RequestMapping("/admin/api-clients/{id}")
public class AdminApiClientDetailController extends AdminController {

  private final ApiClientService apiClientService;
  private final NotificationService notificationService;

  public AdminApiClientDetailController(
      ApiClientService apiClientService, NotificationService notificationService) {
    this.apiClientService = apiClientService;
    this.notificationService = notificationService;
  }

  @GetMapping
  public String showClient(@PathVariable("id") UUID id, Model model, RedirectAttributes ra) {
    var apiClient = apiClientService.findClientById(id);
    if (apiClient.isEmpty()) {
      notificationService.addErrorMessage(ra, "API client not found");
      return "redirect:/admin/api-clients";
    }

    model.addAttribute("apiClient", apiClient.get());
    model.addAttribute(MODEL_ATTR_URL_PAGE, "adminApiClients");
    return "admin-api-client";
  }

  @PostMapping
  public String updateClientRoles(
      @PathVariable("id") UUID id,
      @RequestParam(value = "roles", required = false) List<String> roleValues,
      RedirectAttributes redirectAttributes) {
    try {
      Set<ApiClientRole> roles = parseRoles(roleValues);
      apiClientService.updateRoles(id, roles);
      notificationService.addSuccessMessage(redirectAttributes, "API client roles updated");
    } catch (NoSuchElementException ex) {
      notificationService.addErrorMessage(redirectAttributes, ex.getMessage());
      return "redirect:/admin/api-clients";
    } catch (IllegalArgumentException ex) {
      notificationService.addErrorMessage(redirectAttributes, ex.getMessage());
    }
    return "redirect:/admin/api-clients/" + id;
  }

  @PostMapping("/rotate-token")
  public String rotateToken(@PathVariable("id") UUID id, RedirectAttributes redirectAttributes) {
    try {
      ApiClientService.IssuedToken issuedToken = apiClientService.rotateToken(id);
      notificationService.addSuccessMessage(redirectAttributes, "API token rotated successfully");
      redirectAttributes.addFlashAttribute("issuedBearerToken", issuedToken.bearerToken());
      return "redirect:/admin/api-clients/" + id;
    } catch (NoSuchElementException ex) {
      notificationService.addErrorMessage(redirectAttributes, ex.getMessage());
      return "redirect:/admin/api-clients";
    }
  }

  @PostMapping("/enable")
  public String enableClient(@PathVariable("id") UUID id, RedirectAttributes redirectAttributes) {
    return setEnabled(id, true, redirectAttributes);
  }

  @PostMapping("/disable")
  public String disableClient(@PathVariable("id") UUID id, RedirectAttributes redirectAttributes) {
    return setEnabled(id, false, redirectAttributes);
  }

  private String setEnabled(UUID id, boolean enabled, RedirectAttributes redirectAttributes) {
    try {
      apiClientService.setEnabled(id, enabled);
      notificationService.addSuccessMessage(
          redirectAttributes, enabled ? "API client enabled" : "API client disabled");
      return "redirect:/admin/api-clients/" + id;
    } catch (NoSuchElementException ex) {
      notificationService.addErrorMessage(redirectAttributes, ex.getMessage());
      return "redirect:/admin/api-clients";
    }
  }

  private Set<ApiClientRole> parseRoles(List<String> roleValues) {
    if (roleValues == null || roleValues.isEmpty()) {
      return Set.of();
    }
    return roleValues.stream()
        .map(ApiClientRole::fromValue)
        .collect(Collectors.toCollection(() -> EnumSet.noneOf(ApiClientRole.class)));
  }
}
