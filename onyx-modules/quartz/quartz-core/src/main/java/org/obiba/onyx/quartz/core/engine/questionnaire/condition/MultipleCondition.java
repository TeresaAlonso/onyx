/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.onyx.quartz.core.engine.questionnaire.condition;

import java.util.ArrayList;
import java.util.List;

public class MultipleCondition extends Condition {

  private static final long serialVersionUID = 2969604617578085834L;

  private List<Condition> conditions;

  private ConditionOperator conditionOperator;

  public MultipleCondition() {
    this.conditions = new ArrayList<Condition>();
  }

  public List<Condition> getConditions() {
    return conditions;
  }

  public void setConditions(List<Condition> conditions) {
    this.conditions = conditions;
  }

  public ConditionOperator getConditionOperator() {
    return conditionOperator;
  }

  public void setConditionOperator(ConditionOperator conditionOperator) {
    this.conditionOperator = conditionOperator;
  }

  public boolean isToBeAnswered() {
    boolean previousConditionIsToBeAnswer = conditions.get(0).isToBeAnswered();

    if(conditionOperator.equals(ConditionOperator.AND)) {
      // Starting with second element of the list
      for(int i = 1; i < conditions.size(); i++) {
        previousConditionIsToBeAnswer = (previousConditionIsToBeAnswer && conditions.get(i).isToBeAnswered());
        if(previousConditionIsToBeAnswer == false) break;
      }
    } else {
      // Starting with second element of the list
      for(int i = 1; i < conditions.size(); i++) {
        previousConditionIsToBeAnswer = (previousConditionIsToBeAnswer || conditions.get(i).isToBeAnswered());
        if(previousConditionIsToBeAnswer == true) break;
      }
    }
    return previousConditionIsToBeAnswer;
  }

}
