/*
    JPC: An x86 PC Hardware Emulator for a pure Java Virtual Machine
    Release Version 2.4

    A project from the Physics Dept, The University of Oxford

    Copyright (C) 2007-2010 The University of Oxford

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License version 2 as published by
    the Free Software Foundation.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 
    Details (including contact information) can be found at: 

    jpc.sourceforge.net
    or the developer website
    sourceforge.net/projects/jpc/

    Conceived and Developed by:
    Rhys Newman, Ian Preston, Chris Dennis

    End of licence header
*/

package org.jpc.support;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Ian Preston
 */
public class EthernetHub extends EthernetOutput
{
    private int errorDelay, port;
    private String serverHost;
    @Nullable
    private volatile DataOutputStream out;
    @Nullable
    private volatile DataInputStream in;
    @NonNull
    private Queue<byte[]> inQueue = new ConcurrentLinkedQueue<>();
    private final Queue<byte[]> outQueue = new ConcurrentLinkedQueue<>();

    public EthernetHub(String host, int port)
    {
        int packetSize = -1;
        serverHost = host;
        this.port = port;
        errorDelay = 10000;
        System.out.println("Connecting to remote EthernetHub at: " + host + ":" + port);
        
        new Thread(new Reader()).start();
        new Thread(new Writer()).start();
    }

    private synchronized void reconnect()
    {
        notifyAll();
        if (in != null)
            return;

        in = null;
        out = null;
        while (true)
        {
            try
            {
                Socket server = new Socket(serverHost, port);
                server.setTcpNoDelay(true);
                server.setPerformancePreferences(0, 2, 1);
                
                out = new DataOutputStream(server.getOutputStream());
                in = new DataInputStream(server.getInputStream());
                errorDelay = 1000;
                return;
            }
            catch (Exception e)
            {
                errorDelay = Math.min(errorDelay+2000, 30000);
                System.out.println("Error connecting to ethernet hub: "+e);
                try
                {
                    wait(errorDelay);
                }
                catch (Exception ignored) {}
            }
        }
    }

    private class Reader implements Runnable
    {
        public void run()
        {
            while (true) 
            {
                if (inQueue.size() > 100)
                {
                    System.out.println("Clearing ETH0 packet queue");
                    inQueue.clear();
                }

                try
                {
                    int size = in.readInt();
                    System.out.println(">>> "+size);
                    byte[] packet = new byte[size];
                    in.readFully(packet);
                    inQueue.add(packet);
                } 
                catch (Exception ex)
                {
                    in = null;
                    out = null;
                    reconnect();
                }
            }
        }
    }

    private class Writer implements Runnable
    {
        public void run()
        {
            while (true) 
            {
                try
                {
                    synchronized (outQueue)
                    {
                        while (outQueue.size() == 0)
                            outQueue.wait();
                    }

                    byte[] packet = outQueue.poll();
                    out.writeInt(packet.length);
                    out.write(packet);
                    out.flush();
                } 
                catch (Exception ex)
                {
                    in = null;
                    out = null;
                    reconnect();
                }
            }
        }
    }

    public byte[] getPacket()
    {
        byte[] packet = inQueue.poll();
        return packet;
    }

    public void sendPacket(@NonNull byte[] data, int offset, int length)
    {
        byte[] p = new byte[length];
        System.arraycopy(data, offset, p, 0, length);
        
        synchronized (outQueue)
        {
            outQueue.add(p);
            outQueue.notify();
        }
    }
}
