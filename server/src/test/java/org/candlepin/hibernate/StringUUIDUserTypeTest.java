package org.candlepin.hibernate;

import static org.junit.Assert.assertEquals;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Persistence;
import javax.persistence.Table;

/**
 * Copyright (c) 2009 - 2018 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License, version 2 (GPLv2). There is NO
 * WARRANTY for this software, express or implied, including the implied warranties of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2 along with this software; if
 * not, see http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is granted to use or replicate Red Hat
 * trademarks that are incorporated in this software or its documentation.
 */

/**
 * Test for StringUUIDUserType
 */
public class StringUUIDUserTypeTest {
    private EntityManagerFactory emf;
    private EntityManager em;

    @Entity
    @Table(name = "Thing")
    public static class Thing {
        @Id
        @GeneratedValue
        @Column(columnDefinition = "VARCHAR(32)")
        @Type(type = "org.candlepin.hibernate.StringUUIDUserType")
        private UUID id;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }
    }

    @Before
    public void setUp() throws Exception {
        emf =  Persistence.createEntityManagerFactory("testingUserType");
        em = emf.createEntityManager();
    }

    @After
    public void tearDown() throws Exception {
        em.close();
        emf.close();
    }

    @Test
    public void testWritesUUIDWithNoHyphens() throws Exception {
        Thing t = new Thing();

        em.getTransaction().begin();
        em.persist(t);
        em.flush();
        em.getTransaction().commit();
        em.clear();

        UUID actual = t.getId();
        List l = em.createQuery("FROM StringUUIDUserTypeTest$Thing").getResultList();
        assertEquals(1, l.size());
        assertEquals(actual, ((Thing) l.get(0)).getId());
    }

    @Test
    public void testReadsLegacyUUIDWithNoHyphens() throws Exception {
        String legacyId = "8a8d0197624a170801624a1873e80952";
        em.getTransaction().begin();
        em.createNativeQuery("INSERT INTO Thing (id) VALUES (?)")
            .setParameter(1, legacyId)
            .executeUpdate();
        em.getTransaction().commit();
        em.clear();

        List l = em.createQuery("FROM StringUUIDUserTypeTest$Thing").getResultList();
        assertEquals(1, l.size());
        Thing t = (Thing) l.get(0);
        assertEquals(legacyId, t.getId().toString().replace("-", ""));
    }
}
