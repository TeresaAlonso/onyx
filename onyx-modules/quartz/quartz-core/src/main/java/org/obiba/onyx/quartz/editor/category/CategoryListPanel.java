/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.onyx.quartz.editor.category;

import static org.apache.commons.lang.StringUtils.abbreviate;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SimpleFormComponentLabel;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.PatternValidator;
import org.obiba.onyx.quartz.core.engine.questionnaire.question.Category;
import org.obiba.onyx.quartz.core.engine.questionnaire.question.Question;
import org.obiba.onyx.quartz.core.engine.questionnaire.question.QuestionCategory;
import org.obiba.onyx.quartz.core.engine.questionnaire.question.Questionnaire;
import org.obiba.onyx.quartz.core.engine.questionnaire.util.QuestionnaireElementComparator;
import org.obiba.onyx.quartz.core.engine.questionnaire.util.QuestionnaireFinder;
import org.obiba.onyx.quartz.editor.QuartzEditorPanel;
import org.obiba.onyx.quartz.editor.locale.LocaleProperties;
import org.obiba.onyx.quartz.editor.locale.LocalePropertiesUtils;
import org.obiba.onyx.quartz.editor.question.EditedQuestion;
import org.obiba.onyx.quartz.editor.utils.AbstractAutoCompleteTextField;
import org.obiba.onyx.quartz.editor.widget.sortable.SortableList;
import org.obiba.onyx.wicket.Images;
import org.obiba.onyx.wicket.reusable.FeedbackWindow;

import com.google.common.collect.Multimap;

/**
 *
 */
@SuppressWarnings("serial")
public class CategoryListPanel extends Panel {

  // private final transient Logger logger = LoggerFactory.getLogger(getClass());

  @SpringBean
  private LocalePropertiesUtils localePropertiesUtils;

  private final ModalWindow categoryWindow;

  private final FeedbackPanel feedbackPanel;

  private final FeedbackWindow feedbackWindow;

  private SortableList<QuestionCategory> categoryList;

  private List<Category> sharedCategories;

  private Multimap<Category, Question> questionsByCategory;

  private List<Category> questionnaireCategories;

  public CategoryListPanel(String id, final IModel<EditedQuestion> model, final IModel<Questionnaire> questionnaireModel, final IModel<LocaleProperties> localePropertiesModel, FeedbackPanel feedbackPanel, FeedbackWindow feedbackWindow) {
    super(id, model);
    this.feedbackPanel = feedbackPanel;
    this.feedbackWindow = feedbackWindow;

    add(CSSPackageResource.getHeaderContribution(CategoryListPanel.class, "CategoryListPanel.css"));

    final Question question = model.getObject().getElement();

    QuestionnaireFinder questionnaireFinder = QuestionnaireFinder.getInstance(questionnaireModel.getObject());
    questionsByCategory = questionnaireFinder.findQuestionsByCategory();
    questionnaireCategories = new ArrayList<Category>(questionsByCategory.keySet());
    Collections.sort(questionnaireCategories, new QuestionnaireElementComparator());

    sharedCategories = questionnaireFinder.findSharedCategories();

    categoryWindow = new ModalWindow("categoryWindow");
    categoryWindow.setCssClassName("onyx");
    categoryWindow.setInitialWidth(950);
    categoryWindow.setInitialHeight(550);
    categoryWindow.setResizable(true);
    categoryWindow.setTitle(new ResourceModel("Category"));
    add(categoryWindow);

    List<ITab> tabs = new ArrayList<ITab>();
    tabs.add(new AbstractTab(new ResourceModel("Add.simple")) {
      @Override
      public Panel getPanel(String panelId) {
        return new SimpleAddPanel(panelId);
      }
    });
    tabs.add(new AbstractTab(new ResourceModel("Add.bulk")) {
      @Override
      public Panel getPanel(String panelId) {
        return new BulkAddPanel(panelId);
      }
    });
    tabs.add(new AbstractTab(new ResourceModel("Add.existing")) {
      @Override
      public Panel getPanel(String panelId) {
        return new AddExistingPanel(panelId);
      }
    });
    add(new AjaxTabbedPanel("addTabs", tabs));

    categoryList = new SortableList<QuestionCategory>("categories", question.getQuestionCategories()) {

      @Override
      public void onItemPopulation(QuestionCategory questionCategory) {
        localePropertiesUtils.load(localePropertiesModel.getObject(), questionnaireModel.getObject(), questionCategory, questionCategory.getCategory());
      }

      @Override
      public Component getItemTitle(@SuppressWarnings("hiding") String id, QuestionCategory questionCategory) {
        Category category = questionCategory.getCategory();
        if(sharedCategories.contains(category)) {
          StringBuilder sb = new StringBuilder();
          for(Question q : questionsByCategory.get(category)) {
            if(q.getName().equals(question.getName())) continue;
            if(sb.length() > 0) sb.append(", ");
            sb.append(q.getName());
          }
          String shared = " <span class=\"shared\">" + new StringResourceModel("sharedWith", CategoryListPanel.this, null, new Object[] { abbreviate(sb.toString(), 50) }).getString() + "</span>";
          return new Label(id, category.getName() + shared).setEscapeModelStrings(false);
        }
        return new Label(id, category.getName());
      }

      @Override
      public void editItem(QuestionCategory questionCategory, AjaxRequestTarget target) {
        categoryWindow.setContent(new CategoryWindow("content", new Model<QuestionCategory>(questionCategory), questionnaireModel, localePropertiesModel, categoryWindow) {
          @Override
          public void onSave(AjaxRequestTarget target1, QuestionCategory editedCategory) {
            refreshList(target1);
          }
        });
        categoryWindow.show(target);
      }

      @Override
      @SuppressWarnings("unchecked")
      public void deleteItem(QuestionCategory questionCategory, AjaxRequestTarget target) {
        ((IModel<EditedQuestion>) CategoryListPanel.this.getDefaultModel()).getObject().getElement().getQuestionCategories().remove(questionCategory);
        refreshList(target);
      }

      @Override
      public Button[] getButtons() {
        return null;
      }

    };
    add(categoryList);

  }

