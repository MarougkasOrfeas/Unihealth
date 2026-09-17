package gr.uniwa.unihealth.backend.model.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface UIRef {
  String text();

  boolean defaultField() default false;

  int exportOrder() default 1000;
}
