package ch.wisv.chpay.core.aop;

import ch.wisv.chpay.core.service.SettingService;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SystemFreezeAspect {

  private final SettingService freezeService;

  @Autowired
  public SystemFreezeAspect(SettingService freezeService) {
    this.freezeService = freezeService;
  }

  @Before("@annotation(ch.wisv.chpay.core.aop.CheckSystemNotFrozen)")
  public void checkFreeze() {
    freezeService.assertNotFrozen();
  }
}
