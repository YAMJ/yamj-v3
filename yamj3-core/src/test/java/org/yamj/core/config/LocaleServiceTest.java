/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      YAMJ is free software: you can redistribute it and/or modify
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
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.config;

import java.util.Set;
import java.util.TreeSet;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.yamj.core.AbstractTest;

public class LocaleServiceTest extends AbstractTest {

    @Autowired
    private LocaleService localeService;

    @Test
    public void testFindCountryCode() {
        Set<String> countries = new TreeSet<>();
        countries.add("Allemagne");
        countries.add("U.S.A.");
        countries.add("France");
        countries.add("Italie");
        countries.add("Espagne");
        countries.add("Belgique");
        countries.add("République tchèque");
        countries.add("Grande-Bretagne");
        countries.add("United Kingdom");
        countries.add("United States of America");
        countries.add("Canada");
        countries.add("Israel");
        countries.add("Chine");
        countries.add("Irlande");
        countries.add("Mexique");
        countries.add("Russie");
        countries.add("Danemark");
        countries.add("Luxembourg");
        countries.add("Hong-Kong");
        countries.add("Nouvelle-Zélande");
        countries.add("Australie");
        countries.add("Slovénie");
        countries.add("Pologne");
        countries.add("Corée du Sud");
        countries.add("Suisse");
        countries.add("Finlande");
        countries.add("Japon");
        countries.add("Brésil");
        countries.add("Hongrie");
        countries.add("Singapour");
        countries.add("Pays-Bas");
        countries.add("Inde");
        countries.add("Afrique du Sud");
        countries.add("Bulgarie");
        countries.add("Argentine");
        countries.add("Czech Republic");
        countries.add("AALAND ISLAND");
        countries.add("BOSNIA AND HERZEGOWINA");
        countries.add("COTE D'IVOIRE");
        
        for (String country : countries) {
            String code = localeService.findCountryCode(country);
            Assert.assertNotNull(code);
        }
    }
}