  private class SimpleAddPanel extends Panel {

    public SimpleAddPanel(String id) {
      super(id);
      Form<String> form = new Form<String>("form");
      add(form);

      final TextField<String> categoryName = new TextField<String>("category", new Model<String>());
      categoryName.setOutputMarkupId(true);
      categoryName.setLabel(new ResourceModel("NewCategory"));
      categoryName.add(new PatternValidator(QuartzEditorPanel.ELEMENT_NAME_PATTERN));

      form.add(categoryName);
      form.add(new SimpleFormComponentLabel("categoryLabel", categoryName));

      AjaxSubmitLink addLink = new AjaxSubmitLink("link", form) {
        @Override
        @SuppressWarnings("unchecked")
        protected void onSubmit(AjaxRequestTarget target, Form<?> form1) {
          String name = categoryName.getModelObject();
          if(StringUtils.isBlank(name)) return;
          if(checkIfCategoryAlreadyExists(((IModel<EditedQuestion>) CategoryListPanel.this.getDefaultModel()).getObject().getElement(), name)) {
            error(new StringResourceModel("CategoryAlreadyExists", CategoryListPanel.this, null).getObject());
            return;
          }
          addCategory(((IModel<EditedQuestion>) CategoryListPanel.this.getDefaultModel()).getObject().getElement(), name);
          categoryName.setModelObject(null);
          target.addComponent(categoryName);
          target.addComponent(categoryList);
        }

        @Override
        protected void onError(AjaxRequestTarget target, Form<?> form1) {
          feedbackWindow.setContent(feedbackPanel);
          feedbackWindow.show(target);
        }
      };

      addLink.add(new Image("img", Images.ADD).add(new AttributeModifier("title", true, new ResourceModel("Add"))));
      form.add(addLink);
    }
  }

  private class BulkAddPanel extends Panel {

    public BulkAddPanel(String id) {
      super(id);
      Form<String> form = new Form<String>("form");
      add(form);
      final TextArea<String> categories = new TextArea<String>("categories", new Model<String>());
      categories.setOutputMarkupId(true);
      categories.setLabel(new ResourceModel("NewCategories"));
      form.add(categories);
      form.add(new SimpleFormComponentLabel("categoriesLabel", categories));
      AjaxSubmitLink addLink = new AjaxSubmitLink("bulkAddLink") {
        @Override
        @SuppressWarnings("unchecked")
        protected void onSubmit(AjaxRequestTarget target, Form<?> form1) {
          String[] names = StringUtils.split(categories.getModelObject(), ',');
          if(names == null) return;
          Question question = ((IModel<EditedQuestion>) CategoryListPanel.this.getDefaultModel()).getObject().getElement();
          for(String name : new HashSet<String>(Arrays.asList(names))) {
            if(QuartzEditorPanel.ELEMENT_NAME_PATTERN.matcher(name).matches()) addCategory(question, name);
          }
          categories.setModelObject(null);
          target.addComponent(categories);
          target.addComponent(categoryList);
        }

        @Override
        protected void onError(AjaxRequestTarget target, Form<?> form1) {
          feedbackWindow.setContent(feedbackPanel);
          feedbackWindow.show(target);
        }
      };

      addLink.add(new Image("bulkAddImg", Images.ADD).add(new AttributeModifier("title", true, new ResourceModel("Add"))));
      form.add(addLink);
    }
  }

  private class AddExistingPanel extends Panel {

