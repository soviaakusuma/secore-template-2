package com.inomial.secore.template;

import java.util.Date;

import com.telflow.assembly.healthcheck.Healthcheck;
import com.telflow.assembly.healthcheck.HealthcheckResult;

public class InitialisedHealthCheck extends Healthcheck
{

  private boolean initialised;
  private String message;

  public InitialisedHealthCheck()
  {
    starting();
  }
  
  @Override
  public HealthcheckResult check()
  {
    return new HealthcheckResult().withHealthy(initialised).withMessage(message);
  }

  @Override
  public String getName()
  {
    return "initialised";
  }

  public void initialised()
  {
    initialised=true;
    message="App started at " + new Date();
  }

  public void starting()
  {
    initialised=false;
    message = "App starting since " + new Date();
  }

}
