package chpay.frontend.customer.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminLogController {

  @PreAuthorize("hasRole('ADMIN')")
  @GetMapping(value = "/admin/log")
  public String getLog(Model model) {
    try {
      Path path = Paths.get("src/main/resources/logs/application.log");
      String logContent = Files.readString(path);
      model.addAttribute("errorLog", logContent);
    } catch (IOException e) {
      model.addAttribute("errorLog", "Unable to read error log: " + e.getMessage());
    }
    model.addAttribute("urlPage", "adminLogs");
    return "admin-log";
  }
}
