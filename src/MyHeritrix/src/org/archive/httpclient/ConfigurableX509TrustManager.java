/* ConfigurableX509TrustManager
 *
 * Created on Feb 18, 2004
 *
 * Copyright (C) 2004 Internet Archive.
 *
 * This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 * Heritrix is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * Heritrix is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with Heritrix; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.archive.httpclient;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * A configurable trust manager built on X509TrustManager.
 *
 * If set to 'open' trust, the default, will get us into sites for whom we do
 * not have the CA or any of intermediary CAs that go to make up the cert chain
 * of trust.  Will also get us past selfsigned and expired certs.  'loose'
 * trust will get us into sites w/ valid certs even if they are just
 * selfsigned.  'normal' is any valid cert not including selfsigned.  'strict'
 * means cert must be valid and the cert DN must match server name.
 *
 * <p>Based on pointers in
 * <a href="http://jakarta.apache.org/commons/httpclient/sslguide.html">SSL
 * Guide</a>,
 * and readings done in <a
 * href="http://java.sun.com/j2se/1.4.2/docs/guide/security/jsse/JSSERefGuide.html#Introduction">JSSE
 * Guide</a>.
 *
 * <p>TODO: Move to an ssl subpackage when we have other classes other than
 * just this one.
 *
 * @author stack
 * @version $Id: ConfigurableX509TrustManager.java 4232 2006-05-15 21:52:30Z stack-sf $
 */
public class ConfigurableX509TrustManager implements X509TrustManager
{
    /**
     * Logging instance.
     */
    protected static Logger logger = Logger.getLogger(
        "org.archive.httpclient.ConfigurableX509TrustManager");

    /**
     * Trust anything given us.
     *
     * Default setting.
     *
     * <p>See <a href="http://javaalmanac.com/egs/javax.net.ssl/TrustAll.html">
     *  e502. Disabling Certificate Validation in an HTTPS Connection</a> from
     * the java almanac for how to trust all.
     */
    public final static String OPEN = "open";

    /**
     * Trust any valid cert including self-signed certificates.
     */
    public final static String LOOSE = "loose";

    /**
     * Normal jsse behavior.
     *
     * Seemingly any certificate that supplies valid chain of trust.
     */
    public final static String NORMAL = "normal";

    /**
     * Strict trust.
     *
     * Ensure server has same name as cert DN.
     */
    public final static String STRICT = "strict";

    /**
     * All the levels of trust as an array from babe-in-the-wood to strict.
     */
    public static String [] LEVELS_AS_ARRAY = {OPEN, LOOSE, NORMAL, STRICT};

    /**
     * Levels as a list.
     */
    private static List LEVELS = Arrays.asList(LEVELS_AS_ARRAY);

    /**
     * Default setting for trust level.
     */
    public final static String DEFAULT = OPEN;

    /**
     * Trust level.
     */
    private String trustLevel = DEFAULT;


    /**
     * An instance of the SUNX509TrustManager that we adapt variously
     * depending upon passed configuration.
     *
     * We have it do all the work we don't want to.
     */
    private X509TrustManager standardTrustManager = null;


    public ConfigurableX509TrustManager()
    throws NoSuchAlgorithmException, KeyStoreException {
        this(DEFAULT);
    }

    /**
     * Constructor.
     *
     * @param level Level of trust to effect.
     *
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     */
    public ConfigurableX509TrustManager(String level)
    throws NoSuchAlgorithmException, KeyStoreException {
        super();
        TrustManagerFactory factory = TrustManagerFactory.
            getInstance(TrustManagerFactory.getDefaultAlgorithm());

        // Pass in a null (Trust) KeyStore.  Null says use the 'default'
        // 'trust' keystore (KeyStore class is used to hold keys and to hold
        // 'trusts' (certs)). See 'X509TrustManager Interface' in this doc:
        // http://java.sun.com
        // /j2se/1.4.2/docs/guide/security/jsse/JSSERefGuide.html#Introduction
        factory.init((KeyStore)null);
        TrustManager[] trustmanagers = factory.getTrustManagers();
        if (trustmanagers.length == 0) {
            throw new NoSuchAlgorithmException(TrustManagerFactory.
                getDefaultAlgorithm() + " trust manager not supported");
        }
        this.standardTrustManager = (X509TrustManager)trustmanagers[0];

        this.trustLevel =
            (LEVELS.contains(level.toLowerCase()))? level: DEFAULT;
    }

    public void checkClientTrusted(X509Certificate[] certificates, String type)
    throws CertificateException {
        if (this.trustLevel.equals(OPEN)) {
            return;
        }

        this.standardTrustManager.checkClientTrusted(certificates, type);
    }

    public void checkServerTrusted(X509Certificate[] certificates, String type)
    throws CertificateException {
        if (this.trustLevel.equals(OPEN)) {
            return;
        }

        try {
            this.standardTrustManager.checkServerTrusted(certificates, type);
            if (this.trustLevel.equals(STRICT)) {
                logger.severe(STRICT + " not implemented.");
            }
        } catch (CertificateException e) {
            if (this.trustLevel.equals(LOOSE) &&
                certificates != null && certificates.length == 1)
            {
                    // If only one cert and its valid and it caused a
                    // CertificateException, assume its selfsigned.
                    X509Certificate certificate = certificates[0];
                    certificate.checkValidity();
            } else {
                // If we got to here, then we're probably NORMAL. Rethrow.
                throw e;
            }
        }
    }

    public X509Certificate[] getAcceptedIssuers() {
        return this.standardTrustManager.getAcceptedIssuers();
    }
}
