package org.obiba.onyx.quartz.core.engine.questionnaire.question;

import java.io.Serializable;


public class Category implements Serializable, ILocalizable {

  private static final long serialVersionUID = -1722883141794376906L;

  private String name;

  private OpenAnswerDefinition openAnswerDefinition;

  public Category(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public OpenAnswerDefinition getOpenAnswerDefinition() {
    return openAnswerDefinition;
  }

  public void setOpenAnswerDefinition(OpenAnswerDefinition openAnswerDefinition) {
    this.openAnswerDefinition = openAnswerDefinition;
  }

  public void accept(IQuestionnaireVisitor visitor) {
    visitor.visit(this);
  }

}