    private static final int AUTO_COMPLETE_SIZE = 15;

    public AddExistingPanel(String id) {
      super(id);
      Form<String> form = new Form<String>("form");
      add(form);

      final AbstractAutoCompleteTextField<CategoryWithQuestions> categoryNameFinder = new AbstractAutoCompleteTextField<CategoryWithQuestions>("category", new Model<CategoryWithQuestions>()) {
        @SuppressWarnings("unchecked")
        @Override
        protected List<CategoryWithQuestions> getChoiceList(String input) {
          if(StringUtils.isBlank(input)) {
            List<CategoryWithQuestions> emptyList = Collections.emptyList();
            return emptyList;
          }
          Question question = ((IModel<EditedQuestion>) CategoryListPanel.this.getDefaultModel()).getObject().getElement();
          List<String> questionCatNames = new ArrayList<String>(question.getCategories().size());
          for(Category category : question.getCategories()) {
            questionCatNames.add(category.getName().toUpperCase());
          }
          List<CategoryWithQuestions> choices = new ArrayList<CategoryWithQuestions>(AUTO_COMPLETE_SIZE);
          for(Category category : questionnaireCategories) {
            String name = category.getName().toUpperCase();
            if(!questionCatNames.contains(name) && name.startsWith(input.toUpperCase())) {
              choices.add(new CategoryWithQuestions(category, questionsByCategory.get(category)));
              if(choices.size() == AUTO_COMPLETE_SIZE) break;
            }
          }
          return choices;
        }

        @Override
        protected String getChoiceValue(CategoryWithQuestions categoryWithQuestions) throws Throwable {
          return categoryWithQuestions.toString();
        }

      };
      categoryNameFinder.setOutputMarkupId(true);
      categoryNameFinder.setLabel(new ResourceModel("CategoryName"));

      form.add(categoryNameFinder);
      form.add(new SimpleFormComponentLabel("categoryLabel", categoryNameFinder));

      AjaxSubmitLink addLink = new AjaxSubmitLink("link", form) {
        @SuppressWarnings("unchecked")
        @Override
        protected void onSubmit(AjaxRequestTarget target, Form<?> form1) {
          CategoryWithQuestions categoryWithQuestions = categoryNameFinder.findChoice();
          if(categoryWithQuestions == null) {
            error(new StringResourceModel("CategoryDoesNotExist", CategoryListPanel.this, null).getObject());
            return;
          }
          String name = categoryWithQuestions.getCategory().getName();
          if(checkIfCategoryAlreadyExists(((IModel<EditedQuestion>) CategoryListPanel.this.getDefaultModel()).getObject().getElement(), name)) {
            error(new StringResourceModel("CategoryAlreadyExists", CategoryListPanel.this, null).getObject());
            return;
          }
          Category category = categoryWithQuestions.getCategory();
          addCategory(((IModel<EditedQuestion>) CategoryListPanel.this.getDefaultModel()).getObject().getElement(), category);
          categoryNameFinder.setModelObject(null);
          sharedCategories.add(category);

          target.addComponent(categoryNameFinder);
          target.addComponent(categoryList);
        }

        @Override
        protected void onError(AjaxRequestTarget target, Form<?> form1) {
          feedbackWindow.setContent(feedbackPanel);
          feedbackWindow.show(target);
        }
      };

      addLink.add(new Image("img", Images.ADD).add(new AttributeModifier("title", true, new ResourceModel("Add"))));
      form.add(addLink);
    }

    private class CategoryWithQuestions implements Serializable {

      private Category category;

      private Collection<Question> questions;

      public CategoryWithQuestions(Category category, Collection<Question> questions) {
        this.category = category;
        this.questions = questions;
      }

      public Category getCategory() {
        return category;
      }

      @Override
      public String toString() {
        StringBuilder sb = new StringBuilder();
        for(Question q : questions) {
          if(sb.length() > 0) sb.append(", ");
          sb.append(q.getName());
        }
        return category.getName() + " (" + StringUtils.abbreviate(sb.toString(), 50) + ")";
      }
    }
  }

  private boolean checkIfCategoryAlreadyExists(Question question, String name) {
    for(QuestionCategory questionCategory : question.getQuestionCategories()) {
      if(equalsIgnoreCase(questionCategory.getName(), name) || equalsIgnoreCase(questionCategory.getCategory().getName(), name)) {
        return true; // category already exists
      }
    }
    return false;
  }

  private void addCategory(Question question, String name) {
    if(StringUtils.isNotBlank(name) && !checkIfCategoryAlreadyExists(question, name)) {
      addCategory(question, new Category(name));
    }
  }

  private void addCategory(Question question, Category category) {
    QuestionCategory questionCategory = new QuestionCategory();
    questionCategory.setCategory(category);
    question.addQuestionCategory(questionCategory);
  }

}
