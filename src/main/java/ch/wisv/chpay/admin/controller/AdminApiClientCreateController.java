package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.api.client.model.ApiClientRole;
import ch.wisv.chpay.api.client.service.ApiClientService;
import ch.wisv.chpay.core.service.NotificationService;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('SUPERADMIN')")
@RequestMapping("/admin/api-clients/new")
public class AdminApiClientCreateController extends AdminController {

  private final ApiClientService apiClientService;
  private final NotificationService notificationService;

  public AdminApiClientCreateController(
      ApiClientService apiClientService, NotificationService notificationService) {
    this.apiClientService = apiClientService;
    this.notificationService = notificationService;
  }

  @GetMapping
  public String showCreateForm(Model model) {
    model.addAttribute(MODEL_ATTR_URL_PAGE, "adminApiClients");
    return "admin-api-client-create";
  }

  @PostMapping
  public String createClient(
      @RequestParam("name") String name,
      @RequestParam(value = "roles", required = false) List<String> roleValues,
      RedirectAttributes redirectAttributes) {
    try {
      Set<ApiClientRole> roles = parseRoles(roleValues);
      ApiClientService.IssuedToken issuedToken = apiClientService.createClient(name, roles);
      notificationService.addSuccessMessage(redirectAttributes, "API client created successfully");
      redirectAttributes.addFlashAttribute("issuedBearerToken", issuedToken.bearerToken());
      return "redirect:/admin/api-clients/" + issuedToken.client().getId();
    } catch (IllegalArgumentException ex) {
      notificationService.addErrorMessage(redirectAttributes, ex.getMessage());
      return "redirect:/admin/api-clients/new";
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
