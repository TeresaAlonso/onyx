/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.onyx.quartz.core.engine.questionnaire.question;

import static org.obiba.onyx.quartz.core.engine.questionnaire.question.QuestionType.ARRAY_CHECKBOX;
import static org.obiba.onyx.quartz.core.engine.questionnaire.question.QuestionType.ARRAY_RADIO;
import static org.obiba.onyx.quartz.core.engine.questionnaire.question.QuestionType.BOILER_PLATE;
import static org.obiba.onyx.quartz.core.engine.questionnaire.question.QuestionType.LIST_CHECKBOX;
import static org.obiba.onyx.quartz.core.engine.questionnaire.question.QuestionType.LIST_DROP_DOWN;
import static org.obiba.onyx.quartz.core.engine.questionnaire.question.QuestionType.LIST_RADIO;
import static org.obiba.onyx.quartz.core.engine.questionnaire.question.QuestionType.SINGLE_OPEN_ANSWER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wicket.util.value.ValueMap;
import org.obiba.onyx.core.data.IDataSource;
import org.obiba.onyx.quartz.core.engine.questionnaire.IVisitor;
import org.obiba.onyx.quartz.core.service.ActiveQuestionnaireAdministrationService;
import org.obiba.onyx.util.data.Data;
import org.obiba.onyx.util.data.DataType;

public class Question implements IHasQuestion {

  private static final long serialVersionUID = -7795909448581432466L;

  private String name;

  private String number;

  private Page page;

  private boolean multiple;

  private Integer minCount;

  private Integer maxCount;

  private String uIFactoryName;

  private List<String[]> uIArguments;

  private List<QuestionCategory> questionCategories;

  private IDataSource condition;

  private Question parentQuestion;

  private List<Question> questions;

  private String variableName;

  public Question() {
  }

  public Question(String name) {
    this.name = name;
  }

  public Page getPage() {
    if(page != null) {
      return page;
    }
    return getPageRecursively(parentQuestion);
  }

