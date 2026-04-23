package ch.wisv.chpay.admin.controller;

import ch.wisv.chpay.api.client.service.ApiClientService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@PreAuthorize("hasRole('SUPERADMIN')")
@RequestMapping("/admin/api-clients")
public class AdminApiClientsController extends AdminController {

  private final ApiClientService apiClientService;

  public AdminApiClientsController(ApiClientService apiClientService) {
    this.apiClientService = apiClientService;
  }

  @GetMapping
  public String showOverview(Model model) {
    model.addAttribute("apiClients", apiClientService.findAllClients());
    model.addAttribute(MODEL_ATTR_URL_PAGE, "adminApiClients");
    return "admin-api-clients";
  }
}
