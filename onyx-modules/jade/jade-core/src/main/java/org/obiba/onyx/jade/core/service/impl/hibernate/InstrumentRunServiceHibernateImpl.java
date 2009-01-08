/*******************************************************************************
 * Copyright 2008(c) The OBiBa Consortium. All rights reserved.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.onyx.jade.core.service.impl.hibernate;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.obiba.core.service.SortingClause;
import org.obiba.core.service.impl.hibernate.AssociationCriteria;
import org.obiba.core.service.impl.hibernate.AssociationCriteria.Operation;
import org.obiba.onyx.core.domain.participant.Participant;
import org.obiba.onyx.jade.core.domain.instrument.InstrumentType;
import org.obiba.onyx.jade.core.domain.run.InstrumentRun;
import org.obiba.onyx.jade.core.domain.run.InstrumentRunStatus;
import org.obiba.onyx.jade.core.domain.run.InstrumentRunValue;
import org.obiba.onyx.jade.core.service.impl.DefaultInstrumentRunServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class InstrumentRunServiceHibernateImpl extends DefaultInstrumentRunServiceImpl {

  private static final Logger log = LoggerFactory.getLogger(InstrumentRunServiceHibernateImpl.class);

  private SessionFactory factory;

  public void setSessionFactory(SessionFactory factory) {
    this.factory = factory;
  }

  private Session getSession() {
    return factory.getCurrentSession();
  }

  public InstrumentRun getLastInstrumentRun(Participant participant, InstrumentType instrumentType) {
    InstrumentRun template = new InstrumentRun();
    template.setInstrumentType(instrumentType);
    template.setParticipant(participant);
    List<InstrumentRun> runs = getPersistenceManager().match(template, SortingClause.create("timeEnd", false));
    if(runs != null && runs.size() > 0) {
      return runs.get(0);
    }
    return null;
  }

  public InstrumentRun getLastCompletedInstrumentRun(Participant participant, InstrumentType instrumentType) {
    Criteria criteria = AssociationCriteria.create(InstrumentRun.class, getSession()).add("instrumentType", Operation.eq, instrumentType).add("participant", Operation.eq, participant).addSortingClauses(new SortingClause("timeEnd", false)).getCriteria();
    criteria.add(Restrictions.or(Restrictions.eq("status", InstrumentRunStatus.COMPLETED), Restrictions.eq("status", InstrumentRunStatus.CONTRA_INDICATED)));

    return (InstrumentRun) criteria.setMaxResults(1).uniqueResult();
  }

  public InstrumentRunValue findInstrumentRunValue(Participant participant, InstrumentType instrumentType, String parameterCode) {
    InstrumentRunValue runValue = null;
    InstrumentRun run = getLastCompletedInstrumentRun(participant, instrumentType);

    if(run != null) {
      log.info("Run.id={} Param.code={}", run.getId(), parameterCode);
      runValue = (InstrumentRunValue) AssociationCriteria.create(InstrumentRunValue.class, getSession()).add("instrumentRun", Operation.eq, run).add("instrumentParameter.code", Operation.eq, parameterCode).getCriteria().uniqueResult();
    }

    return runValue;
  }

  public void setInstrumentRunStatus(InstrumentRun run, InstrumentRunStatus status) {
    run.setStatus(status);
    getPersistenceManager().save(run);
  }

}
