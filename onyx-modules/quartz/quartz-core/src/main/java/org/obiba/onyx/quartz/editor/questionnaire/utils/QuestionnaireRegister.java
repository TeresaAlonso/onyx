/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.onyx.quartz.editor.questionnaire.utils;

import org.obiba.onyx.engine.ModuleRegistry;
import org.obiba.onyx.engine.Stage;
import org.obiba.onyx.engine.StageManager;
import org.obiba.onyx.quartz.core.engine.questionnaire.question.Questionnaire;
import org.obiba.onyx.quartz.engine.QuartzModule;
import org.springframework.beans.factory.annotation.Required;

public class QuestionnaireRegister {

  private ModuleRegistry moduleRegistry;

  public void register(Questionnaire questionnaire) {
    // create Stage if needed
    QuartzModule quartzModule = (QuartzModule) moduleRegistry.getModule(QuartzModule.MODULE_NAME);
    StageManager stageManager = quartzModule.getStageManager();
    Stage stage = stageManager.getStage(questionnaire.getName());
    if(stage == null) {
      quartzModule.addStage(stageManager.getStages().size(), new Stage(quartzModule, questionnaire.getName()));
      moduleRegistry.unregisterModule(QuartzModule.MODULE_NAME);
      moduleRegistry.registerModule(quartzModule);
    } else {
      quartzModule.stageChanged(stage);
    }
  }

  @Required
  public void setModuleRegistry(ModuleRegistry moduleRegistry) {
    this.moduleRegistry = moduleRegistry;
  }
}