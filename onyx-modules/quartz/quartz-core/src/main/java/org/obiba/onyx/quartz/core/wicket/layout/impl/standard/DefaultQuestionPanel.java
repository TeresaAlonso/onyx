/***********************************************************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 **********************************************************************************************************************/
package org.obiba.onyx.quartz.core.wicket.layout.impl.standard;

import org.apache.wicket.model.IModel;
import org.obiba.onyx.quartz.core.engine.questionnaire.question.Question;
import org.obiba.onyx.quartz.core.wicket.layout.impl.BaseQuestionPanel;
import org.obiba.onyx.quartz.core.wicket.layout.impl.util.QuestionListProvider;
import org.obiba.onyx.wicket.behavior.InvalidFormFieldBehavior;

/**
 * Support for question multiple or not, with(out) child questions, with shared categories, but not yet with joined
 * categories.
 */
public class DefaultQuestionPanel extends BaseQuestionPanel {

  private static final long serialVersionUID = 2951128797454847260L;

  public DefaultQuestionPanel(String id, IModel questionModel) {
    super(id, questionModel);
    setOutputMarkupId(true);
  }

  @Override
  protected void setContent(String id) {
    Question question = (Question) getModelObject();

    if(!question.hasSubQuestions()) {
      add(new DefaultQuestionCategoriesPanel(id, getModel()));
    } else if(!question.hasCategories()) {
      add(new DefaultQuestionListPanel(id, new QuestionListProvider(question)));
    } else if(question.isArrayOfSharedCategories()) {
      add(new DefaultQuestionSharedCategoriesPanel(id, getModel()));
    } else {
      throw new UnsupportedOperationException("Joined categories array questions not supported yet");
    }

    add(new InvalidFormFieldBehavior());
  }
}
