/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.common.resteasy.filter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import org.candlepin.common.exceptions.BadRequestException;
import org.candlepin.common.guice.CommonI18nProvider;
import org.candlepin.common.paging.PageRequest;

import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.xnap.commons.i18n.I18n;

import java.util.Locale;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;

/**
 * PageRequestFilterTest
 */
public class PageRequestFilterTest {

    @Inject @Mock
    private HttpServletRequest mockServletReq;

    private javax.inject.Provider<I18n> i18nProvider;
    private PageRequestFilter interceptor;

    private MockHttpRequest mockReq;
    @Mock private ContainerRequestContext mockRequestContext;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(mockServletReq.getLocale()).thenReturn(Locale.US);
        this.i18nProvider = new CommonI18nProvider(this.mockServletReq);
        interceptor = new PageRequestFilter(this.i18nProvider);
    }

    @Test
    public void testNoAnything() throws Exception {
        mockReq = MockHttpRequest.create("GET",
            "http://localhost/candlepin/status");
        when(mockRequestContext.getUriInfo()).thenReturn(mockReq.getUri());

        interceptor.filter(mockRequestContext);

        PageRequest p = ResteasyProviderFactory.getContextData(PageRequest.class);
        assertNull(p);
    }

    @Test
    public void testBothLimitAndPage() throws Exception {
        mockReq = MockHttpRequest.create("GET",
            "http://localhost/candlepin/status?per_page=10&page=4");
        when(mockRequestContext.getUriInfo()).thenReturn(mockReq.getUri());

        interceptor.filter(mockRequestContext);

        PageRequest p = ResteasyProviderFactory.getContextData(PageRequest.class);
        assertEquals(Integer.valueOf(10), p.getPerPage());
        assertEquals(Integer.valueOf(4), p.getPage());
        assertEquals(PageRequest.DEFAULT_ORDER, p.getOrder());
        assertNull(p.getSortBy());
    }

    @Test
    public void testNoLimitButPage() throws Exception {
        mockReq = MockHttpRequest.create("GET",
            "http://localhost/candlepin/status?page=5");
        when(mockRequestContext.getUriInfo()).thenReturn(mockReq.getUri());

        interceptor.filter(mockRequestContext);

        PageRequest p = ResteasyProviderFactory.getContextData(PageRequest.class);
        assertEquals(PageRequest.DEFAULT_PER_PAGE, p.getPerPage());
        assertEquals(Integer.valueOf(5), p.getPage());
        assertEquals(PageRequest.DEFAULT_ORDER, p.getOrder());
        assertNull(p.getSortBy());
    }

    @Test
    public void testLimitButNoPage() throws Exception {
        mockReq = MockHttpRequest.create("GET",
            "http://localhost/candlepin/status?per_page=10");
        when(mockRequestContext.getUriInfo()).thenReturn(mockReq.getUri());

        interceptor.filter(mockRequestContext);

        PageRequest p = ResteasyProviderFactory.getContextData(PageRequest.class);
        assertEquals(Integer.valueOf(10), p.getPerPage());
        assertEquals(PageRequest.DEFAULT_PAGE, p.getPage());
        assertEquals(PageRequest.DEFAULT_ORDER, p.getOrder());
        assertNull(p.getSortBy());
    }

    @Test(expected = BadRequestException.class)
    public void testBadIntegerValue() throws Exception {
        mockReq = MockHttpRequest.create("GET",
            "http://localhost/candlepin/status?page=foo&per_page=456");
        when(mockRequestContext.getUriInfo()).thenReturn(mockReq.getUri());

        interceptor.filter(mockRequestContext);
    }

    @Test(expected = BadRequestException.class)
    public void testDoesNotAllowPageZero() throws Exception {
        mockReq = MockHttpRequest.create("GET",
            "http://localhost/candlepin/status?page=0&per_page=456");
        when(mockRequestContext.getUriInfo()).thenReturn(mockReq.getUri());

        interceptor.filter(mockRequestContext);
    }

    @Test
    public void testNoPagingIfJustOrderAndSortBy() throws Exception {
        mockReq = MockHttpRequest.create("GET",
            "http://localhost/candlepin/status?order=asc&sort_by=id");
        when(mockRequestContext.getUriInfo()).thenReturn(mockReq.getUri());

        interceptor.filter(mockRequestContext);

        PageRequest p = ResteasyProviderFactory.getContextData(PageRequest.class);
        assertFalse(p.isPaging());
        assertEquals(PageRequest.Order.ASCENDING, p.getOrder());
        assertEquals("id", p.getSortBy());
    }

    @Test
    public void testUsesDefaultOrderIfNoOrderProvided() throws Exception {
        mockReq = MockHttpRequest.create("GET",
            "http://localhost/candlepin/status?sort_by=id");
        when(mockRequestContext.getUriInfo()).thenReturn(mockReq.getUri());

        interceptor.filter(mockRequestContext);

        PageRequest p = ResteasyProviderFactory.getContextData(PageRequest.class);
        assertFalse(p.isPaging());
        assertEquals(PageRequest.DEFAULT_ORDER, p.getOrder());
        assertEquals("id", p.getSortBy());
    }

    @Test
    public void testDescendingOrder() throws Exception {
        mockReq = MockHttpRequest.create("GET",
            "http://localhost/candlepin/status?order=descending&sort_by=id");
        when(mockRequestContext.getUriInfo()).thenReturn(mockReq.getUri());

        interceptor.filter(mockRequestContext);

        PageRequest p = ResteasyProviderFactory.getContextData(PageRequest.class);
        assertFalse(p.isPaging());
        assertEquals(PageRequest.Order.DESCENDING, p.getOrder());
        assertEquals("id", p.getSortBy());
    }
}