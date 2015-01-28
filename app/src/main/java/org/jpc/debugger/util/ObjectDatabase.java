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


package org.jpc.debugger.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ObjectDatabase
{
    private Map<Class, Object> table;

    public ObjectDatabase()
    {
        table = new HashMap<>();
    }
    
    public synchronized boolean addObject(@Nullable Object value)
    {
        if (value == null)
            return false;

        Class cls = (Class) value.getClass();

        if (table.containsKey(cls))
            return false;
        
        table.put(cls, value);
        return true;
    }

    public synchronized Object getObject(Class cls)
    {
        return table.get(cls);
    }

    @Nullable
    public synchronized Object removeObject(@Nullable Object obj)
    {
        if (obj == null)
            return null;
        
        return removeObject(obj.getClass());
    }

    @Nullable
    synchronized Object removeObject(@Nullable Class cls)
    {
        if (cls == null)
            return null;

        return table.remove(cls);
    }

    @NonNull
    public synchronized List<Object> entries()
    {
        return new ArrayList<>(table.values());
    }
}