  public void setPage(Page page) {
    this.page = page;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getNumber() {
    return number;
  }

  public void setNumber(String number) {
    this.number = number;
  }

  public boolean isRequired() {
    return minCount != null && minCount > 0;
  }

  public boolean isMultiple() {
    return multiple;
  }

  public void setMultiple(boolean multiple) {
    this.multiple = multiple;
  }

  public boolean isBoilerPlate() {
    return (!hasCategories() && (getParentQuestion() == null || !getParentQuestion().hasCategories()));
  }

  public boolean hasSubQuestions() {
    return getQuestions().size() > 0;
  }

  public boolean hasCategories() {
    return getQuestionCategories().size() > 0;
  }

  public boolean isArrayOfSharedCategories() {
    if(hasSubQuestions() && hasCategories()) {
      return !hasSubQuestionsCategories();
    }
    return false;
  }

  public boolean isArrayOfJoinedCategories() {
    if(hasSubQuestions() && hasCategories()) {
      return hasSubQuestionsCategories();
    }
    return false;
  }

  /**
   * Check if any of the sub questions has categories defined.
   * @return
   */
  private boolean hasSubQuestionsCategories() {
    for(Question child : getQuestions()) {
      if(child.hasCategories()) {
        return true;
      }
    }
    return false;
  }

  public QuestionType getType() {
    if(isBoilerPlate()) return BOILER_PLATE;

    if(isArrayOfSharedCategories()) {
      return isMultiple() ? ARRAY_CHECKBOX : ARRAY_RADIO;
    }
    if(isArrayOfJoinedCategories()) {
      throw new RuntimeException("Unsupported question type [Array of joined categories] for question " + getName());
    }
    int nbCategories = getQuestionCategories().size();
    if(nbCategories == 1) return SINGLE_OPEN_ANSWER;
    if(nbCategories > 1) {
      if(isMultiple()) return LIST_CHECKBOX;
      return "quartz.DropDownQuestionPanelFactory".equals(getUIFactoryName()) ? LIST_DROP_DOWN : LIST_RADIO;
    }
    return null;
  }

  public boolean isToBeAnswered(ActiveQuestionnaireAdministrationService service) {
    if(condition == null) return true;
    if(service.isQuestionnaireDevelopmentMode()) return true;

    Data data = condition.getData(service.getQuestionnaireParticipant().getParticipant());
    if(data == null) return false;

    if(data.getType().equals(DataType.BOOLEAN)) {
      Boolean val = data.getValue();
      return val != null && val;
    }
    try {
      return Boolean.parseBoolean(data.getValueAsString());
    } catch(Exception e) {
      throw new IllegalArgumentException("Could not parse as a boolean: " + data, e);
    }
  }

  public String getUIFactoryName() {
    return uIFactoryName;
  }

  public void setUIFactoryName(String factoryName) {
    uIFactoryName = factoryName;
  }

  public ValueMap getUIArgumentsValueMap() {
    if(uIArguments == null) return null;

    ValueMap map = new ValueMap();
    for(String[] pair : uIArguments) {
      map.add(pair[0], pair[1]);
    }
    return map;
  }

  public void clearUIArguments() {
    if(uIArguments != null) uIArguments.clear();
  }

  public void addUIArgument(String key, String value) {
    if(uIArguments == null) {
      uIArguments = new ArrayList<String[]>();
    }
    uIArguments.add(new String[] { key, value });
  }

  public List<QuestionCategory> getQuestionCategories() {
    return questionCategories != null ? questionCategories : (questionCategories = new ArrayList<QuestionCategory>());
  }

  /**
   * Get the underlying {@link Category} list.
   * @return
   */
  public List<Category> getCategories() {
    List<Category> categories = new ArrayList<Category>();
    for(QuestionCategory questionCategory : getQuestionCategories()) {
      categories.add(questionCategory.getCategory());
    }
    return categories;
  }

  public Map<String, Category> getCategoriesByName() {
    Map<String, Category> categories = new HashMap<String, Category>();
    for(QuestionCategory questionCategory : getQuestionCategories()) {
      categories.put(questionCategory.getCategory().getName(), questionCategory.getCategory());
    }
    return categories;
  }

  public void addQuestionCategory(QuestionCategory questionCategory) {
    if(questionCategory != null) {
      getQuestionCategories().add(questionCategory);
      questionCategory.setQuestion(this);
    }
  }

  public void addQuestionCategory(QuestionCategory questionCategory, int index) {
    if(questionCategory != null) {
      getQuestionCategories().add(index, questionCategory);
      questionCategory.setQuestion(this);
    }
  }

  public void removeQuestionCategory(QuestionCategory questionCategory) {
    if(questionCategory != null) {
      getQuestionCategories().remove(questionCategory);
      questionCategory.setQuestion(null);
    }
  }

  /**
   * Does the question has an escape category.
   * @return
   */
  public boolean hasEscapeCategories() {
    for(Category category : getCategories()) {
      if(category.isEscape()) return true;
    }
    return false;
  }

  public IDataSource getCondition() {
    return condition;
  }

  public void setCondition(IDataSource condition) {
    this.condition = condition;
  }

  public Question getParentQuestion() {
    return parentQuestion;
  }

  public void setParentQuestion(Question parentQuestion) {
    this.parentQuestion = parentQuestion;
  }

  @Override
  public List<Question> getQuestions() {
    return questions != null ? questions : (questions = new ArrayList<Question>());
  }

  @Override
  public void addQuestion(Question question) {
    if(question != null) {
      getQuestions().add(question);
      question.setParentQuestion(this);
    }
  }

  @Override
  public void addQuestion(Question question, int index) {
    if(question != null) {
      getQuestions().add(index, question);
      question.setParentQuestion(this);
    }
  }

  @Override
  public void removeQuestion(Question question) {
    if(question != null && getQuestions().remove(question)) {
      question.setParentQuestion(null);
    }
  }

  public Integer getMinCount() {
    return minCount;
  }

  public void setMinCount(Integer minCount) {
    this.minCount = minCount;
  }

  public Integer getMaxCount() {
    return maxCount;
  }

  public void setMaxCount(Integer maxCount) {
    this.maxCount = maxCount;
  }

  public String getVariableName() {
    return variableName;
  }

  public void setVariableName(String variableName) {
    this.variableName = variableName;
  }

  //
  // Find methods
  //
  public Category findCategory(String name1) {
    for(Category category : getCategories()) {
      if(category.getName().equals(name1)) {
        return category;
      }
    }
    return null;
  }

  public QuestionCategory findQuestionCategory(String name1) {
    for(QuestionCategory qCategory : getQuestionCategories()) {
      if(qCategory.getCategory().getName().equals(name1)) {
        return qCategory;
      }
    }
    return null;
  }

  @Override
  public void accept(IVisitor visitor) {
    visitor.visit(this);
  }

  public boolean hasDataSource() {

    OpenAnswerDefinition openAnswerDefinition;
    List<Category> categories = getCategories();
    for(Category category : categories) {
      if((openAnswerDefinition = category.getOpenAnswerDefinition()) != null) {
        if(openAnswerDefinition.getDataSource() != null) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return getName();
  }

  private Page getPageRecursively(Question question) {
    if(question == null) {
      return null;
    }

    if(question.getPage() != null) {
      return question.getPage();
    }

    if(question.getParentQuestion() != null) {
      return getPageRecursively(question.getParentQuestion());
    }

    return null;
  }

  public QuestionCategory getNoAnswerQuestionCategory() {
    for(QuestionCategory questionCategory : getQuestionCategories()) {
      if(questionCategory.getCategory().isNoAnswer()) {
        return questionCategory;
      }
    }
    return null;
  }

  public Category getNoAnswerCategory() {
    for(Category category : getCategories()) {
      if(category.isNoAnswer()) {
        return category;
      }
    }
    return null;
  }

  public boolean hasNoAnswerCategory() {
    if(getNoAnswerCategory() != null) {
      return true;
    }
    return false;
  }
}
