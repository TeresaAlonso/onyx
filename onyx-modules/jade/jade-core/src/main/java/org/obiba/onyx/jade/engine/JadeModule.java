/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.onyx.jade.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.protocol.http.WebApplication;
import org.obiba.magma.VariableValueSource;
import org.obiba.magma.VariableValueSourceFactory;
import org.obiba.onyx.core.domain.participant.Interview;
import org.obiba.onyx.core.domain.participant.Participant;
import org.obiba.onyx.core.service.ActiveInterviewService;
import org.obiba.onyx.engine.Module;
import org.obiba.onyx.engine.Stage;
import org.obiba.onyx.engine.state.AbstractStageState;
import org.obiba.onyx.engine.state.IStageExecution;
import org.obiba.onyx.engine.state.StageExecutionContext;
import org.obiba.onyx.engine.state.TransitionEvent;
import org.obiba.onyx.engine.variable.IVariablePathNamingStrategy;
import org.obiba.onyx.engine.variable.IVariableProvider;
import org.obiba.onyx.engine.variable.Variable;
import org.obiba.onyx.engine.variable.VariableData;
import org.obiba.onyx.engine.variable.VariableHelper;
import org.obiba.onyx.jade.core.domain.instrument.InstrumentType;
import org.obiba.onyx.jade.core.service.InstrumentRunService;
import org.obiba.onyx.jade.core.service.InstrumentService;
import org.obiba.onyx.jade.core.wicket.workstation.WorkstationPanel;
import org.obiba.onyx.jade.engine.variable.IInstrumentTypeToVariableMappingStrategy;
import org.obiba.onyx.jade.magma.InstrumentRunBeanResolver;
import org.obiba.onyx.jade.magma.InstrumentRunVariableValueSourceFactory;
import org.obiba.onyx.magma.OnyxAttributeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.google.common.collect.ImmutableSet;

public class JadeModule implements Module, IVariableProvider, VariableValueSourceFactory, ApplicationContextAware {

  private static final Logger log = LoggerFactory.getLogger(JadeModule.class);

  private ApplicationContext applicationContext;

  private ActiveInterviewService activeInterviewService;

  private InstrumentService instrumentService;

  private InstrumentRunService instrumentRunService;

  private IInstrumentTypeToVariableMappingStrategy instrumentTypeToVariableMappingStrategy;

  // private DatabaseSeed databaseSeed;

  private List<Stage> stages;

  @Autowired(required = true)
  private InstrumentRunBeanResolver resolver;

  @Autowired(required = true)
  private OnyxAttributeHelper attributeHelper;

  public String getName() {
    return "jade";
  }

  public void initialize(WebApplication application) {
  }

  public void shutdown(WebApplication application) {
    log.info("shutdown");
  }

  public IStageExecution createStageExecution(Interview interview, Stage stage) {
    StageExecutionContext exec = (StageExecutionContext) applicationContext.getBean("stageExecutionContext");
    exec.setStage(stage);
    exec.setInterview(interview);

    AbstractStageState ready = (AbstractStageState) applicationContext.getBean("jadeReadyState");
    AbstractStageState inProgress = (AbstractStageState) applicationContext.getBean("jadeInProgressState");
    AbstractStageState skipped = (AbstractStageState) applicationContext.getBean("jadeSkippedState");
    AbstractStageState completed = (AbstractStageState) applicationContext.getBean("jadeCompletedState");
    AbstractStageState notApplicable = (AbstractStageState) applicationContext.getBean("jadeNotApplicableState");
    AbstractStageState contraIndicated = (AbstractStageState) applicationContext.getBean("jadeContraIndicatedState");
    AbstractStageState waiting = (AbstractStageState) applicationContext.getBean("jadeWaitingState");
    AbstractStageState interrupted = (AbstractStageState) applicationContext.getBean("jadeInterruptedState");

    exec.addEdge(ready, TransitionEvent.NOTAPPLICABLE, notApplicable);
    exec.addEdge(ready, TransitionEvent.START, inProgress);
    exec.addEdge(ready, TransitionEvent.SKIP, skipped);
    exec.addEdge(ready, TransitionEvent.CONTRAINDICATED, notApplicable);

    exec.addEdge(inProgress, TransitionEvent.CANCEL, ready);
    exec.addEdge(inProgress, TransitionEvent.COMPLETE, completed);
    exec.addEdge(inProgress, TransitionEvent.NOTAPPLICABLE, notApplicable);
    exec.addEdge(inProgress, TransitionEvent.CONTRAINDICATED, contraIndicated);
    exec.addEdge(inProgress, TransitionEvent.INTERRUPT, interrupted);

    exec.addEdge(skipped, TransitionEvent.CANCEL, ready);
    exec.addEdge(skipped, TransitionEvent.NOTAPPLICABLE, notApplicable);

    exec.addEdge(completed, TransitionEvent.CANCEL, ready);
    exec.addEdge(completed, TransitionEvent.NOTAPPLICABLE, notApplicable);
    exec.addEdge(completed, TransitionEvent.CONTRAINDICATED, notApplicable);

    exec.addEdge(contraIndicated, TransitionEvent.CANCEL, ready);
    exec.addEdge(contraIndicated, TransitionEvent.INVALID, waiting);

    exec.addEdge(notApplicable, TransitionEvent.VALID, ready);
    exec.addEdge(notApplicable, TransitionEvent.INVALID, waiting);

    exec.addEdge(waiting, TransitionEvent.NOTAPPLICABLE, notApplicable);
    exec.addEdge(waiting, TransitionEvent.CONTRAINDICATED, notApplicable);
    exec.addEdge(waiting, TransitionEvent.VALID, ready);
    exec.addEdge(ready, TransitionEvent.INVALID, waiting);
    exec.addEdge(skipped, TransitionEvent.INVALID, waiting);
    exec.addEdge(completed, TransitionEvent.INVALID, waiting);

    exec.addEdge(interrupted, TransitionEvent.CANCEL, ready);
    exec.addEdge(interrupted, TransitionEvent.RESUME, inProgress);
    exec.addEdge(interrupted, TransitionEvent.NOTAPPLICABLE, notApplicable);
    exec.addEdge(interrupted, TransitionEvent.INVALID, waiting);

    if(stage.getStageDependencyCondition() == null) {
      exec.setInitialState(ready);
    } else {
      if(stage.getStageDependencyCondition().isDependencySatisfied(stage, activeInterviewService) == null) {
        exec.setInitialState(waiting);
      } else if(stage.getStageDependencyCondition().isDependencySatisfied(stage, activeInterviewService) == true) {
        exec.setInitialState(ready);
      } else {
        exec.setInitialState(notApplicable);
      }
    }
    return exec;
  }

