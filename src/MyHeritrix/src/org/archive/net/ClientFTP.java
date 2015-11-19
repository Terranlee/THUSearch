/* ClientFTP.java
 *
 * $Id: ClientFTP.java 6662 2009-11-17 02:27:20Z nlevitt $
 *
 * Created on Jun 5, 2003
 *
 * Copyright (C) 2003 Internet Archive.
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
package org.archive.net;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.Socket;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.LineIterator;
import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.ftp.FTPClient;

/**
 * Client for FTP operations. Saves the commands sent to the server and replies
 * received, which can be retrieved with {@link #getControlConversation()}.
 * 
 * @author pjack
 * @author nlevitt
 */
public class ClientFTP extends FTPClient implements ProtocolCommandListener {

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    // Records the conversation on the ftp control channel. The format is based on "curl -v".
    protected StringBuilder controlConversation;
    protected Socket dataSocket;

    /**
     * Constructs a new <code>ClientFTP</code>.
     */
    public ClientFTP() {
        controlConversation = new StringBuilder();
        addProtocolCommandListener(this);
    }

    /**
     * Opens a data connection.
     * 
     * @param command
     *            the data command (eg, RETR or LIST)
     * @param path
     *            the path of the file to retrieve
     * @return the socket to read data from, or null if server says not found,
     *         permission denied, etc
     * @throws IOException
     *             if a network error occurs
     */
    public Socket openDataConnection(int command, String path)
    throws IOException {
        try {
            dataSocket = _openDataConnection_(command, path);
            if (dataSocket != null) {
                recordAdditionalInfo("Opened data connection to "
                        + dataSocket.getInetAddress().getHostAddress() + ":"
                        + dataSocket.getPort());
            }
            return dataSocket;
        } catch (IOException e) {
            if (getPassiveHost() != null) {
                recordAdditionalInfo("Failed to open data connection to "
                        + getPassiveHost() + ":" + getPassivePort() + ": "
                        + e.getMessage());
            } else {
                recordAdditionalInfo("Failed to open data connection: "
                        + e.getMessage());
            }
            throw e;
        }
    }

    public void closeDataConnection() {
        if (dataSocket != null) {
            String dataHostPort = dataSocket.getInetAddress().getHostAddress()
                    + ":" + dataSocket.getPort();
            try {
                dataSocket.close();
                recordAdditionalInfo("Closed data connection to "
                        + dataHostPort);
            } catch (IOException e) {
                recordAdditionalInfo("Problem closing data connection to "
                        + dataHostPort + ": " + e.getMessage());
            }
        }
    }

    protected void _connectAction_() throws IOException {
        try {
            recordAdditionalInfo("Opening control connection to "
                    + getRemoteAddress().getHostAddress() + ":"
                    + getRemotePort());
            super._connectAction_();
        } catch (IOException e) {
            recordAdditionalInfo("Failed to open control connection to "
                    + getRemoteAddress().getHostAddress() + ":"
                    + getRemotePort() + ": " + e.getMessage());
            throw e;
        }
    }
    
    public void disconnect() throws IOException {
        String remoteHostPort = getRemoteAddress().getHostAddress() + ":"
                + getRemotePort();
        super.disconnect();
        recordAdditionalInfo("Closed control connection to " + remoteHostPort);
    }

    public String getControlConversation() {
        return controlConversation.toString();
    }    

    private class IterableLineIterator extends LineIterator implements Iterable<String> {
        public IterableLineIterator(final Reader reader) throws IllegalArgumentException {
            super(reader);
        }
        @SuppressWarnings("unchecked")
        public Iterator<String> iterator() {
            return this;
        }
    }
    
    protected void recordControlMessage(String linePrefix, String message) {
        for (String line: new IterableLineIterator(new BufferedReader(new StringReader(message)))) {
            controlConversation.append(linePrefix);
            controlConversation.append(line);
            controlConversation.append(NETASCII_EOL);
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest(linePrefix + line);
            }
        }
    }

    public void protocolCommandSent(ProtocolCommandEvent event) {
        recordControlMessage("> ", event.getMessage());
    }

    public void protocolReplyReceived(ProtocolCommandEvent event) {
        recordControlMessage("< ", event.getMessage());
    }

    // for noting things like successful/unsuccessful connection to data port
    private void recordAdditionalInfo(String message) {
        recordControlMessage("* ", message);
    }

    // XXX see https://issues.apache.org/jira/browse/NET-257
    @Override
    public String[] getReplyStrings() {
        return _replyLines.toArray(new String[0]);
    }
}
