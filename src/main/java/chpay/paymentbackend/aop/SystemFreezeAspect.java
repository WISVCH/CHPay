package chpay.paymentbackend.aop;

import chpay.paymentbackend.service.SettingService;
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

  @Before("@annotation(chpay.paymentbackend.aop.CheckSystemNotFrozen)")
  public void checkFreeze() {
    freezeService.assertNotFrozen();
  }
}
