/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2012 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2012 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.ng.services.opennmsserverconfig;

import java.io.InputStream;

import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.opennms.netmgt.config.server.LocalServer;
import org.opennms.ng.util.xml.CastorUtils;

/**
 * <p>OpennmsServerConfigManager class.</p>
 */
public class OpennmsServerConfigManager implements OpennmsServerConfig {
    /**
     * The config class loaded from the config file
     */
    private LocalServer m_config;
    
    /**
     * <p>Constructor for OpennmsServerConfigManager.</p>
     *
     * @param is a {@link java.io.InputStream} object.
     * @throws org.exolab.castor.xml.MarshalException if any.
     * @throws org.exolab.castor.xml.ValidationException if any.
     */
    protected OpennmsServerConfigManager(final InputStream is) throws MarshalException, ValidationException {

        m_config = CastorUtils.unmarshal(LocalServer.class, is);
    }

    /**
     * Return the local opennms server name.
     *
     * @return the name of the local opennms server
     */
    @Override
    public String getServerName() {
        return m_config.getServerName();
    }

    /**
     * Return the default critical path IP
     *
     * @return the default critical path IP
     */
    @Override
    public String getDefaultCriticalPathIp() {
        return m_config.getDefaultCriticalPathIp();
    }

    /**
     * Return the default critical path service
     *
     * @return the default critical path service
     */
    @Override
    public String getDefaultCriticalPathService() {
        return m_config.getDefaultCriticalPathService();
    }

    /**
     * Return the default critical path timeout
     *
     * @return the default critical path timeout
     */
    @Override
    public int getDefaultCriticalPathTimeout() {
        return m_config.getDefaultCriticalPathTimeout();
    }

    /**
     * Return the default critical path retries
     *
     * @return the default critical path retries
     */
    @Override
    public int getDefaultCriticalPathRetries() {
        return m_config.getDefaultCriticalPathRetries();
    }

    /**
     * Return the boolean flag verify server to determine if poller what to use
     * server to restrict services to poll.
     *
     * @return boolean flag
     */
    @Override
    public boolean verifyServer() {
        return Boolean.valueOf(m_config.getVerifyServer());
    }

}
