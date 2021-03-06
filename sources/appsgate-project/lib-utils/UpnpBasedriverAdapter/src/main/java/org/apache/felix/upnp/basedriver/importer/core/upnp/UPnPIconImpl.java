/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.upnp.basedriver.importer.core.upnp;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.cybergarage.upnp.Icon;

import org.osgi.service.upnp.UPnPIcon;

import org.apache.felix.upnp.basedriver.importer.util.HTTPRequestForIcon;
import org.apache.felix.upnp.basedriver.importer.util.ParseLocation;

/* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class UPnPIconImpl implements UPnPIcon {

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPIcon#getMimeType()
	 */
	private Icon icon;
	private org.cybergarage.upnp.Device cyberdev;
	public UPnPIconImpl(Icon cybericon,org.cybergarage.upnp.Device cyberdev){
		this.icon=cybericon;
		this.cyberdev=cyberdev;
	}
	public String getMimeType() {
		// TODO to check
		return icon.getMimeType();
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPIcon#getWidth()
	 */
	public int getWidth() {
		String width=String.valueOf(icon.getWidth());
		if(width.length()==0){
			return -1;
		}
		return Integer.parseInt(width);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPIcon#getHeight()
	 */
	public int getHeight() {
		String higth=String.valueOf(icon.getHeight());
		if(higth.length()==0){
			return -1;
		}
		return Integer.parseInt(higth);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPIcon#getSize()
	 */
	public int getSize() {
		// TODO Auto-generated method stub
		return -1;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPIcon#getDepth()
	 */
	public int getDepth() {
		String depth=icon.getDepth();
		if(depth.length()==0){
			return -1;
		}
		return Integer.parseInt(depth);
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPIcon#getInputStream()
	 */
	public InputStream getInputStream() throws IOException {
		String urlString=ParseLocation.getUrlBase(cyberdev.getLocation())+icon.getURL();
		URL url=new URL(urlString);
		HTTPRequestForIcon requestor=new HTTPRequestForIcon(url);
		
		return requestor.getInputStream();
	}

}
