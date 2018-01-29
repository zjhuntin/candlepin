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
package org.candlepin.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;


/**
 * PropertyUtil
 *
 * @param <T> type of property to get
 */
public class PropertyUtil<T> {

    private PropertyUtil() {
    }

    /**
     * Returns the value of the static property named field of the given Class
     * as a String. Returns null if the field is not static.
     * @param clazz class to interrogate.
     * @param field name of field.
     * @return the value of the static property named field of the given Class
     * as a String. Returns null if the field is not static.
     * @throws NoSuchFieldException thrown if field is not found.
     */
    public static <T> T getStaticProperty(Class clazz, String field)
        throws NoSuchFieldException {

        T value = null;
        try {
            Field f = clazz.getDeclaredField(field);
            if (Modifier.isStatic(f.getModifiers()) && f.get(clazz) != null) {
                value = (T) f.get(clazz);
            }
        }
        catch (SecurityException se) {
            throw new RuntimeException(se.getMessage(), se);
        }
        catch (IllegalArgumentException iae) {
            throw new RuntimeException(iae);
        }
        catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }

        return value;
    }

    /**
     * Returns the value of the static property named field of the given Class
     * as a String. Returns null if the field is not static.
     * @param cname class name to interrogate.
     * @param field name of field.
     * @return the value of the static property named field of the given Class
     * Returns null if the field is not static.
     * @throws NoSuchFieldException thrown if field is not found.
     * @throws ClassNotFoundException thrown if the class cname is not found.
     */
    public static <T> T getStaticProperty(String cname, String field)
        throws NoSuchFieldException, ClassNotFoundException {

        Class clazz = Class.forName(cname);
        return PropertyUtil.getStaticProperty(clazz, field);
    }
}
