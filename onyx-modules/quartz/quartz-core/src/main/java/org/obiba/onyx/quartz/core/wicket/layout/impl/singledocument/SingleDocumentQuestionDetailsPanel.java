/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.onyx.quartz.core.wicket.layout.impl.singledocument;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.obiba.onyx.quartz.core.engine.questionnaire.question.Question;
import org.obiba.onyx.quartz.core.wicket.layout.QuestionPanel;

public class SingleDocumentQuestionDetailsPanel extends QuestionPanel {

  private static final long serialVersionUID = 1L;

  public SingleDocumentQuestionDetailsPanel(String id, IModel<Question> questionModel) {
    super(id, questionModel);

    Question question = (Question) getDefaultModelObject();
    String questionName = (question.getParentQuestion() != null) ? question.getParentQuestion().getName() + "." + question.getName() : question.getName();
    Label questionLabel;
    add(questionLabel = new Label("label", new Model<String>(questionName)));

    if(question.hasSubQuestions() && !question.isArrayOfSharedCategories()) {
      questionLabel.setVisible(false);
    }

    add(new SingleDocumentQuestionContentPanel("content", questionModel));
  }
}