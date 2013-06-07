/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.database.dao;

import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Service;
import org.yamj.common.type.StatusType;
import org.yamj.core.database.model.Person;
import org.yamj.core.database.model.dto.CreditDTO;
import org.yamj.core.hibernate.HibernateDao;

@Service("personDao")
public class PersonDao extends HibernateDao {

    public void storePerson(CreditDTO dto) {
        Session session = getSession();
        Criteria criteria = session.createCriteria(Person.class);
        criteria.setLockMode(LockMode.PESSIMISTIC_WRITE);
        criteria.add(Restrictions.eq("name", dto.getName()));
        Person person = (Person)criteria.uniqueResult();
        if (person == null) {
            // create new person
            person = new Person();
            person.setName(dto.getName());
            person.setPersonId(dto.getSourcedb(), dto.getSourcedbId());
            person.setStatus(StatusType.NEW);
            session.save(person);
        } else {
            // update person if ID has has been set
            if (person.setPersonId(dto.getSourcedb(), dto.getSourcedbId())) {
                session.update(person);
            }
        }
    }
}
