/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.shiro.session.mgt.eis;

import org.apache.shiro.cache.HashtableCacheManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.util.JavaEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Random;


/**
 * Simple memory-based implementation of the SessionDAO that relies on its configured
 * {@link #setCacheManager CacheManager} for Session caching and in-memory persistence.
 * <p/>
 * <p><b>PLEASE NOTE</b> the default CacheManager internal to this implementation is a
 * {@link org.apache.shiro.cache.HashtableCacheManager HashtableCacheManager}, which IS NOT RECOMMENDED for production environments.
 * <p/>
 * <p>If you
 * want to use the MemorySessionDAO in production environments, such as those that require session data to be
 * recoverable in case of a server restart, you should do one of two things (or both):
 * <p/>
 * <ul>
 * <li>Configure it with a production-quality CacheManager. The
 * {@code org.apache.shiro.cache.ehcache.EhCacheManager} is one such implementation.  It is not used by default
 * to prevent a forced runtime dependency on ehcache.jar that may not be required in many environments)</li><br/>
 * <li>If you need session information beyond their transient start/stop lifetimes, you should subclass this one and
 * override the <tt>do*</tt> methods to perform CRUD operations using an EIS-tier API (e.g. Hibernate/JPA/JCR/etc).
 * This class implementation does not retain sessions after they have been stopped or expired, so you would need to
 * override these methods to ensure Sessions can be accessed beyond Shiro's needs.</li>
 * </ul>
 *
 * @author Les Hazlewood
 * @since 0.1
 */
public class MemorySessionDAO extends CachingSessionDAO {

    //TODO - complete JavaDoc

    private static final Logger log = LoggerFactory.getLogger(MemorySessionDAO.class);

    private static final String RANDOM_NUM_GENERATOR_ALGORITHM_NAME = "SHA1PRNG";
    private Random randomNumberGenerator = null;

    public MemorySessionDAO() {
        setCacheManager(new HashtableCacheManager());
    }

    private Random getRandomNumberGenerator() {
        if (randomNumberGenerator == null) {
            if (log.isInfoEnabled()) {
                String msg = "On Java 1.4 platforms and below, there is no built-in UUID class (Java 1.5 and above " +
                        "only) to use for Session ID generation - reverting to SecureRandom number generator.  " +
                        "Although this is probably sufficient for all but high user volume applications, if you " +
                        "see ID collision, you will want to upgrade to JDK 1.5 or better as soon as possible, or " +
                        "subclass the " + getClass().getName() + " class and override the #generateNewSessionId() " +
                        "method to use a better algorithm.";
                log.info(msg);
            }

            try {
                randomNumberGenerator = java.security.SecureRandom.getInstance(RANDOM_NUM_GENERATOR_ALGORITHM_NAME);
            } catch (java.security.NoSuchAlgorithmException e) {
                randomNumberGenerator = new java.security.SecureRandom();
            }
        }
        return randomNumberGenerator;
    }

    protected Serializable generateNewSessionId() {
        if (JavaEnvironment.isAtLeastVersion15()) {
            return java.util.UUID.randomUUID().toString();
        } else {
            return Long.toString(getRandomNumberGenerator().nextLong());
        }
    }

    protected Serializable doCreate(Session session) {
        Serializable sessionId = generateNewSessionId();
        assignSessionId(session, sessionId);
        return sessionId;
    }

    protected void assignSessionId(Session session, Serializable sessionId) {
        ((SimpleSession) session).setId(sessionId);
    }

    protected Session doReadSession(Serializable sessionId) {
        return null; //should never execute because this implementation relies on parent class to access cache, which
        //is where all sessions reside - it is the cache implementation that determines if the
        //cache is memory only or disk-persistent, etc.
    }

    protected void doUpdate(Session session) {
        //does nothing - parent class persists to cache.
    }

    protected void doDelete(Session session) {
        //does nothing - parent class removes from cache.
    }
}