  public List<Stage> getStages() {
    return stages;
  }

  public void setStages(List<Stage> stages) {
    this.stages = stages;
  }

  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  public void setActiveInterviewService(ActiveInterviewService activeInterviewService) {
    this.activeInterviewService = activeInterviewService;
  }

  public void setInstrumentService(InstrumentService instrumentService) {
    this.instrumentService = instrumentService;
  }

  public InstrumentRunService getInstrumentRunService() {
    return instrumentRunService;
  }

  public void setInstrumentRunService(InstrumentRunService instrumentRunService) {
    this.instrumentRunService = instrumentRunService;
  }

  public void setInstrumentTypeToVariableMappingStrategy(IInstrumentTypeToVariableMappingStrategy instrumentTypeToVariableMappingStrategy) {
    this.instrumentTypeToVariableMappingStrategy = instrumentTypeToVariableMappingStrategy;
  }

  public VariableData getVariableData(Participant participant, Variable variable, IVariablePathNamingStrategy variablePathNamingStrategy) {
    VariableData varData = new VariableData(variablePathNamingStrategy.getPath(variable));
    varData = instrumentTypeToVariableMappingStrategy.getVariableData(participant, variable, variablePathNamingStrategy, varData);

    return varData;
  }

  public List<Variable> getVariables() {
    List<Variable> entities = new ArrayList<Variable>();

    instrumentTypeToVariableMappingStrategy.setVariableHelper(new VariableHelper(applicationContext));

    for(InstrumentType type : instrumentService.getInstrumentTypes().values()) {
      entities.add(instrumentTypeToVariableMappingStrategy.getVariable(type));
    }

    return entities;
  }

  public List<Variable> getContributedVariables(Variable root, IVariablePathNamingStrategy variablePathNamingStrategy) {
    return null;
  }

  public Component getWidget(String id) {
    return new WorkstationPanel(id);
  }

  public boolean isInteractive() {
    return true;
  }

  public void delete(Participant participant) {
    instrumentRunService.deleteAllInstrumentRuns(participant);
  }

  public void setResolver(InstrumentRunBeanResolver resolver) {
    this.resolver = resolver;
  }

  public void setAttributeHelper(OnyxAttributeHelper attributeHelper) {
    this.attributeHelper = attributeHelper;
  }

  //
  // VariableValueSourceFactory Methods
  //

  public Set<VariableValueSource> createSources(String collection) {
    ImmutableSet.Builder<VariableValueSource> sources = new ImmutableSet.Builder<VariableValueSource>();

    InstrumentRunVariableValueSourceFactory factory = new InstrumentRunVariableValueSourceFactory();
    factory.setInstrumentService(instrumentService);
    factory.setAttributeHelper(attributeHelper);
    sources.addAll(factory.createSources(collection, resolver));

    return sources.build();
  }
}
