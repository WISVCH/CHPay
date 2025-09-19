package ch.wisv.chpay.admin.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping(value = "/admin/log")
public class AdminLogController extends AdminController {
  @GetMapping
  public String getLog(Model model) {
    try {
      Path path = Paths.get("src/main/resources/logs/application.log");
      String logContent = Files.readString(path);
      model.addAttribute(MODEL_ATTR_ERROR_LOG, logContent);
    } catch (IOException e) {
      model.addAttribute(MODEL_ATTR_ERROR_LOG, "Unable to read error log: " + e.getMessage());
    }
    model.addAttribute(MODEL_ATTR_URL_PAGE, "adminLogs");
    return "admin-log";
  }
}
