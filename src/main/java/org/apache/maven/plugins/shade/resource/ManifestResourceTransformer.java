package org.apache.maven.plugins.shade.resource;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringInputStream;

/**
 * A resource processor that allows the arbitrary addition of attributes to
 * the first MANIFEST.MF that is found in the set of JARs being processed, or
 * to a newly created manifest for the shaded JAR.
 *  
 * @author Jason van Zyl
 */
public class ManifestResourceTransformer
    implements ResourceTransformer
{
    private static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";

    // Configuration
    private String mainClass;
    private Properties additionalAttributes;
    
    // Fields
    private boolean manifestDiscovered;
    private Manifest manifest;
    
    public boolean canTransformResource( String resource )
    {
        if ( MANIFEST_PATH.equalsIgnoreCase( resource ) )
        {
            return true;
        }

        return false;
    }

    public void processResource( InputStream is )
        throws IOException
    {
        // We just want to take the first manifest we come across as that's our project's manifest. This is the behavior now
        // which is situational at best. Right now there is no context passed in with the processing so we cannot tell what
        // artifact is being processed.
        if ( !manifestDiscovered )
        {
            manifest = new Manifest( is );
            manifestDiscovered = true;
        }
    }

    public boolean hasTransformedResource()
    {
        return true;
    }

    public void modifyOutputStream( JarOutputStream jos )
        throws IOException
    {
        // If we didn't find a manifest, then let's create one.
        if ( manifest == null )
        {            
            manifest = new Manifest();
        }

        Attributes attributes = manifest.getMainAttributes();
        
        if ( mainClass != null )
        {
            attributes.put( Attributes.Name.MAIN_CLASS, mainClass );                    
        }
        
        if ( additionalAttributes != null )
        {
            for ( Iterator i = additionalAttributes.keySet().iterator(); i.hasNext(); )
            {
                String key = (String) i.next();
                attributes.put( new Attributes.Name( key ), additionalAttributes.get( key ) );                                    
            }
        }
        
        jos.putNextEntry( new JarEntry( MANIFEST_PATH ) );
        StringBuffer sb = new StringBuffer();
        for ( Iterator i = attributes.keySet().iterator(); i.hasNext(); )
        {
            Attributes.Name a = (Attributes.Name) i.next();
            sb.append( a.toString() ).append(  ": " ).append( attributes.getValue(  a ) ).append( "\n" );
        }       
        InputStream is = new StringInputStream( sb.toString() );
        IOUtil.copy( is, jos );
        is.close();
    }    
}