/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.athento.nuxeo.module;

import org.nuxeo.ecm.core.api.NuxeoException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This servlet is bound to /api/athento and dispatch calls to /site/api/athento in oder to have better looking URLs.
 *
 * @since 8.10
 */
public class ExtendedServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String toReplace = req.getContextPath() + "/api/athento";
        String newPath = req.getRequestURI();
        if (newPath.startsWith(toReplace)) {
            newPath = "/site/api/athento" + newPath.substring(toReplace.length());
        } else {
            throw new NuxeoException("Cannot forward " + newPath);
        }
        RequestDispatcher rd = req.getRequestDispatcher(newPath);
        rd.forward(req, resp);
    }

}
